package com.guardianescolar.api.modules.auth.service;

import jakarta.persistence.EntityManager;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Shared, database-backed attempt counters. Keys are hashed so account names,
 * IP addresses and 2FA challenge tokens are not written to security_settings.
 */
@Service
@RequiredArgsConstructor
public class AuthAttemptLimiter {

    private static final String KEY_PREFIX = "AUTH_LIMIT:";
    private static volatile OffsetDateTime lastCleanupAt = OffsetDateTime.MIN;

    private final EntityManager entityManager;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void assertAllowed(String scope, String identity, int maximum, Duration window) {
        cleanupIfDue();
        List<?> result = entityManager.createNativeQuery("""
                SELECT setting_value
                FROM security_settings
                WHERE setting_name = :settingName
                  AND updated_at > CURRENT_TIMESTAMP - (:windowSeconds * INTERVAL '1 second')
                """)
                .setParameter("settingName", key(scope, identity))
                .setParameter("windowSeconds", window.toSeconds())
                .getResultList();
        if (!result.isEmpty() && Integer.parseInt(result.get(0).toString()) >= maximum) {
            throw new AuthRateLimitException();
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int record(String scope, String identity, int maximum, Duration window) {
        cleanupIfDue();
        Object result = entityManager.createNativeQuery("""
                INSERT INTO security_settings(setting_name, setting_value, description, updated_at)
                VALUES (:settingName, '1', 'Hashed authentication attempt counter', CURRENT_TIMESTAMP)
                ON CONFLICT (setting_name) DO UPDATE SET
                    setting_value = CASE
                        WHEN security_settings.updated_at < CURRENT_TIMESTAMP - (:windowSeconds * INTERVAL '1 second')
                            THEN '1'
                        ELSE LEAST(
                            COALESCE(NULLIF(security_settings.setting_value, ''), '0')::integer + 1,
                            :maximumStored
                        )::text
                    END,
                    updated_at = CURRENT_TIMESTAMP
                RETURNING setting_value
                """)
                .setParameter("settingName", key(scope, identity))
                .setParameter("windowSeconds", window.toSeconds())
                .setParameter("maximumStored", maximum + 1)
                .getSingleResult();
        return Integer.parseInt(result.toString());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void clear(String scope, String identity) {
        entityManager.createNativeQuery("DELETE FROM security_settings WHERE setting_name = :settingName")
                .setParameter("settingName", key(scope, identity))
                .executeUpdate();
    }

    public String clientIp() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            String remoteAddress = attributes.getRequest().getRemoteAddr();
            return remoteAddress == null || remoteAddress.isBlank() ? "unknown" : remoteAddress;
        }
        return "unknown";
    }

    private String key(String scope, String identity) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] value = (scope + ":" + (identity == null ? "" : identity.trim().toLowerCase()))
                    .getBytes(StandardCharsets.UTF_8);
            return KEY_PREFIX + HexFormat.of().formatHex(digest.digest(value));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private void cleanupIfDue() {
        OffsetDateTime now = OffsetDateTime.now();
        if (lastCleanupAt.isAfter(now.minusHours(1))) {
            return;
        }
        synchronized (AuthAttemptLimiter.class) {
            if (lastCleanupAt.isAfter(now.minusHours(1))) {
                return;
            }
            entityManager.createNativeQuery("""
                    DELETE FROM security_settings
                    WHERE setting_name LIKE 'AUTH_LIMIT:%'
                      AND updated_at < CURRENT_TIMESTAMP - INTERVAL '1 day'
                    """).executeUpdate();
            lastCleanupAt = now;
        }
    }
}
