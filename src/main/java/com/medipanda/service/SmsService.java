package com.medipanda.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/** Bangladesh SMS gateways. Providers: "brevo" (default, needs SMS credits), "alphasms" (api.sms.net.bd) or "bulksmsbd". */
@Service
public class SmsService {
    private final boolean enabled;
    private final String provider, apiKey, senderId;
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
    private final ObjectMapper mapper = new ObjectMapper();

    public SmsService(@Value("${app.sms.enabled:false}") boolean enabled,
                      @Value("${app.sms.provider:brevo}") String provider,
                      @Value("${app.sms.api-key:}") String apiKey,
                      @Value("${app.sms.sender-id:MediPanda}") String senderId,
                      @Value("${app.mail.brevo-api-key:}") String brevoKey) {
        this.enabled = enabled; this.provider = provider.trim().toLowerCase();
        // Brevo SMS reuses the Brevo email API key unless SMS_API_KEY is set explicitly.
        String k = apiKey == null ? "" : apiKey.trim();
        this.apiKey = k.isEmpty() && "brevo".equals(this.provider) && brevoKey != null ? brevoKey.trim() : k;
        this.senderId = senderId == null ? "" : senderId.trim();
    }

    public boolean isEnabled() { return enabled && !apiKey.isBlank(); }

    /** Converts 01XXXXXXXXX / +8801XXXXXXXXX / 8801XXXXXXXXX to 8801XXXXXXXXX, or null if not a BD mobile. */
    public static String normalize(String phone) {
        if (phone == null) return null;
        String d = phone.replaceAll("[^0-9]", "");
        if (d.startsWith("880")) d = d.substring(3);
        else if (d.startsWith("0")) d = d.substring(1);
        else if (d.length() == 10) { /* 1XXXXXXXXX */ } else return null;
        return d.matches("1[3-9][0-9]{8}") ? "880" + d : null;
    }

    public void send(String phone, String message) {
        if (!isEnabled()) throw new IllegalStateException("SMS is not configured. Set SMS_ENABLED=true and SMS_API_KEY.");
        String to = normalize(phone);
        if (to == null) throw new IllegalArgumentException("Invalid Bangladeshi mobile number");
        try {
            HttpRequest req;
            if ("brevo".equals(provider)) {
                var payload = mapper.createObjectNode();
                payload.put("sender", senderId.length() > 11 ? senderId.substring(0, 11) : senderId);
                payload.put("recipient", to); payload.put("content", message); payload.put("type", "transactional");
                req = HttpRequest.newBuilder(URI.create("https://api.brevo.com/v3/transactionalSMS/send")).timeout(Duration.ofSeconds(15))
                        .header("api-key", apiKey).header("accept", "application/json").header("content-type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(payload))).build();
            } else if ("bulksmsbd".equals(provider)) {
                String url = "http://bulksmsbd.net/api/smsapi?api_key=" + enc(apiKey) + "&type=text&number=" + to
                        + "&senderid=" + enc(senderId) + "&message=" + enc(message);
                req = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(15)).GET().build();
            } else {
                String body = "api_key=" + enc(apiKey) + "&msg=" + enc(message) + "&to=" + to
                        + (senderId.isBlank() ? "" : "&sender_id=" + enc(senderId));
                req = HttpRequest.newBuilder(URI.create("https://api.sms.net.bd/sendsms")).timeout(Duration.ofSeconds(15))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString(body)).build();
            }
            HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() < 200 || res.statusCode() >= 300) throw new IllegalStateException("SMS gateway HTTP " + res.statusCode());
            if ("alphasms".equals(provider) && mapper.readTree(res.body()).path("error").asInt(0) != 0)
                throw new IllegalStateException("SMS gateway rejected the message");
        } catch (IllegalStateException e) { throw e; }
        catch (Exception e) { throw new IllegalStateException("Could not reach the SMS gateway", e); }
    }

    private static String enc(String s) { return URLEncoder.encode(s == null ? "" : s, StandardCharsets.UTF_8); }
}
