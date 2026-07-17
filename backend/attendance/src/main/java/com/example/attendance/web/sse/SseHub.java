package com.example.attendance.web.sse;

import com.example.attendance.domain.biometric.RegistrationSessionHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class SseHub {
    private final Map<UUID, SseEmitter> registrationEmitters = new ConcurrentHashMap<>();

    private final Map<UUID, List<SseEmitter>> attendanceEmitters = new ConcurrentHashMap<>();

    private final RegistrationSessionHolder sessionHolder;

    public SseHub(RegistrationSessionHolder sessionHolder) {
        this.sessionHolder = sessionHolder;
    }

    public SseEmitter subscribeRegistration(UUID roomId) {
        SseEmitter emitter = new SseEmitter(0L);
        emitter.onCompletion(() -> {
            registrationEmitters.remove(roomId);
            sessionHolder.clear(roomId);
        });
        emitter.onTimeout(() -> {
            registrationEmitters.remove(roomId);
            sessionHolder.clear(roomId);
        });
        emitter.onError(t -> {
            registrationEmitters.remove(roomId);
            sessionHolder.clear(roomId);
        });
        registrationEmitters.put(roomId, emitter);
        return emitter;
    }

    public void sendRegistrationEvent(UUID roomId, String type, String message, int sampleCount) {
        SseEmitter emitter = registrationEmitters.get(roomId);
        if (emitter == null) return;
        try {
            emitter.send(SseEmitter.event()
                .name(type)
                .data(Map.of("message", message, "sampleCount", sampleCount)));
            if ("complete".equals(type) || "error".equals(type)) {
                emitter.complete();
                registrationEmitters.remove(roomId);
            }
        } catch (IOException e) {
            registrationEmitters.remove(roomId);
        }
    }

    public SseEmitter subscribeAttendance(UUID sessionId) {
        SseEmitter emitter = new SseEmitter(0L);
        List<SseEmitter> list = attendanceEmitters.computeIfAbsent(sessionId, k -> new CopyOnWriteArrayList<>());
        list.add(emitter);
        emitter.onCompletion(() -> list.remove(emitter));
        emitter.onTimeout(() -> { list.remove(emitter); emitter.complete(); });
        emitter.onError(t -> list.remove(emitter));
        return emitter;
    }

    public void sendAttendanceEvent(UUID sessionId, String type, Object payload) {
        List<SseEmitter> list = attendanceEmitters.get(sessionId);
        if (list == null) return;
        for (SseEmitter emitter : list) {
            try {
                emitter.send(SseEmitter.event().name(type).data(payload));
            } catch (IOException | IllegalStateException e) {
                list.remove(emitter);
            }
        }
    }
}
