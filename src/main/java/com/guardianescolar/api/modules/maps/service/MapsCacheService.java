package com.guardianescolar.api.modules.maps.service;

import java.util.Optional;

public interface MapsCacheService {

    Optional<String> getGeocoding(String key);

    void putGeocoding(String key, String payload);

    Optional<String> getDirections(String key);

    void putDirections(String key, String payload);
}
