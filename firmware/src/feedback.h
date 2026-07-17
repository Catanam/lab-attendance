#pragma once

enum class FeedbackEvent {
    REGULAR_OK,
    NO_MATCH,
    GUEST,
    ERROR,
    OFFLINE,
    REGISTER_SAMPLE_OK,
    REGISTER_DUPLICATE,
    REGISTER_COMPLETE,
};

#define FEEDBACK_LED_R_PIN  25
#define FEEDBACK_LED_G_PIN  26
#define FEEDBACK_LED_B_PIN  27
#define FEEDBACK_BUZZER_PIN 14

void feedback_init();

void feedback_trigger(FeedbackEvent event);

void feedback_tick();
