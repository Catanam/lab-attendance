package com.example.attendance.domain.attendance.dto;

import com.example.attendance.domain.schedule.LabSessionStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record TodaySessionDTO(
        UUID sessionId,
        String courseCode,
        String courseName,
        String groupName,
        Integer yearOfStudy,
        String roomName,
        LocalDateTime startAt,
        LocalDateTime endAt,
        LabSessionStatus status
) {}
