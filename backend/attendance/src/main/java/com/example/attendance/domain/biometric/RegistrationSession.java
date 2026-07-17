package com.example.attendance.domain.biometric;

import com.machinezoo.sourceafis.FingerprintTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

public class RegistrationSession {
    private static final int SAMPLES_REQUIRED   = 6;
    private static final double DUPLICATE_THRESHOLD = 40.0;

    private final UUID studentId;
    private final UUID roomId;
    private final List<FingerprintTemplate> samples = new ArrayList<>();
    private final List<String> progressMessages = new CopyOnWriteArrayList<>();
    private volatile Instant lastActivityAt;

    public RegistrationSession(UUID studentId, UUID roomId) {
        this.studentId = studentId;
        this.roomId = roomId;
        this.lastActivityAt = Instant.now();
    }

    public boolean isStale(Duration ttl) {
        return Duration.between(lastActivityAt, Instant.now()).compareTo(ttl) > 0;
    }

    public UUID getStudentId() { return studentId; }
    public UUID getRoomId() { return roomId; }
    public int getSampleCount() { return samples.size(); }
    public int getSamplesRequired() { return SAMPLES_REQUIRED; }
    public List<FingerprintTemplate> getSamples() { return samples; }
    public List<String> getProgressMessages() { return progressMessages; }

    public enum AddResult { DUPLICATE, ADDED, COMPLETE }

    public synchronized AddResult addSample(FingerprintTemplate template) {
        lastActivityAt = Instant.now();

        var matcher = new com.machinezoo.sourceafis.FingerprintMatcher(template);
        for (FingerprintTemplate existing : samples) {
            if (matcher.match(existing) > DUPLICATE_THRESHOLD) return AddResult.DUPLICATE;
        }
        samples.add(template);
        progressMessages.add("Sample " + samples.size() + "/" + SAMPLES_REQUIRED + " captured");
        if (samples.size() >= SAMPLES_REQUIRED) return AddResult.COMPLETE;
        return AddResult.ADDED;
    }
}
