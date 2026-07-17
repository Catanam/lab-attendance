package com.example.attendance.security.hmac;

import org.springframework.stereotype.Component;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class NonceCache {
    private static final int MAX_ENTRIES = 10_000;
    private static final long TTL_MS = 2 * 60 * 1000L;

    private final Map<String, Long> seen = new LinkedHashMap<>(256, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Long> eldest) {
            return size() > MAX_ENTRIES;
        }
    };

    public synchronized boolean isReplayed(String nonce) {
        long now = Instant.now().toEpochMilli();
        evictExpired(now);
        if (seen.containsKey(nonce)) return true;
        seen.put(nonce, now);
        return false;
    }

    private void evictExpired(long now) {
        seen.entrySet().removeIf(e -> (now - e.getValue()) > TTL_MS);
    }
}
