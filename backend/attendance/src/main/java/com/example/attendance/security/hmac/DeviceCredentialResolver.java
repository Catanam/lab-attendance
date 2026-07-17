package com.example.attendance.security.hmac;

import com.example.attendance.domain.device.Device;
import com.example.attendance.domain.device.DeviceRepository;
import com.example.attendance.domain.device.DeviceStatus;
import org.springframework.stereotype.Component;
import java.util.Optional;
import java.util.UUID;

@Component
public class DeviceCredentialResolver {
    private final DeviceRepository deviceRepository;

    public DeviceCredentialResolver(DeviceRepository deviceRepository) {
        this.deviceRepository = deviceRepository;
    }

    public Optional<Device> resolveActive(String deviceIdHeader) {
        try {
            UUID id = UUID.fromString(deviceIdHeader);
            return deviceRepository.findByIdAndStatus(id, DeviceStatus.ACTIVE);
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
