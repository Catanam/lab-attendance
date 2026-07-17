package com.example.attendance.domain.schedule;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class ScheduleConflicts {
    private ScheduleConflicts() {}

    public static Optional<ScheduledLab> firstOverlap(List<ScheduledLab> existing,
            UUID roomId, String weekday, LocalTime start, LocalTime end) {
        return existing.stream()
            .filter(l -> l.getRoom().getId().equals(roomId)
                      && l.getWeekday().equalsIgnoreCase(weekday)
                      && l.getStartTime().isBefore(end)
                      && start.isBefore(l.getEndTime()))
            .findFirst();
    }

    public static String describe(ScheduledLab l) {
        return l.getRoom().getName() + " already has " + l.getCourse().getCode()
            + " (group " + l.getGroup().getName() + ") " + l.getWeekday() + " "
            + l.getStartTime() + "-" + l.getEndTime();
    }
}
