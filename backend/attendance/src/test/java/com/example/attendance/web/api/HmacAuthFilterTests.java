package com.example.attendance.web.api;

import com.example.attendance.domain.academic.Room;
import com.example.attendance.domain.academic.RoomRepository;
import com.example.attendance.domain.device.Device;
import com.example.attendance.domain.device.DeviceRepository;
import com.example.attendance.support.AbstractIntegrationTest;
import com.example.attendance.support.HmacRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HmacAuthFilterTests extends AbstractIntegrationTest {
    @Autowired RoomRepository roomRepository;
    @Autowired DeviceRepository deviceRepository;

    static final String SECRET = "test-secret-0123456789abcdef0123";
    String deviceId;

    @BeforeEach
    void seed() {
        Room room = roomRepository.save(new Room("IT-ROOM-" + UUID.randomUUID(), "B"));
        Device d = deviceRepository.save(new Device(room, SECRET));
        deviceId = d.getId().toString();
    }

    private int healthStatus(Map<String, String> headers) {
        return postBytes("/api/health", headers, new byte[0]).status();
    }

    @Test
    void missingHeadersIs401() {
        Map<String, String> h = new LinkedHashMap<>();
        h.put("Content-Type", "application/octet-stream");
        assertEquals(401, healthStatus(h));
    }

    @Test
    void validHmacHealthIs200() {
        assertEquals(200, healthStatus(HmacRequest.headers(deviceId, SECRET, new byte[0])));
    }

    @Test
    void wrongSecretIs401() {
        assertEquals(401, healthStatus(HmacRequest.headers(deviceId, "wrong-secret", new byte[0])));
    }

    @Test
    void timestampSkewBeyondWindowIs401() {
        long old = Instant.now().getEpochSecond() - 1000;
        assertEquals(401, healthStatus(
                HmacRequest.headers(deviceId, SECRET, new byte[0], old, UUID.randomUUID().toString())));
    }

    @Test
    void replayedNonceIs401() {
        long ts = Instant.now().getEpochSecond();
        String nonce = UUID.randomUUID().toString();
        assertEquals(200, healthStatus(HmacRequest.headers(deviceId, SECRET, new byte[0], ts, nonce)));

        assertEquals(401, healthStatus(HmacRequest.headers(deviceId, SECRET, new byte[0], ts, nonce)));
    }

    @Test
    void unknownDeviceIs401() {
        assertEquals(401, healthStatus(
                HmacRequest.headers(UUID.randomUUID().toString(), SECRET, new byte[0])));
    }
}
