#include "provisioning.h"
#include <Arduino.h>

static void prompt(const char* label, char* buf, size_t len) {
    Serial.print(label);
    Serial.flush();
    while (Serial.available() == 0) delay(50);
    size_t i = 0;
    while (i < len - 1) {
        while (Serial.available() == 0) delay(10);
        char c = Serial.read();
        if (c == '\n' || c == '\r') {
            if (i > 0) break;
        } else {
            buf[i++] = c;
        }
    }
    buf[i] = '\0';
    Serial.println(buf);
}

bool provisioning_load(Preferences& prefs, DeviceConfig* out) {
    String did    = prefs.getString("device_id",    "");
    String secret = prefs.getString("hmac_secret",  "");
    String ssid   = prefs.getString("wifi_ssid",    "");
    String pass   = prefs.getString("wifi_pass",    "");
    String url    = prefs.getString("server_url",   "");
    String room   = prefs.getString("room_id",      "");

    if (did.isEmpty() || secret.isEmpty() || ssid.isEmpty() ||
        pass.isEmpty() || url.isEmpty()   || room.isEmpty()) {
        return false;
    }

    strlcpy(out->device_id,   did.c_str(),    sizeof(out->device_id));
    strlcpy(out->hmac_secret, secret.c_str(), sizeof(out->hmac_secret));
    strlcpy(out->wifi_ssid,   ssid.c_str(),   sizeof(out->wifi_ssid));
    strlcpy(out->wifi_pass,   pass.c_str(),   sizeof(out->wifi_pass));
    strlcpy(out->server_url,  url.c_str(),    sizeof(out->server_url));
    strlcpy(out->room_id,     room.c_str(),   sizeof(out->room_id));
    return true;
}

void provisioning_run_menu(Preferences& prefs) {
    Serial.println("\n=== FIRST-INSTALL PROVISIONING ===");

    DeviceConfig cfg;
    prompt("WiFi SSID: ",      cfg.wifi_ssid,   sizeof(cfg.wifi_ssid));
    prompt("WiFi Password: ",   cfg.wifi_pass,   sizeof(cfg.wifi_pass));
    prompt("Server URL: ",      cfg.server_url,  sizeof(cfg.server_url));
    prompt("Device ID (UUID): ", cfg.device_id,  sizeof(cfg.device_id));
    prompt("HMAC Secret (hex): ", cfg.hmac_secret, sizeof(cfg.hmac_secret));
    prompt("Room ID (UUID): ",  cfg.room_id,     sizeof(cfg.room_id));

    prefs.putString("device_id",   cfg.device_id);
    prefs.putString("hmac_secret", cfg.hmac_secret);
    prefs.putString("wifi_ssid",   cfg.wifi_ssid);
    prefs.putString("wifi_pass",   cfg.wifi_pass);
    prefs.putString("server_url",  cfg.server_url);
    prefs.putString("room_id",     cfg.room_id);

    Serial.println("Saved. Rebooting...");
    delay(500);
    ESP.restart();
}
