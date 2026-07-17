#pragma once
#include "sensor.h"
#include <freertos/FreeRTOS.h>
#include <freertos/queue.h>
#include <stdint.h>

struct ScanResult {
  bool success;
  uint32_t actual_size;
  uint8_t *image;
  uint32_t
      finger_detect_ms;
  uint32_t read_ms;
};

extern volatile bool g_scan_in_progress;

extern volatile uint32_t g_scan_bytes;

QueueHandle_t capture_task_start();
