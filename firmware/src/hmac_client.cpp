#include "hmac_client.h"
#include "provisioning.h"
#include <Arduino.h>
#include <string.h>
#include <WiFi.h>
#include <WiFiClient.h>
#include <WiFiClientSecure.h>
#include <HTTPClient.h>
#include <mbedtls/sha256.h>
#include <mbedtls/md.h>
#include <esp_random.h>

extern DeviceConfig g_cfg;

static WiFiClient       s_plainClient;
static WiFiClientSecure s_tlsClient;
static HTTPClient       s_http;
static bool             s_tls_inited = false;

static const int32_t  CONNECT_TIMEOUT_MS = 8000;
static const uint16_t IO_TIMEOUT_MS      = 15000;

static void bytes_to_hex(const uint8_t* in, size_t len, char* out) {
    static const char hex[] = "0123456789abcdef";
    for (size_t i = 0; i < len; i++) {
        out[i * 2]     = hex[in[i] >> 4];
        out[i * 2 + 1] = hex[in[i] & 0x0F];
    }
    out[len * 2] = '\0';
}

static void make_nonce_uuid(char* buf37) {
    uint8_t r[16];
    for (int i = 0; i < 4; i++) {
        uint32_t v = esp_random();
        memcpy(r + i * 4, &v, 4);
    }
    r[6] = (r[6] & 0x0F) | 0x40;
    r[8] = (r[8] & 0x3F) | 0x80;
    snprintf(buf37, 37,
        "%02x%02x%02x%02x-%02x%02x-%02x%02x-%02x%02x-%02x%02x%02x%02x%02x%02x",
        r[0],r[1],r[2],r[3], r[4],r[5], r[6],r[7],
        r[8],r[9], r[10],r[11],r[12],r[13],r[14],r[15]);
}

static void compute_hmac(const char* timestamp, const char* nonce,
                         const char* body_sha_hex, char* hmac_hex_out) {
    char hmac_input[256];
    snprintf(hmac_input, sizeof(hmac_input), "%s:%s:%s:%s",
             timestamp, g_cfg.device_id, nonce, body_sha_hex);

    uint8_t hmac_raw[32];
    const mbedtls_md_info_t* md = mbedtls_md_info_from_type(MBEDTLS_MD_SHA256);
    mbedtls_md_hmac(md,
        (const uint8_t*)g_cfg.hmac_secret, strlen(g_cfg.hmac_secret),
        (const uint8_t*)hmac_input, strlen(hmac_input),
        hmac_raw);
    bytes_to_hex(hmac_raw, 32, hmac_hex_out);
}

static PostResult http_post(const char* url,
                            const char* timestamp, const char* nonce,
                            const char* hmac_hex,
                            const uint8_t* body, size_t body_len,
                            char* response_buf, size_t response_buf_len) {
    bool useTls = (strncmp(url, "https://", 8) == 0);
    if (useTls && !s_tls_inited) {
        s_tlsClient.setInsecure();
        s_tls_inited = true;
    }

    bool begun = useTls ? s_http.begin(s_tlsClient, url)
                        : s_http.begin(s_plainClient, url);
    if (!begun) return PostResult::TLS_FAIL;

    s_http.setReuse(true);
    s_http.setConnectTimeout(CONNECT_TIMEOUT_MS);
    s_http.setTimeout(IO_TIMEOUT_MS);

    s_http.addHeader("Content-Type", "application/octet-stream");
    s_http.addHeader("X-Device-ID",  g_cfg.device_id);
    s_http.addHeader("X-Timestamp",  timestamp);
    s_http.addHeader("X-Nonce",      nonce);
    s_http.addHeader("X-HMAC",       hmac_hex);

    int code = s_http.POST(const_cast<uint8_t*>(body), body_len);

    if (code > 0 && response_buf && response_buf_len > 0) {
        String resp = s_http.getString();
        strlcpy(response_buf, resp.c_str(), response_buf_len);
    }
    s_http.end();

    if (code == 200) return PostResult::OK;
    if (code == 401) return PostResult::UNAUTHORIZED;
    if (code >= 500) return PostResult::SERVER_ERROR;
    if (code  <  0)  return PostResult::NETWORK_FAIL;
    return PostResult::SERVER_ERROR;
}

PostResult hmac_post_health() {
    static const char EMPTY_SHA[] =
        "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";

    char timestamp[16];
    snprintf(timestamp, sizeof(timestamp), "%lu", (unsigned long)time(nullptr));
    char nonce[37];
    make_nonce_uuid(nonce);

    char hmac_hex[65];
    compute_hmac(timestamp, nonce, EMPTY_SHA, hmac_hex);

    char url[160];
    snprintf(url, sizeof(url), "%s/api/health", g_cfg.server_url);

    static const uint8_t EMPTY_BODY[1] = {0};
    char body[128] = {};
    PostResult rc = http_post(url, timestamp, nonce, hmac_hex,
                              EMPTY_BODY, 0, body, sizeof(body));
    Serial.printf("[HEALTH] rc=%d body: %s\n", (int)rc, body);
    return rc;
}

PostResult hmac_post_scan(const uint8_t* image, uint32_t image_len,
                           char* response_buf, size_t response_buf_len) {
    uint8_t body_sha[32];
    mbedtls_sha256(image, image_len, body_sha, 0);
    char body_sha_hex[65];
    bytes_to_hex(body_sha, 32, body_sha_hex);

    char timestamp[16];
    snprintf(timestamp, sizeof(timestamp), "%lu", (unsigned long)time(nullptr));
    char nonce[37];
    make_nonce_uuid(nonce);

    char hmac_hex[65];
    compute_hmac(timestamp, nonce, body_sha_hex, hmac_hex);

    char url[160];
    snprintf(url, sizeof(url), "%s/api/scan", g_cfg.server_url);

    return http_post(url, timestamp, nonce, hmac_hex,
                     image, image_len, response_buf, response_buf_len);
}
