package com.example.attendance.domain.attendance;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GracePolicyTest {
    private final GracePolicy policy = new GracePolicy(15);
    private final LocalDateTime start = LocalDateTime.of(2026, 6, 20, 10, 0);

    @Test
    void noScanIsAbsent() {
        assertEquals(AttendanceStatus.ABSENT, policy.classify(start, null));
    }

    @Test
    void scanWithinGraceIsPresent() {
        assertEquals(AttendanceStatus.PRESENT, policy.classify(start, start.plusMinutes(5)));
    }

    @Test
    void scanExactlyAtGraceBoundaryIsPresent() {
        assertEquals(AttendanceStatus.PRESENT, policy.classify(start, start.plusMinutes(15)));
    }

    @Test
    void scanAfterGraceIsLate() {
        assertEquals(AttendanceStatus.LATE, policy.classify(start, start.plusMinutes(16)));
    }

    @Test
    void earlyScanIsPresent() {
        assertEquals(AttendanceStatus.PRESENT, policy.classify(start, start.minusMinutes(2)));
    }
}
