package com.example.attendance.domain.attendance.dto;

import com.example.attendance.domain.attendance.AttendanceStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record AttendancePivotDTO(List<LocalDate> days, List<Row> students) {
    public record Row(String matricolNumber, String fullName, Map<LocalDate, AttendanceStatus> byDay) {}
}
