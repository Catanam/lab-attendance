#include "ntp_time.h"
#include <Arduino.h>
#include <time.h>

static bool     s_synced        = false;
static uint32_t s_last_sync_ms  = 0;

void ntp_begin(const char* server1, const char* server2) {
    configTime(0, 0, server1, server2);
}

static bool poll_clock(uint32_t wait_ms) {
    struct tm ti;
    if (getLocalTime(&ti, wait_ms) && ti.tm_year > 120) {
        if (!s_synced) {
            Serial.printf(" OK (%04d-%02d-%02d %02d:%02d:%02d UTC)\n",
                          ti.tm_year + 1900, ti.tm_mon + 1, ti.tm_mday,
                          ti.tm_hour, ti.tm_min, ti.tm_sec);
        }
        s_synced       = true;
        s_last_sync_ms = millis();
        return true;
    }
    return false;
}

bool ntp_wait_for_sync(uint32_t timeout_ms) {
    Serial.print("NTP sync");
    unsigned long start = millis();
    while (millis() - start < timeout_ms) {
        if (poll_clock(500)) return true;
        Serial.print('.');
    }
    Serial.println(" timed out — continuing; will keep retrying in loop()");
    return false;
}

bool ntp_is_synced() { return s_synced; }

uint32_t ntp_now() {
    return (uint32_t)time(nullptr);
}

bool ntp_tick(uint32_t resync_interval_ms) {
    if (!s_synced) {
        poll_clock(0);
        return s_synced;
    }
    if ((millis() - s_last_sync_ms) >= resync_interval_ms) {
        poll_clock(0);
    }
    return s_synced;
}
