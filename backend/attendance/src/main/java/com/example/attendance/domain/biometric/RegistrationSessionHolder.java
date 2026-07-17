package com.example.attendance.domain.biometric;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RegistrationSessionHolder {
    private static final Duration SESSION_TTL = Duration.ofMinutes(5);

    private final Map<UUID, RegistrationSession> sessions = new ConcurrentHashMap<>();

    public void start(UUID roomId, UUID studentId) {
        sessions.put(roomId, new RegistrationSession(studentId, roomId));
    }

    public Optional<RegistrationSession> get(UUID roomId) {
        RegistrationSession s = sessions.get(roomId);
        if (s == null) return Optional.empty();
        if (s.isStale(SESSION_TTL)) {
            sessions.remove(roomId, s);
            return Optional.empty();
        }
        return Optional.of(s);
    }

    public void clear(UUID roomId) {
        sessions.remove(roomId);
    }

    public boolean isActive(UUID roomId) {
        return get(roomId).isPresent();
    }
}
