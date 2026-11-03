package com.guardianescolar.api.shared.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

class TransientTokenHashServiceTest {

    private final TransientTokenHashService service = new TransientTokenHashService();

    @Test
    void hashesAreDeterministicAndDoNotExposeTheRawToken() {
        String rawToken = "verification-token-value";

        String firstHash = service.hash(rawToken);
        String secondHash = service.hash(rawToken);

        assertEquals(firstHash, secondHash);
        assertNotEquals(rawToken, firstHash);
        assertFalse(firstHash.isBlank());
        assertEquals(64, firstHash.length());
    }
}
