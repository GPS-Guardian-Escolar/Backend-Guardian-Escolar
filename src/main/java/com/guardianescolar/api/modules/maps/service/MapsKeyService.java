package com.guardianescolar.api.modules.maps.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.HexFormat;
import org.springframework.stereotype.Service;

@Service
public class MapsKeyService {

    public String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(normalize(value).getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 no está disponible", exception);
        }
    }

    public String normalize(String value) {
        String compact = value == null ? "" : value.trim().toLowerCase().replaceAll("\\s+", " ");
        return Normalizer.normalize(compact, Normalizer.Form.NFKC);
    }
}
