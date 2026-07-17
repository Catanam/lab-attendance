#include "display.h"
#include <Arduino.h>
#include <Wire.h>
#include <Adafruit_GFX.h>
#include <Adafruit_SSD1306.h>

#define SCREEN_WIDTH  128
#define SCREEN_HEIGHT  64
#define OLED_RESET    -1

#define RESULT_DURATION_MS 3000

static Adafruit_SSD1306 oled(SCREEN_WIDTH, SCREEN_HEIGHT, &Wire, OLED_RESET);
static DisplayState s_state      = DisplayState::BOOTING;
static uint32_t     s_result_at  = 0;
static char         s_detail[32] = {};
static uint8_t      s_progress   = 0;

static bool is_transient(DisplayState s) {
    return s == DisplayState::WELCOME ||
           s == DisplayState::GUEST ||
           s == DisplayState::NO_MATCH ||
           s == DisplayState::UPLOAD_FAIL ||
           s == DisplayState::REGISTER_DUPLICATE ||
           s == DisplayState::REGISTER_COMPLETE;
}

#define CHAR_W(sz) (6 * (sz))
#define LINE_H(sz) (8 * (sz))

static void print_centered(const char* s, uint8_t size, int16_t y) {
    oled.setTextSize(size);
    int16_t x1, y1; uint16_t w, h;
    oled.getTextBounds(s, 0, 0, &x1, &y1, &w, &h);
    int16_t x = ((int16_t)SCREEN_WIDTH - (int16_t)w) / 2;
    if (x < 0) x = 0;
    oled.setCursor(x, y);
    oled.print(s);
}

static void draw_named_result(const char* name, const char* label) {
    if (!name || !name[0]) name = "?";

    int longest = 0, cur = 0;
    for (const char* q = name; ; q++) {
        if (*q == ' ' || *q == '\0') { if (cur > longest) longest = cur; cur = 0; if (!*q) break; }
        else cur++;
    }
    size_t len  = strlen(name);
    uint8_t size = (longest <= SCREEN_WIDTH / CHAR_W(2) && len <= 20) ? 2 : 1;
    int maxChars = SCREEN_WIDTH / CHAR_W(size);

    char lines[5][32];
    int  nLines = 0, lineLen = 0;
    lines[0][0] = '\0';
    const char* p = name;
    while (*p && nLines < 5) {
        const char* w0 = p;
        while (*p && *p != ' ') p++;
        int wlen = (int)(p - w0);
        if (wlen > maxChars) wlen = maxChars;
        int need = (lineLen ? lineLen + 1 : 0) + wlen;
        if (need > maxChars && lineLen) {
            lines[nLines][lineLen] = '\0';
            if (++nLines >= 5) break;
            lineLen = 0;
        }
        if (lineLen) lines[nLines][lineLen++] = ' ';
        memcpy(lines[nLines] + lineLen, w0, wlen);
        lineLen += wlen;
        lines[nLines][lineLen] = '\0';
        while (*p == ' ') p++;
    }
    if (lineLen && nLines < 5) nLines++;
    if (nLines == 0) nLines = 1;

    int16_t nameH  = nLines * LINE_H(size);
    int16_t blockH = nameH + 3 + LINE_H(1);
    int16_t top    = ((int16_t)SCREEN_HEIGHT - blockH) / 2;
    if (top < 0) top = 0;

    for (int i = 0; i < nLines; i++)
        print_centered(lines[i], size, top + i * LINE_H(size));
    print_centered(label, 1, top + nameH + 3);
}

static void draw_scanning(uint8_t pct) {
    print_centered("SCANNING", 2, 4);
    const int16_t x = 14, y = 30, w = SCREEN_WIDTH - 28, h = 12;
    oled.drawRect(x, y, w, h, SSD1306_WHITE);
    int16_t fill = (int16_t)(((int32_t)(w - 2)) * pct / 100);
    if (fill > 0) oled.fillRect(x + 1, y + 1, fill, h - 2, SSD1306_WHITE);
    char buf[8];
    snprintf(buf, sizeof(buf), "%u%%", (unsigned)pct);
    print_centered(buf, 1, y + h + 4);
}

static void render() {
    oled.clearDisplay();
    oled.setTextColor(SSD1306_WHITE);

    switch (s_state) {
        case DisplayState::WELCOME:           draw_named_result(s_detail, "welcome");  oled.display(); return;
        case DisplayState::GUEST:             draw_named_result(s_detail, "guest");    oled.display(); return;
        case DisplayState::REGISTER_COMPLETE: draw_named_result(s_detail, "enrolled"); oled.display(); return;
        case DisplayState::SCANNING:          draw_scanning(s_progress);               oled.display(); return;
        default: break;
    }

    oled.setCursor(0, 0);
    oled.setTextSize(2);

    const char* title = "";
    const char* sub   = nullptr;

    switch (s_state) {
        case DisplayState::BOOTING:            title = "BOOTING";  sub = "..."; break;
        case DisplayState::SYNCING_TIME:       title = "SYNCING";  sub = "time..."; break;
        case DisplayState::READY:              title = "READY";    sub = "place finger"; break;
        case DisplayState::NO_MATCH:           title = "NO MATCH"; sub = "try again"; break;
        case DisplayState::UPLOAD_FAIL:        title = "RETRY";    sub = "send failed"; break;
        case DisplayState::OFFLINE:            title = "OFFLINE";  sub = "no network"; break;
        case DisplayState::NO_TIME:            title = "NO TIME";  sub = "syncing..."; break;
        case DisplayState::REGISTER_PROGRESS:  title = "ENROLL";   sub = s_detail; break;
        case DisplayState::REGISTER_DUPLICATE: title = "MOVE";     sub = "too similar"; break;
        default: break;
    }

    oled.print(title);

    if (sub && sub[0]) {
        oled.setCursor(0, 32);
        oled.setTextSize(1);
        oled.print(sub);
    }

    oled.display();
}

void display_init() {
    Wire.begin();

    if (!oled.begin(SSD1306_SWITCHCAPVCC, 0x3C) &&
        !oled.begin(SSD1306_SWITCHCAPVCC, 0x3D)) {
        Serial.println("[display] SSD1306 not found at 0x3C or 0x3D — check wiring");
        for (;;) delay(1000);
    }
    oled.clearDisplay();
    oled.display();
}

void display_set_state(DisplayState state, const char* detail) {
    s_state = state;
    if (detail) strlcpy(s_detail, detail, sizeof(s_detail));
    else s_detail[0] = '\0';
    if (is_transient(state)) s_result_at = millis();
    render();
}

DisplayState display_get_state() {
    return s_state;
}

void display_scan_progress(uint8_t percent) {
    if (percent > 100) percent = 100;

    int diff = (int)percent - (int)s_progress;
    if (diff < 0) diff = -diff;
    if (s_state == DisplayState::SCANNING && diff < 2 && percent != 100) return;
    s_state    = DisplayState::SCANNING;
    s_progress = percent;
    render();
}

void display_tick() {
    if (is_transient(s_state) && (millis() - s_result_at) >= RESULT_DURATION_MS) {
        display_set_state(DisplayState::READY);
    }
}
