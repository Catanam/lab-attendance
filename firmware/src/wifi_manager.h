#pragma once
#include <stdint.h>

void wifi_init(const char* ssid, const char* password);
bool wifi_is_connected();

void wifi_ensure_connected(uint32_t hard_reset_ms = 30000);
