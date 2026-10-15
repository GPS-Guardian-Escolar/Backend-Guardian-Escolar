package com.guardianescolar.api.shared.security;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import com.guardianescolar.api.modules.security.domain.User;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

    private final String secret;
    private final long expirationMinutes;

    public JwtService(
            @Value("${guardian.security.jwt.secret}") String secret,
            @Value("${guardian.security.jwt.expiration-minutes}") long expirationMinutes) {
        this.secret = secret;
        this.expirationMinutes = expirationMinutes;
    }

    public String generateToken(User user) {
        List<String> roles = user.getRoles().stream()
                 .map(role -> role.getRoleName().name())
                .toList();
        Map<String, Object> claims = Map.of(
                "UserId", user.getId().toString(),
                "roles", roles,
                "nombre", user.getFullName(),
                "emailVerified", Boolean.TRUE.equals(user.getEmailVerified()));
        Instant now = Instant.now();
        return Jwts.builder()
                .claims(claims)
                .subject(user.getEmail())
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expirationMinutes, ChronoUnit.MINUTES)))
                .signWith(signingKey())
                .compact();
    }

    public String generateChildToken(User owner, UUID studentId, String deviceIdentifier) {
        Instant now = Instant.now();
        Map<String, Object> claims = Map.of(
                "tokenType", "CHILD_DEVICE",
                "studentId", studentId.toString(),
                "deviceIdentifier", deviceIdentifier);
        return Jwts.builder()
                .claims(claims)
                .subject(owner.getEmail())
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expirationMinutes, ChronoUnit.MINUTES)))
                .signWith(signingKey())
                .compact();
    }

    public boolean isChildToken(String token) {
        return "CHILD_DEVICE".equals(claims(token).get("tokenType", String.class));
    }

    public UUID childStudentId(String token) {
        return UUID.fromString(claims(token).get("studentId", String.class));
    }

    public String childDeviceIdentifier(String token) {
        return claims(token).get("deviceIdentifier", String.class);
    }

    public String extractUsername(String token) {
        return claims(token).getSubject();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && claims(token).getExpiration().after(new Date());
    }

    public long getExpirationMinutes() {
        return expirationMinutes;
    }

    private Claims claims(String token) {
        return Jwts.parser()
                .verifyWith((javax.crypto.SecretKey) signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private Key signingKey() {
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(secret);
        } catch (RuntimeException exception) {
            keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        }

        if (keyBytes.length < 32) {
            byte[] padded = new byte[32];
            System.arraycopy(keyBytes, 0, padded, 0, keyBytes.length);
            for (int i = keyBytes.length; i < padded.length; i++) {
                padded[i] = (byte) 'x';
            }
            keyBytes = padded;
        }

        return Keys.hmacShaKeyFor(keyBytes);
    }
}
