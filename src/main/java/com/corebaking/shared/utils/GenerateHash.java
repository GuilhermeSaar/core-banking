package com.corebaking.shared.utils;

import lombok.AllArgsConstructor;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.UUID;

@AllArgsConstructor
public class GenerateHash {


    private final static ObjectMapper mapper = new ObjectMapper();

    public static String hash(UUID id, BigDecimal amount) {
        try {
            var amountNormalized = amount.stripTrailingZeros();

            Map<String, Object> payload = Map.of(
                    "id", id.toString(),
                    "amount", amountNormalized
            );

            return generateSha256(mapper.writeValueAsString(payload));
        } catch (Exception e) {
            throw new RuntimeException("Error generating payload hash", e);
        }
    }

    public static String hash(UUID idSource, UUID idDest, BigDecimal amount) {
        try {
            var amountNormalized = amount.stripTrailingZeros();

            Map<String, Object> payload = Map.of(
                    "id1", idSource.toString(),
                    "id2", idDest.toString(),
                    "amount", amountNormalized
            );

            return generateSha256(mapper.writeValueAsString(payload));
        } catch (Exception e) {
            throw new RuntimeException("Error generating payload hash", e);
        }
    }

    private static String generateSha256(String jsonInput) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = digest.digest(jsonInput.getBytes(StandardCharsets.UTF_8));

        StringBuilder hexString = new StringBuilder();
        for (byte b : hashBytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }

        return hexString.toString();
    }

}
