package com.example.attendance.web.admin.imports;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public record SheetRow(int rowNumber, Map<String, String> cells) {
    public String get(String col) {
        String v = cells.get(col);
        return v == null ? "" : v.trim();
    }

    public boolean has(String col) {
        return cells.containsKey(col);
    }

    public Integer getInt(String col) {
        String v = get(col);
        if (v.isEmpty()) return null;

        if (v.endsWith(".0")) v = v.substring(0, v.length() - 2);
        return Integer.parseInt(v);
    }

    public LocalTime getTime(String col) {
        String v = get(col);
        if (v.isEmpty()) return null;
        if (v.length() == 5) return LocalTime.parse(v, DateTimeFormatter.ofPattern("HH:mm"));
        return LocalTime.parse(v);
    }
}
