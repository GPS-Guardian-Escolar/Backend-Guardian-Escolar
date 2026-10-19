package com.guardianescolar.api.modules.maps.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.guardianescolar.api.modules.maps.config.GoogleMapsProperties;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class CaffeineMapsCacheService implements MapsCacheService {

    private final Cache<String, String> geocodingCache;
    private final Cache<String, String> directionsCache;

    public CaffeineMapsCacheService(GoogleMapsProperties properties) {
        this.geocodingCache = Caffeine.newBuilder()
                .maximumSize(properties.cache().maximumSize())
                .expireAfterWrite(properties.cache().geocodingTtl())
                .build();
        this.directionsCache = Caffeine.newBuilder()
                .maximumSize(properties.cache().maximumSize())
                .expireAfterWrite(properties.cache().directionsTtl())
                .build();
    }

    @Override
    public Optional<String> getGeocoding(String key) {
        return Optional.ofNullable(geocodingCache.getIfPresent(key));
    }

    @Override
    public void putGeocoding(String key, String payload) {
        geocodingCache.put(key, payload);
    }

    @Override
    public Optional<String> getDirections(String key) {
        return Optional.ofNullable(directionsCache.getIfPresent(key));
    }

    @Override
    public void putDirections(String key, String payload) {
        directionsCache.put(key, payload);
    }
}
