#pragma once
#include <stdint.h>

enum class DisplayState {
    BOOTING,
    SYNCING_TIME,
    READY,
    SCANNING,
    WELCOME,
    NO_MATCH,
    UPLOAD_FAIL,
    GUEST,
    OFFLINE,
    NO_TIME,
    REGISTER_PROGRESS,
    REGISTER_DUPLICATE,
    REGISTER_COMPLETE,
};

void display_init();

void display_set_state(DisplayState state, const char* detail = nullptr);

DisplayState display_get_state();

void display_scan_progress(uint8_t percent);

void display_tick();
