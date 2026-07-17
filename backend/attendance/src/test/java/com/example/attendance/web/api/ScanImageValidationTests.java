package com.example.attendance.web.api;

import com.example.attendance.domain.academic.Room;
import com.example.attendance.domain.academic.RoomRepository;
import com.example.attendance.domain.biometric.RegistrationSessionHolder;
import com.example.attendance.domain.device.Device;
import com.example.attendance.domain.device.DeviceRepository;
import com.example.attendance.support.AbstractIntegrationTest;
import com.example.attendance.support.HmacRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScanImageValidationTests extends AbstractIntegrationTest {
    @Autowired RoomRepository roomRepository;
    @Autowired DeviceRepository deviceRepository;
    @Autowired RegistrationSessionHolder sessionHolder;

    static final String SECRET = "test-secret-0123456789abcdef0123";
    String deviceId;
    UUID roomId;

    @BeforeEach
    void seed() {
        Room room = roomRepository.save(new Room("SCAN-ROOM-" + UUID.randomUUID(), "B"));
        roomId = room.getId();
        Device d = deviceRepository.save(new Device(room, SECRET));
        deviceId = d.getId().toString();
        sessionHolder.clear(roomId);
    }

    private Resp postScan(byte[] body) {
        return postBytes("/api/scan", HmacRequest.headers(deviceId, SECRET, body), body);
    }

    @Test
    void shortImageNoSessionIsUnmatchedNotRecorded() {
        Resp r = postScan(new byte[100]);
        assertEquals(200, r.status());
        assertTrue(r.body().contains("UNMATCHED"), r.body());
        assertTrue(r.body().contains("malformed"), r.body());
    }

    @Test
    void shortImageDuringRegistrationAsksForRetry() {
        sessionHolder.start(roomId, UUID.randomUUID());
        Resp r = postScan(new byte[100]);
        assertEquals(200, r.status());
        assertTrue(r.body().contains("REGISTER_RETRY"), r.body());
    }
}
