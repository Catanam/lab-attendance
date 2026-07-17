#include "feedback.h"
#include <Arduino.h>

struct Step { uint8_t r, g, b; bool buzz; uint32_t dur_ms; };

static const Step SEQ_REGULAR_OK[] = {
    {0, 255, 0, true,  200},
    {0,   0, 0, false, 100},
    {0,   0, 0, false,   0},
};
static const Step SEQ_NO_MATCH[] = {
    {255, 0, 0, true,  150},
    {  0, 0, 0, false,  80},
    {255, 0, 0, true,  150},
    {  0, 0, 0, false,   0},
};
static const Step SEQ_GUEST[] = {
    {0, 0, 255, true,  200},
    {0, 0,   0, false,   0},
};
static const Step SEQ_ERROR[] = {
    {255, 0, 0, true,  600},
    {  0, 0, 0, false,   0},
};
static const Step SEQ_OFFLINE[] = {
    {255, 80, 0, false, 400},
    {  0,  0, 0, false,   0},
};
static const Step SEQ_REGISTER_SAMPLE_OK[] = {
    {0, 0, 255, true,  120},
    {0, 0,   0, false,   0},
};
static const Step SEQ_REGISTER_DUPLICATE[] = {
    {255, 80, 0, true,  120},
    {  0,  0, 0, false,  80},
    {255, 80, 0, true,  120},
    {  0,  0, 0, false,   0},
};
static const Step SEQ_REGISTER_COMPLETE[] = {
    {0, 255, 0, true,  180},
    {0,   0, 0, false,  80},
    {0, 255, 0, true,  180},
    {0,   0, 0, false,  80},
    {0, 255, 0, true,  240},
    {0,   0, 0, false,   0},
};

static const Step* s_seq     = nullptr;
static int         s_step    = 0;
static uint32_t    s_step_at = 0;

static void set_led(uint8_t r, uint8_t g, uint8_t b) {
    ledcWrite(FEEDBACK_LED_R_PIN, r);
    ledcWrite(FEEDBACK_LED_G_PIN, g);
    ledcWrite(FEEDBACK_LED_B_PIN, b);
}

static void apply(const Step& s) {
    set_led(s.r, s.g, s.b);
    if (s.buzz) tone(FEEDBACK_BUZZER_PIN, 2000);
    else        noTone(FEEDBACK_BUZZER_PIN);
}

static void leds_off() {
    set_led(0, 0, 0);
    noTone(FEEDBACK_BUZZER_PIN);
}

void feedback_init() {
    ledcSetup(0, 5000, 8); ledcAttachPin(FEEDBACK_LED_R_PIN, 0);
    ledcSetup(1, 5000, 8); ledcAttachPin(FEEDBACK_LED_G_PIN, 1);
    ledcSetup(2, 5000, 8); ledcAttachPin(FEEDBACK_LED_B_PIN, 2);
    pinMode(FEEDBACK_BUZZER_PIN, OUTPUT);
    leds_off();
}

void feedback_trigger(FeedbackEvent event) {
    switch (event) {
        case FeedbackEvent::REGULAR_OK:         s_seq = SEQ_REGULAR_OK;         break;
        case FeedbackEvent::NO_MATCH:           s_seq = SEQ_NO_MATCH;           break;
        case FeedbackEvent::GUEST:              s_seq = SEQ_GUEST;              break;
        case FeedbackEvent::ERROR:              s_seq = SEQ_ERROR;              break;
        case FeedbackEvent::OFFLINE:            s_seq = SEQ_OFFLINE;            break;
        case FeedbackEvent::REGISTER_SAMPLE_OK: s_seq = SEQ_REGISTER_SAMPLE_OK; break;
        case FeedbackEvent::REGISTER_DUPLICATE: s_seq = SEQ_REGISTER_DUPLICATE; break;
        case FeedbackEvent::REGISTER_COMPLETE:  s_seq = SEQ_REGISTER_COMPLETE;  break;
    }
    s_step    = 0;
    s_step_at = millis();
    apply(s_seq[0]);
}

void feedback_tick() {
    if (!s_seq) return;
    const Step& cur = s_seq[s_step];
    if (cur.dur_ms == 0) { leds_off(); s_seq = nullptr; return; }
    if ((millis() - s_step_at) >= cur.dur_ms) {
        s_step++;
        s_step_at = millis();
        apply(s_seq[s_step]);
    }
}
