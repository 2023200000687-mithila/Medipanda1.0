package com.medipanda.config;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {
    private final SecretKey key;
    private final long expiration;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.expiration-ms}") long expiration) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = expiration;
    }

    public String generate(String email, String role) {
        return Jwts.builder().subject(email).claim("role", role)
                .issuedAt(new Date()).expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(key).compact();
    }

    public String email(String token) { return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload().getSubject(); }
    public String role(String token) { return (String) Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload().get("role"); }
}
