package com.google.synapseflow.integration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Component
public class HmacSignatureVerifier {

    private static final Logger log = LoggerFactory.getLogger(HmacSignatureVerifier.class);
    private static final String HMAC_SHA256 = "HmacSHA256";

    private final String webhookSecret;

    public HmacSignatureVerifier(@Value("${app.webhook.secret}") String webhookSecret) {
        this.webhookSecret = webhookSecret;
    }

    public boolean verifySignature(String rawPayload, String signatureHeader) {
        if (signatureHeader == null || signatureHeader.isBlank()) {
            return false;
        }

        try {
            String cleanSignature = signatureHeader.startsWith("sha256=")
                    ? signatureHeader.substring(7)
                    : signatureHeader;

            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKey = new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
            mac.init(secretKey);

            byte[] hmacBytes = mac.doFinal(rawPayload.getBytes(StandardCharsets.UTF_8));
            String calculatedHex = HexFormat.of().formatHex(hmacBytes);

            // Constant-time comparison to prevent timing attacks
            return MessageDigest.isEqual(
                    calculatedHex.getBytes(StandardCharsets.UTF_8),
                    cleanSignature.getBytes(StandardCharsets.UTF_8)
            );
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Error computing HMAC signature verification", e);
            return false;
        }
    }

    public String generateSignature(String rawPayload) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKey = new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
            mac.init(secretKey);

            byte[] hmacBytes = mac.doFinal(rawPayload.getBytes(StandardCharsets.UTF_8));
            return "sha256=" + HexFormat.of().formatHex(hmacBytes);
        } catch (Exception e) {
            throw new RuntimeException("Failed to calculate HMAC signature", e);
        }
    }
}
