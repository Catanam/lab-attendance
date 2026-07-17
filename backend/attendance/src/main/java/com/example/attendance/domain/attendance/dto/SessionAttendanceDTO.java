package com.example.attendance.domain.attendance.dto;

import com.example.attendance.domain.schedule.LabSessionStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record SessionAttendanceDTO(
        UUID sessionId,
        String courseCode,
        String courseName,
        String groupName,
        Integer yearOfStudy,
        String roomName,
        LocalDate sessionDate,
        LocalDateTime startAt,
        LocalDateTime endAt,
        LabSessionStatus status,
        int presentCount,
        int lateCount,
        int absentCount,
        int guestCount,
        List<AttendanceRowDTO> enrolled,
        List<AttendanceRowDTO> guests
) {}
