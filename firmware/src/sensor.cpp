#include "sensor.h"
#include <Arduino.h>
#include <HardwareSerial.h>
#include <Adafruit_Fingerprint.h>

static HardwareSerial mySerial(2);
static Adafruit_Fingerprint finger(&mySerial);

#define SENSOR_TARGET_BAUD 115200u

static const byte handshakePacket[] = {0xEF,0x01,0xFF,0xFF,0xFF,0xFF,0x01,0x00,0x03,0x0F,0x00,0x13};

static const byte set57600Packet[]  = {0xEF,0x01,0xFF,0xFF,0xFF,0xFF,0x01,0x00,0x05,0x0E,0x04,0x06,0x00,0x1E};
static const byte set115200Packet[] = {0xEF,0x01,0xFF,0xFF,0xFF,0xFF,0x01,0x00,0x05,0x0E,0x04,0x0C,0x00,0x24};

static uint32_t s_sensor_baud = SENSOR_TARGET_BAUD;

static bool sensor_probe(uint32_t baud) {
    mySerial.begin(baud, SERIAL_8N1, SENSOR_RX_PIN, SENSOR_TX_PIN);
    delay(50);
    while (mySerial.available()) mySerial.read();
    mySerial.write(handshakePacket, sizeof(handshakePacket));
    bool ok = false;
    unsigned long start = millis();
    while (millis() - start < 120) {
        if (mySerial.available() >= 2) {
            if (mySerial.read() == 0xEF && mySerial.peek() == 0x01) { ok = true; break; }
        }
    }
    mySerial.end();
    delay(20);
    return ok;
}

static long sensor_detect_baud() {
    static const uint32_t candidates[] = {SENSOR_TARGET_BAUD, 57600, 9600, 19200, 38400, 1200};
    for (size_t i = 0; i < sizeof(candidates) / sizeof(candidates[0]); i++) {
        if (sensor_probe(candidates[i])) return (long)candidates[i];
    }
    return 0;
}

static void sensor_send_setbaud(uint32_t at_baud, const byte* pkt, size_t len) {
    mySerial.begin(at_baud, SERIAL_8N1, SENSOR_RX_PIN, SENSOR_TX_PIN);
    delay(50);
    for (int k = 0; k < 3; k++) { mySerial.write(pkt, len); delay(50); }
    mySerial.end();
    delay(500);
}

void sensor_ensure_high_speed() {
    Serial.println("[Sensor] Auto-detecting baud rate...");
    long found = sensor_detect_baud();
    if (!found) {
        Serial.println("[FATAL] Sensor not found at any baud — check wiring RX=16 TX=17");
        while (true) delay(1000);
    }
    Serial.printf("[Sensor] Found at %ld baud\n", found);

    if (found != (long)SENSOR_TARGET_BAUD) {
        Serial.printf("[Sensor] Switching %ld -> %u...\n", found, SENSOR_TARGET_BAUD);
        sensor_send_setbaud((uint32_t)found, set115200Packet, sizeof(set115200Packet));

        long now = sensor_detect_baud();
        if (!now) {
            Serial.println("[FATAL] Sensor lost after baud switch");
            while (true) delay(1000);
        }
        found = now;
        if (found == (long)SENSOR_TARGET_BAUD)
            Serial.println("[Sensor] Switch confirmed at 115200.");
        else
            Serial.printf("[Sensor] Switch did NOT take — running at %ld\n", found);
    } else {
        Serial.println("[Sensor] Already at 115200.");
    }
    s_sensor_baud = (uint32_t)found;
}

void sensor_init() {
    mySerial.setRxBufferSize(16384);
    mySerial.begin(s_sensor_baud, SERIAL_8N1, SENSOR_RX_PIN, SENSOR_TX_PIN);
    finger.begin(s_sensor_baud);
    delay(100);
    if (!finger.verifyPassword()) {
        Serial.printf("[FATAL] Sensor password verification failed at %u\n", s_sensor_baud);
        while (true) delay(1000);
    }
    Serial.printf("[OK] Sensor ready at %u\n", s_sensor_baud);
}

bool sensor_finger_present() {
    return finger.getImage() == FINGERPRINT_OK;
}

static void sensor_drain_to_idle(uint32_t quiet_ms) {
    unsigned long last = millis();
    while (millis() - last < quiet_ms) {
        if (mySerial.available()) {
            mySerial.read();
            last = millis();
        } else {
            vTaskDelay(1);
        }
    }
}

bool sensor_capture(uint8_t* buf, uint32_t* actual_size_out, volatile uint32_t* progress_out) {
    sensor_drain_to_idle(60);

    byte packet[] = {0xEF,0x01,0xFF,0xFF,0xFF,0xFF,0x01,0x00,0x03,0x0A,0x00,0x0E};
    mySerial.write(packet, sizeof(packet));

    uint32_t actualSize = 0;
    if (progress_out) *progress_out = 0;
    unsigned long timeout = millis();

    while (millis() - timeout < 3000) {
        if (mySerial.available() >= 9) {
            if (mySerial.read() != 0xEF) continue;
            if (mySerial.read() != 0x01) continue;
            for (int i = 0; i < 4; i++) mySerial.read();
            byte pid   = mySerial.read();
            byte lenHi = mySerial.read();
            byte lenLo = mySerial.read();
            uint16_t len = ((uint16_t)lenHi << 8) | lenLo;
            if (len < 2) return false;
            uint16_t dataChunkLen = len - 2;

            uint32_t sum = (uint32_t)pid + lenHi + lenLo;

            for (int i = 0; i < dataChunkLen; i++) {
                unsigned long byteTimeout = millis();
                while (!mySerial.available()) {
                    if (millis() - byteTimeout > 100) return false;
                    vTaskDelay(1);
                }
                byte b = mySerial.read();
                sum += b;
                if (actualSize < SENSOR_RAW_SIZE) buf[actualSize++] = b;
            }
            if (progress_out) *progress_out = actualSize;

            unsigned long ckWait = millis();
            while (mySerial.available() < 2) {
                if (millis() - ckWait > 100) return false;
                vTaskDelay(1);
            }
            byte ckHi = mySerial.read();
            byte ckLo = mySerial.read();
            uint16_t rxck = ((uint16_t)ckHi << 8) | ckLo;
            if ((uint16_t)(sum & 0xFFFF) != rxck) {
                Serial.printf("[capture] checksum mismatch (calc=%04X rx=%04X)\n",
                              (unsigned)(sum & 0xFFFF), (unsigned)rxck);
                return false;
            }

            timeout = millis();

            if (pid == 0x08 || actualSize >= SENSOR_RAW_SIZE) {
                *actual_size_out = actualSize;
                return true;
            }
        } else {
            vTaskDelay(1);
        }
    }
    return false;
}
