package com.example.attendance.support;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class HmacRequest {
    private HmacRequest() {}

    public static Map<String, String> headers(String deviceId, String secret, byte[] body) {
        return headers(deviceId, secret, body, Instant.now().getEpochSecond(), UUID.randomUUID().toString());
    }

    public static Map<String, String> headers(String deviceId, String secret, byte[] body,
                                              long epochSeconds, String nonce) {
        String ts = Long.toString(epochSeconds);
        String bodySha = sha256Hex(body == null ? new byte[0] : body);
        String input = ts + ":" + deviceId + ":" + nonce + ":" + bodySha;
        String hmac = hmacSha256Hex(secret, input);

        Map<String, String> h = new LinkedHashMap<>();
        h.put("Content-Type", "application/octet-stream");
        h.put("X-Device-ID", deviceId);
        h.put("X-Timestamp", ts);
        h.put("X-Nonce", nonce);
        h.put("X-HMAC", hmac);
        return h;
    }

    public static String sha256Hex(byte[] data) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static String hmacSha256Hex(String secret, String input) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(input.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
