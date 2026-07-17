#include "capture_task.h"
#include "sensor.h"
#include <Arduino.h>
#include <esp_task_wdt.h>

static uint8_t s_image_buf[SENSOR_RAW_SIZE];
static QueueHandle_t s_queue = nullptr;

volatile bool g_scan_in_progress = false;
volatile uint32_t g_scan_bytes = 0;

static void capture_loop(void *) {
  esp_task_wdt_delete(NULL);

  ScanResult result;
  result.image = s_image_buf;
  bool armed = true;
  for (;;) {
    if (!sensor_finger_present()) {
      armed = true;
      vTaskDelay(pdMS_TO_TICKS(120));
      continue;
    }
    if (!armed || g_scan_in_progress) {
      vTaskDelay(pdMS_TO_TICKS(120));
      continue;
    }
    Serial.println("[capture] finger detected, fetching image...");
    result.finger_detect_ms = millis();
    g_scan_bytes = 0;
    g_scan_in_progress = true;
    uint32_t t_read = millis();
    result.success =
        sensor_capture(s_image_buf, &result.actual_size, &g_scan_bytes);
    if (!result.success) {
      Serial.println("[capture] read failed — retrying once");
      g_scan_bytes = 0;
      result.success =
          sensor_capture(s_image_buf, &result.actual_size, &g_scan_bytes);
    }
    result.read_ms = millis() - t_read;
    if (result.success) {
      Serial.printf("[capture] OK: %lu bytes, read %lu ms\n",
                    (unsigned long)result.actual_size,
                    (unsigned long)result.read_ms);
    } else {
      Serial.println("[capture] FAILED after retry");
    }
    xQueueOverwrite(s_queue, &result);
    armed = false;
  }
}

QueueHandle_t capture_task_start() {
  s_queue = xQueueCreate(1, sizeof(ScanResult));
  configASSERT(s_queue);

  xTaskCreatePinnedToCore(capture_loop, "capture", 4096, nullptr, 1, nullptr,
                          0
  );
  return s_queue;
}
