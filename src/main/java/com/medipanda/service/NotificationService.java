package com.medipanda.service;

import com.medipanda.model.Notification;
import com.medipanda.model.Order;
import com.medipanda.model.User;
import com.medipanda.repository.NotificationRepository;
import com.medipanda.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Every notification is stored in-app. ORDER/DELIVERY notifications also go out by
 * email (Brevo) and push (FCM); the four key order events additionally go out by SMS.
 * External channels run asynchronously and can never break the order flow.
 */
@Service
public class NotificationService {
    public enum OrderEvent { PLACED, PAID, PACKED, DELIVERED }

    private final NotificationRepository repo; private final UserRepository users;
    private final EmailService email; private final FcmService fcm; private final SmsService sms;

    public NotificationService(NotificationRepository r, UserRepository u, EmailService e, FcmService f, SmsService s) {
        repo = r; users = u; email = e; fcm = f; sms = s;
    }

    public void send(String userId, String title, String message, String type, String orderId) { dispatch(userId, title, message, type, orderId, false); }

    /** Order placed / payment confirmed / medicine packed / delivered -> customer (email + push + SMS). */
    public void notifyCustomer(Order o, OrderEvent ev) {
        if (o == null || o.getCustomerEmail() == null) return;
        String ref = o.getId() == null ? "" : o.getId().substring(Math.max(0, o.getId().length() - 6));
        String[] t = switch (ev) {
            case PLACED -> new String[]{"Order placed", "Your order #" + ref + " has been placed. We will confirm it shortly."};
            case PAID -> new String[]{"Payment confirmed", "We received your payment for order #" + ref + ". Thank you!"};
            case PACKED -> new String[]{"Medicine packed", "Your medicine for order #" + ref + " is packed and a rider is being assigned."};
            case DELIVERED -> new String[]{"Order delivered", "Order #" + ref + " has been delivered. Stay healthy!"};
        };
        users.findByEmailIgnoreCaseOrPhone(o.getCustomerEmail(), o.getCustomerEmail())
                .ifPresent(c -> dispatch(c.getId(), t[0], t[1], "ORDER", o.getId(), true));
    }

    private void dispatch(String userId, String title, String message, String type, String orderId, boolean withSms) {
        if (userId == null) return;
        repo.save(Notification.builder().userId(userId).title(title).message(message).type(type).orderId(orderId).read(false).createdAt(new Date()).build());
        if (!"ORDER".equalsIgnoreCase(type) && !"DELIVERY".equalsIgnoreCase(type)) return;
        users.findById(userId).ifPresent(u -> CompletableFuture.runAsync(() -> external(u, title, message, orderId, withSms)));
    }

    private void external(User u, String title, String message, String orderId, boolean withSms) {
        if (u.getEmail() != null && u.getEmail().contains("@") && email.isEnabled())
            try { email.sendNotification(u.getEmail(), title, message, orderId); } catch (Exception e) { warn("email", e); }
        if (fcm.isEnabled() && u.getFcmTokens() != null && !u.getFcmTokens().isEmpty()) {
            List<String> dead = new ArrayList<>();
            Map<String, String> data = new HashMap<>(); data.put("orderId", orderId == null ? "" : orderId);
            for (String tk : new ArrayList<>(u.getFcmTokens()))
                try { if (!fcm.send(tk, title, message, data)) dead.add(tk); } catch (Exception e) { warn("push", e); }
            if (!dead.isEmpty()) users.findById(u.getId()).ifPresent(f -> { f.getFcmTokens().removeAll(dead); users.save(f); });
        }
        if (withSms && sms.isEnabled() && u.getPhone() != null)
            try { sms.send(u.getPhone(), "MediPanda: " + message); } catch (Exception e) { warn("sms", e); }
    }

    private static void warn(String channel, Exception e) { System.err.println("[notify] " + channel + " failed: " + e.getMessage()); }
}
