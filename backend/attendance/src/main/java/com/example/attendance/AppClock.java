package com.example.attendance;

import java.time.LocalDateTime;
import java.time.ZoneId;

public final class AppClock {
    public static final ZoneId ZONE = ZoneId.of("Europe/Bucharest");

    private AppClock() {}

    public static LocalDateTime now() {
        return LocalDateTime.now(ZONE);
    }
}
