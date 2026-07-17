package com.example.attendance.web.api;

import com.example.attendance.AppClock;
import com.example.attendance.domain.device.DeviceRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class DeviceHealthController {
    private final DeviceRepository deviceRepository;

    public DeviceHealthController(DeviceRepository deviceRepository) {
        this.deviceRepository = deviceRepository;
    }

    @PostMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        String deviceId = SecurityContextHolder.getContext().getAuthentication().getName();
        deviceRepository.findById(UUID.fromString(deviceId)).ifPresent(device -> {
            device.setLastSeenAt(AppClock.now());
            deviceRepository.save(device);
        });
        return ResponseEntity.ok(Map.of(
                "status", "OK",
                "serverTime", AppClock.now().toString()
        ));
    }
}
