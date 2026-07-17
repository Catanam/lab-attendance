#include <Arduino.h>
#include <ArduinoJson.h>
#include <esp_bt.h>

#include "provisioning.h"
#include "wifi_manager.h"
#include "ntp_time.h"
#include "sensor.h"
#include "capture_task.h"
#include "hmac_client.h"
#include "display.h"
#include "feedback.h"

DeviceConfig g_cfg;

static QueueHandle_t s_scan_queue = nullptr;

static void disable_bluetooth() {
    esp_bt_controller_status_t st = esp_bt_controller_get_status();
    Serial.printf("[BT] status at boot: %d (0=idle 1=inited 2=enabled)\n", (int)st);
    if (st == ESP_BT_CONTROLLER_STATUS_ENABLED) {
        Serial.println("[BT] active — disabling");
        esp_bt_controller_disable();
    }
    if (esp_bt_controller_get_status() == ESP_BT_CONTROLLER_STATUS_INITED) {
        esp_bt_controller_deinit();
    }
    esp_bt_mem_release(ESP_BT_MODE_BTDM);
    Serial.printf("[BT] off. status=%d free heap=%u\n",
                  (int)esp_bt_controller_get_status(), ESP.getFreeHeap());
}

static void load_hardcoded_config() {
    strlcpy(g_cfg.wifi_ssid,   "mihai",                                    sizeof(g_cfg.wifi_ssid));
    strlcpy(g_cfg.wifi_pass,   "qazwsxedc",                                sizeof(g_cfg.wifi_pass));
    strlcpy(g_cfg.server_url,  "https://lab-attendance.duckdns.org",       sizeof(g_cfg.server_url));
    strlcpy(g_cfg.device_id,   "760eb111-a8df-4a98-81ee-f0980097c3e0",     sizeof(g_cfg.device_id));
    strlcpy(g_cfg.hmac_secret, "mysecret1234567890abcdef12345678",          sizeof(g_cfg.hmac_secret));
    strlcpy(g_cfg.room_id,     "6f47e962-a787-4470-afef-b73db345df34",     sizeof(g_cfg.room_id));
}

void setup() {
    Serial.begin(115200);
    delay(500);

    disable_bluetooth();

    display_init();
    display_set_state(DisplayState::BOOTING);
    feedback_init();

    load_hardcoded_config();

    wifi_init(g_cfg.wifi_ssid, g_cfg.wifi_pass);
    wifi_ensure_connected(30000);

    display_set_state(DisplayState::SYNCING_TIME);
    ntp_begin();
    if (!ntp_wait_for_sync()) {
        display_set_state(DisplayState::NO_TIME);
    }

    sensor_ensure_high_speed();
    sensor_init();

    display_set_state(DisplayState::READY);
    Serial.printf("[MEM] at READY (boot baseline): free heap=%u\n", ESP.getFreeHeap());

    Serial.println("[BOOT] Pinging server...");
    PostResult ping = hmac_post_health();
    if (ping == PostResult::OK) {
        Serial.printf("[BOOT] Server reachable: %s/api/health -> 200 OK\n", g_cfg.server_url);
        feedback_trigger(FeedbackEvent::REGULAR_OK);
    } else if (ping == PostResult::UNAUTHORIZED) {
        Serial.println("[BOOT] Server reachable but HMAC rejected (401) — check device_id/secret");
        feedback_trigger(FeedbackEvent::ERROR);
    } else {
        Serial.printf("[BOOT] Server unreachable (err %d) — continuing offline\n", (int)ping);
        feedback_trigger(FeedbackEvent::OFFLINE);
    }

    s_scan_queue = capture_task_start();
}

void loop() {
    display_tick();
    feedback_tick();

    if (!wifi_is_connected()) {
        Serial.println("\n[wifi] link lost — going offline");
        display_set_state(DisplayState::OFFLINE);
        feedback_trigger(FeedbackEvent::OFFLINE);
        wifi_ensure_connected(30000);
        g_scan_in_progress = false;
        xQueueReset(s_scan_queue);
        display_set_state(DisplayState::READY);
        return;
    }

    if (!ntp_tick()) {
        if (display_get_state() != DisplayState::NO_TIME) {
            Serial.println("[ntp] clock not set — holding scans until NTP syncs");
            display_set_state(DisplayState::NO_TIME);
        }
        g_scan_in_progress = false;
        xQueueReset(s_scan_queue);
        return;
    }
    if (display_get_state() == DisplayState::NO_TIME)
        display_set_state(DisplayState::READY);

    if (g_scan_in_progress) {
        uint32_t got = g_scan_bytes;
        uint8_t  pct = (got >= SENSOR_RAW_SIZE) ? 100
                                                : (uint8_t)(got * 100UL / SENSOR_RAW_SIZE);
        display_scan_progress(pct);
    }

    ScanResult result;
    if (xQueueReceive(s_scan_queue, &result, 0) != pdTRUE) return;

    if (!result.success) {
        g_scan_in_progress = false;
        display_set_state(DisplayState::UPLOAD_FAIL);
        feedback_trigger(FeedbackEvent::ERROR);
        return;
    }

    Serial.printf("[scan] POSTing %lu bytes to /api/scan...\n", (unsigned long)result.actual_size);

    char response[256] = {};
    uint32_t t_post = millis();
    PostResult rc = hmac_post_scan(result.image, result.actual_size,
                                   response, sizeof(response));
    uint32_t post_ms = millis() - t_post;
    g_scan_in_progress = false;

    uint32_t total_ms = millis() - result.finger_detect_ms;
    static uint32_t s_scan_count = 0;
    s_scan_count++;
    Serial.printf("[lat] sensor read %lu ms | sign+transfer %lu ms | total %lu ms\n",
                  (unsigned long)result.read_ms, (unsigned long)post_ms, (unsigned long)total_ms);
    Serial.printf("[MEM] after scan #%lu: free heap=%u\n",
                  (unsigned long)s_scan_count, ESP.getFreeHeap());

    Serial.printf("[scan] rc=%d body=%s\n", (int)rc, response);

    if (rc == PostResult::OK) {
        JsonDocument doc;
        DeserializationError err = deserializeJson(doc, response);
        const char* kind = (err || doc["kind"].isNull()) ? "UNMATCHED"
                                                         : doc["kind"].as<const char*>();
        const char* name = doc["name"].as<const char*>();

        if (strcmp(kind, "REGULAR") == 0) {
            display_set_state(DisplayState::WELCOME, name ? name : "");
            feedback_trigger(FeedbackEvent::REGULAR_OK);
        } else if (strcmp(kind, "GUEST") == 0) {
            display_set_state(DisplayState::GUEST, name ? name : "");
            feedback_trigger(FeedbackEvent::GUEST);
        } else if (strcmp(kind, "REGISTER_PROGRESS") == 0) {
            int sample   = doc["sample"]   | 0;
            int required = doc["required"] | 6;
            char detail[8];
            snprintf(detail, sizeof(detail), "%d/%d", sample, required);
            display_set_state(DisplayState::REGISTER_PROGRESS, detail);
            feedback_trigger(FeedbackEvent::REGISTER_SAMPLE_OK);
        } else if (strcmp(kind, "REGISTER_DUPLICATE") == 0) {
            display_set_state(DisplayState::REGISTER_DUPLICATE);
            feedback_trigger(FeedbackEvent::REGISTER_DUPLICATE);
        } else if (strcmp(kind, "REGISTER_RETRY") == 0) {
            display_set_state(DisplayState::UPLOAD_FAIL);
            feedback_trigger(FeedbackEvent::ERROR);
        } else if (strcmp(kind, "REGISTER_COMPLETE") == 0) {
            display_set_state(DisplayState::REGISTER_COMPLETE, name ? name : "");
            feedback_trigger(FeedbackEvent::REGISTER_COMPLETE);
        } else {
            display_set_state(DisplayState::NO_MATCH);
            feedback_trigger(FeedbackEvent::NO_MATCH);
        }
    } else if (rc == PostResult::UNAUTHORIZED) {
        Serial.println("[ERROR] HMAC rejected — check device_id/secret/clock");
        display_set_state(DisplayState::UPLOAD_FAIL);
        feedback_trigger(FeedbackEvent::ERROR);
    } else {
        display_set_state(DisplayState::UPLOAD_FAIL);
        feedback_trigger(FeedbackEvent::ERROR);
    }
}
