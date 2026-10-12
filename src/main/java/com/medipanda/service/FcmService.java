package com.medipanda.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Duration;
import java.util.Base64;
import java.util.Date;
import java.util.Map;

/** Firebase Cloud Messaging (HTTP v1) using a service-account JSON; no extra dependencies. */
@Service
public class FcmService {
    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
    private JsonNode account;
    private PrivateKey key;
    private String accessToken;
    private long tokenExpiresAt;

    public FcmService(@Value("${app.fcm.service-account-json:}") String json,
                      @Value("${app.fcm.service-account-file:}") String file) {
        try {
            String raw = json == null ? "" : json.trim();
            if (raw.isEmpty() && file != null && !file.isBlank()) raw = Files.readString(Path.of(file.trim()));
            if (!raw.isEmpty()) {
                account = mapper.readTree(raw);
                String pem = account.path("private_key").asText().replaceAll("-----[A-Z ]+-----", "").replaceAll("\\s", "");
                key = KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(pem)));
            }
        } catch (Exception e) {
            account = null; key = null;
            System.err.println("[FCM] Service account could not be loaded: " + e.getMessage());
        }
    }

    public boolean isEnabled() { return account != null && key != null; }

    /** @return false when the token is dead (UNREGISTERED/NOT_FOUND) and should be removed. */
    public boolean send(String deviceToken, String title, String body, Map<String, String> data) {
        if (!isEnabled()) throw new IllegalStateException("FCM is not configured. Set FCM_SERVICE_ACCOUNT_JSON or FCM_SERVICE_ACCOUNT_FILE.");
        try {
            var msg = mapper.createObjectNode().putObject("message");
            msg.put("token", deviceToken);
            msg.putObject("notification").put("title", title).put("body", body);
            var d = msg.putObject("data");
            if (data != null) data.forEach((k, v) -> d.put(k, v == null ? "" : v));
            var root = mapper.createObjectNode(); root.set("message", msg);
            HttpRequest req = HttpRequest.newBuilder(URI.create("https://fcm.googleapis.com/v1/projects/" + account.path("project_id").asText() + "/messages:send"))
                    .timeout(Duration.ofSeconds(15)).header("Authorization", "Bearer " + token())
                    .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(root))).build();
            HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() == 404 || (res.statusCode() == 400 && res.body().contains("INVALID_ARGUMENT") && res.body().contains("token"))) return false;
            if (res.statusCode() < 200 || res.statusCode() >= 300) throw new IllegalStateException("FCM HTTP " + res.statusCode());
            return true;
        } catch (IllegalStateException e) { throw e; }
        catch (Exception e) { throw new IllegalStateException("Could not reach FCM", e); }
    }

    private synchronized String token() throws Exception {
        if (accessToken != null && System.currentTimeMillis() < tokenExpiresAt - 60_000) return accessToken;
        Date now = new Date();
        String assertion = Jwts.builder().issuer(account.path("client_email").asText())
                .claim("scope", "https://www.googleapis.com/auth/firebase.messaging")
                .audience().add("https://oauth2.googleapis.com/token").and()
                .issuedAt(now).expiration(new Date(now.getTime() + 3_600_000)).signWith(key, Jwts.SIG.RS256).compact();
        String form = "grant_type=" + URLEncoder.encode("urn:ietf:params:oauth:grant-type:jwt-bearer", StandardCharsets.UTF_8) + "&assertion=" + assertion;
        HttpResponse<String> res = client.send(HttpRequest.newBuilder(URI.create("https://oauth2.googleapis.com/token"))
                .timeout(Duration.ofSeconds(10)).header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form)).build(), HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() != 200) throw new IllegalStateException("FCM auth failed (HTTP " + res.statusCode() + ")");
        JsonNode j = mapper.readTree(res.body());
        accessToken = j.path("access_token").asText();
        tokenExpiresAt = System.currentTimeMillis() + j.path("expires_in").asLong(3600) * 1000;
        return accessToken;
    }
}
