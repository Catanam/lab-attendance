#pragma once
#include <Preferences.h>

struct DeviceConfig {
    char device_id[37];
    char hmac_secret[65];
    char wifi_ssid[64];
    char wifi_pass[64];
    char server_url[128];
    char room_id[37];
};

bool provisioning_load(Preferences& prefs, DeviceConfig* out);

void provisioning_run_menu(Preferences& prefs);
