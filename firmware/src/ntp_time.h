#pragma once
#include <stdint.h>

void ntp_begin(const char* server1 = "pool.ntp.org",
               const char* server2 = "time.cloudflare.com");

bool ntp_wait_for_sync(uint32_t timeout_ms = 20000UL);

bool     ntp_is_synced();
uint32_t ntp_now();

bool ntp_tick(uint32_t resync_interval_ms = 3600000UL);
