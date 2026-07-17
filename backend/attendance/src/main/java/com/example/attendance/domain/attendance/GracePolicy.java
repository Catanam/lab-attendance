package com.example.attendance.domain.attendance;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

@Component
public class GracePolicy {
    private final long graceMinutes;

    public GracePolicy(@Value("${attendance.grace-minutes:15}") long graceMinutes) {
        this.graceMinutes = graceMinutes;
    }

    public AttendanceStatus classify(LocalDateTime sessionStart, LocalDateTime firstScan) {
        if (firstScan == null) {
            return AttendanceStatus.ABSENT;
        }
        long minutesLate = Duration.between(sessionStart, firstScan).toMinutes();
        return minutesLate <= graceMinutes ? AttendanceStatus.PRESENT : AttendanceStatus.LATE;
    }

    public long getGraceMinutes() {
        return graceMinutes;
    }
}
