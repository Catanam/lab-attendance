#include "wifi_manager.h"
#include <Arduino.h>
#include <WiFi.h>

static const char* s_ssid = nullptr;
static const char* s_pass = nullptr;

static volatile bool s_wifi_up = false;

static void on_wifi_event(WiFiEvent_t event) {
    switch (event) {
        case ARDUINO_EVENT_WIFI_STA_GOT_IP:
        case ARDUINO_EVENT_WIFI_STA_GOT_IP6:
            s_wifi_up = true;
            Serial.printf("\n[wifi] up — IP %s\n", WiFi.localIP().toString().c_str());
            break;
        case ARDUINO_EVENT_WIFI_STA_DISCONNECTED:
            s_wifi_up = false;
            Serial.println("\n[wifi] down — STA disconnected");
            break;
        default:
            break;
    }
}

void wifi_init(const char* ssid, const char* password) {
    s_ssid = ssid;
    s_pass = password;
    WiFi.onEvent(on_wifi_event);
    WiFi.persistent(false);
    WiFi.mode(WIFI_STA);
    WiFi.setSleep(false);
    WiFi.setAutoReconnect(false);
    WiFi.begin(ssid, password);
}

bool wifi_is_connected() {
    return s_wifi_up;
}

void wifi_ensure_connected(uint32_t hard_reset_ms) {
    if (s_wifi_up) return;

    uint32_t start        = millis();
    uint32_t last_attempt = start;
    bool     radio_reset  = false;

    WiFi.begin(s_ssid, s_pass);

    while (!s_wifi_up) {
        delay(250);
        Serial.print('.');
        uint32_t now = millis();

        if (now - last_attempt >= 5000) {
            last_attempt = now;
            if (!radio_reset) {
                Serial.println("\n[wifi] stuck — cycling radio off/on");
                WiFi.disconnect(true, false);
                WiFi.mode(WIFI_OFF);
                delay(300);
                WiFi.mode(WIFI_STA);
                WiFi.setSleep(false);
                radio_reset = true;
            } else {
                Serial.println("\n[wifi] retrying begin");
            }
            WiFi.begin(s_ssid, s_pass);
        }

        if (hard_reset_ms > 0 && (now - start) >= hard_reset_ms) {
            Serial.println("\n[wifi] still down — restarting for a clean re-init");
            ESP.restart();
        }
    }
    Serial.printf("\n[wifi] connected: %s  GW: %s\n",
                  WiFi.localIP().toString().c_str(),
                  WiFi.gatewayIP().toString().c_str());
}
