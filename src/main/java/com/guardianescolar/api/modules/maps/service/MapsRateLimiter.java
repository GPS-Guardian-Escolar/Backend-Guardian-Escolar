package com.guardianescolar.api.modules.maps.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.guardianescolar.api.modules.maps.config.GoogleMapsProperties;
import com.guardianescolar.api.modules.maps.exception.MapsRateLimitException;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class MapsRateLimiter {

    private final int permitsPerMinute;
    private final Cache<String, WindowCounter> counters;

    public MapsRateLimiter(GoogleMapsProperties properties) {
        this.permitsPerMinute = properties.rateLimit().permitsPerMinute();
        this.counters = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(2))
                .maximumSize(10_000)
                .build();
    }

    public void check(String endpoint) {
        if (permitsPerMinute <= 0) {
            return;
        }
        long currentMinute = Instant.now().getEpochSecond() / 60;
        String key = principal() + ":" + endpoint + ":" + currentMinute;
        WindowCounter counter = counters.get(key, ignored -> new WindowCounter(currentMinute));
        if (counter.count().incrementAndGet() > permitsPerMinute) {
            throw new MapsRateLimitException(Duration.ofSeconds(secondsUntilNextMinute()));
        }
    }

    private String principal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            return "anonymous";
        }
        return authentication.getName();
    }

    private long secondsUntilNextMinute() {
        return 60 - (Instant.now().getEpochSecond() % 60);
    }

    private record WindowCounter(long minute, AtomicInteger count) {

        private WindowCounter(long minute) {
            this(minute, new AtomicInteger());
        }
    }
}
