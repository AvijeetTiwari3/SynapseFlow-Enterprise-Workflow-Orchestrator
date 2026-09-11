package com.google.synapseflow.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HmacSignatureVerifierTest {

    private HmacSignatureVerifier verifier;
    private static final String SECRET = "test-secret-key-1234567890";

    @BeforeEach
    void setUp() {
        verifier = new HmacSignatureVerifier(SECRET);
    }

    @Test
    @DisplayName("Should successfully verify matching HMAC-SHA256 signature")
    void testValidSignature() {
        String payload = "{\"event\":\"WORKDAY_HIRE\",\"employeeId\":\"E-1002\"}";
        String signature = verifier.generateSignature(payload);

        assertTrue(verifier.verifySignature(payload, signature));
    }

    @Test
    @DisplayName("Should reject signature if payload was tampered")
    void testTamperedPayloadRejection() {
        String originalPayload = "{\"amount\": 100}";
        String tamperedPayload = "{\"amount\": 100000}";
        String signature = verifier.generateSignature(originalPayload);

        assertFalse(verifier.verifySignature(tamperedPayload, signature));
    }

    @Test
    @DisplayName("Should reject null or empty signature")
    void testEmptySignature() {
        assertFalse(verifier.verifySignature("{}", null));
        assertFalse(verifier.verifySignature("{}", ""));
    }
}
