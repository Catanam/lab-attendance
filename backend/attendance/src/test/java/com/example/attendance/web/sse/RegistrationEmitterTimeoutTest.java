package com.example.attendance.web.sse;

import com.example.attendance.domain.biometric.RegistrationSessionHolder;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RegistrationEmitterTimeoutTest {
    @Test
    void registrationEmitterHasNoServerSideTimeout() {
        SseHub hub = new SseHub(new RegistrationSessionHolder());
        SseEmitter emitter = hub.subscribeRegistration(UUID.randomUUID());
        assertEquals(0L, emitter.getTimeout(),
            "registration emitter must have no timeout so an active enrolment is never cleared by the clock");
    }
}
