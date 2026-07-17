package com.example.attendance.domain.biometric;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegistrationSessionStalenessTest {
    @Test
    void freshSessionIsNotStale() {
        RegistrationSession s = new RegistrationSession(UUID.randomUUID(), UUID.randomUUID());
        assertFalse(s.isStale(Duration.ofMinutes(5)));
    }

    @Test
    void inactiveSessionBecomesStale() throws InterruptedException {
        RegistrationSession s = new RegistrationSession(UUID.randomUUID(), UUID.randomUUID());
        Thread.sleep(30);
        assertTrue(s.isStale(Duration.ofMillis(1)));
    }
}
