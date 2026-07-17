package com.example.attendance.domain.attendance.dto;

import com.example.attendance.domain.attendance.AttendanceStatus;

import java.time.LocalDate;

public record AttendanceExportRowDTO(
        LocalDate date,
        String startTime,
        String courseCode,
        String courseName,
        String groupName,
        String roomName,
        String matricolNumber,
        String fullName,
        AttendanceStatus status,
        String scanTime,
        boolean guest
) {}
