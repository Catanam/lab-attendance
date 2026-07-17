#pragma once
#include <stdint.h>
#include <stddef.h>

enum class PostResult {
    OK           =  200,
    UNAUTHORIZED =  401,
    SERVER_ERROR =  500,
    NETWORK_FAIL =   -1,
    TLS_FAIL     =   -2,
};

PostResult hmac_post_scan(
    const uint8_t* image,
    uint32_t       image_len,
    char*          response_buf,
    size_t         response_buf_len
);

PostResult hmac_post_health();
