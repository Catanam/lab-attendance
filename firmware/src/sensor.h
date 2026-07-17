#pragma once
#include <stdint.h>

#define SENSOR_RX_PIN   16
#define SENSOR_TX_PIN   17
#define SENSOR_RAW_SIZE 36864u

void sensor_ensure_high_speed();

void sensor_init();

bool sensor_finger_present();

bool sensor_capture(uint8_t* buf, uint32_t* actual_size_out,
                    volatile uint32_t* progress_out = nullptr);
