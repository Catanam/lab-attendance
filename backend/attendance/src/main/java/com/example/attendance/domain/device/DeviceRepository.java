package com.example.attendance.domain.device;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeviceRepository extends JpaRepository<Device, UUID> {
    Optional<Device> findByIdAndStatus(UUID id, DeviceStatus status);

    @Query("SELECT d FROM Device d JOIN FETCH d.room")
    List<Device> findAllWithRoom();

    @Query("SELECT d FROM Device d JOIN FETCH d.room WHERE d.id = :id")
    Optional<Device> findByIdWithRoom(UUID id);
}
