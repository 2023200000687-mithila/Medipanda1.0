package com.medipanda.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
public class EmailService {
    private final String apiKey;
    private final String senderEmail;
    private final String senderName;
    private final boolean enabled;
    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();

    public EmailService(
            @Value("${app.mail.brevo-api-key:}") String apiKey,
            @Value("${app.mail.brevo-sender-email:}") String senderEmail,
            @Value("${app.mail.brevo-sender-name:MediPanda}") String senderName,
            @Value("${app.mail.brevo-enabled:true}") boolean enabled) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.senderEmail = senderEmail == null ? "" : senderEmail.trim();
        this.senderName = senderName == null || senderName.isBlank() ? "MediPanda" : senderName.trim();
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled && !apiKey.isBlank() && !senderEmail.isBlank();
    }

    public void sendOtp(String email, String code) {
        sendOtp(email, code, "REGISTRATION");
    }

    public void sendOtp(String email, String code, String purpose) {
        boolean reset = "PASSWORD_RESET".equalsIgnoreCase(purpose);
        String subject = reset ? "MediPanda password reset code" : "MediPanda verification code";
        String heading = reset ? "Reset your MediPanda password" : "Verify your MediPanda account";
        String intro = reset ? "Use this one-time code to reset your password:" : "Your one-time verification code is:";
        String warn = reset
                ? "This code expires in 10 minutes. If you did not request a password reset, you can safely ignore this email."
                : "This code expires in 10 minutes. Never share it with anyone.";
        send(email, subject,
                "<div style='background:#f6f9f7;padding:28px 12px;font-family:Arial,Helvetica,sans-serif'>" +
                "<div style='max-width:520px;margin:auto;background:#ffffff;border:1px solid #e3ebe6;border-radius:16px;padding:32px'>" +
                "<div style='font-size:22px;font-weight:800;color:#0f1f19;margin-bottom:18px'>Medi<span style='color:#12a066'>Panda</span></div>" +
                "<h2 style='margin:0 0 8px;color:#0f1f19'>" + heading + "</h2>" +
                "<p style='color:#55665e;margin:0 0 18px'>" + intro + "</p>" +
                "<div style='font-size:34px;font-weight:800;letter-spacing:10px;text-align:center;background:#eef8f2;color:#0a7a4c;border-radius:12px;padding:18px 0'>" + escape(code) + "</div>" +
                "<p style='color:#6b7b73;font-size:13px;margin:18px 0 0'>" + warn + "</p></div></div>");
    }

    public void sendOrderUpdate(String email, String orderId, String status, int eta) {
        if (email == null || !email.contains("@")) return;
        String safeStatus = escape(status == null ? "Updated" : status.replace("_", " "));
        send(email, "MediPanda order update #" + escape(orderId),
                "<div style='font-family:Arial,sans-serif;max-width:560px;margin:auto;padding:28px'>" +
                "<h2>Your MediPanda order has been updated</h2><p>Order <b>#" + escape(orderId) + "</b>: <b>" + safeStatus + "</b>.</p>" +
                (eta > 0 ? "<p>Estimated delivery: <b>" + eta + " minutes</b></p>" : "") +
                "<p>Open MediPanda to view live tracking and chat.</p></div>");
    }

    public void sendNotification(String email, String title, String message, String orderId) {
        if (email == null || !email.contains("@")) return;
        String order = orderId != null && !orderId.isBlank()
                ? "<p>Order: <b>#" + escape(orderId.substring(Math.max(0, orderId.length() - 6))) + "</b></p>" : "";
        send(email, "MediPanda — " + (title == null ? "Notification" : title),
                "<div style='font-family:Arial,sans-serif;max-width:560px;margin:auto;padding:28px'>" +
                "<h2>" + escape(title) + "</h2><p>" + escape(message) + "</p>" + order + "</div>");
    }

    public void sendBrevoOtp(String to, String code) {
        sendOtp(to, code, "REGISTRATION");
    }

    public void sendBrevoOtp(String to, String code, String purpose) {
        sendOtp(to, code, purpose);
    }

    private void send(String to, String subject, String html) {
        if (!enabled) throw new IllegalStateException("Brevo email delivery is disabled. Set BREVO_ENABLED=true.");
        if (apiKey.isBlank() || senderEmail.isBlank()) {
            throw new IllegalStateException("Brevo is not configured. Set BREVO_API_KEY and BREVO_SENDER_EMAIL in your Run Configuration environment variables.");
        }
        if (to == null || !to.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new IllegalArgumentException("A valid recipient email is required.");
        }
        try {
            var payload = mapper.createObjectNode();
            payload.putObject("sender").put("name", senderName).put("email", senderEmail);
            payload.putArray("to").addObject().put("email", to.trim());
            payload.put("subject", subject);
            payload.put("htmlContent", html);
            HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.brevo.com/v3/smtp/email"))
                    .timeout(Duration.ofSeconds(15))
                    .header("api-key", apiKey)
                    .header("accept", "application/json")
                    .header("content-type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(payload)))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Brevo rejected the email (HTTP " + response.statusCode() + "). Verify the API key, sender verification and Brevo account status.");
            }
        } catch (IllegalStateException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Could not send email through Brevo. Check internet access and Brevo settings.", e);
        }
    }

    private static String escape(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
}
