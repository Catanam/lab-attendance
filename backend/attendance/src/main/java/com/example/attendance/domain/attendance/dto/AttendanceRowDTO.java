package com.example.attendance.domain.attendance.dto;

import com.example.attendance.domain.attendance.AttendanceStatus;

import java.util.UUID;

public record AttendanceRowDTO(
        UUID studentId,
        String matricolNumber,
        String fullName,
        AttendanceStatus status,
        String scanTime,
        boolean guest,
        boolean shouldCount,
        UUID toggleScanEventId
) {}
