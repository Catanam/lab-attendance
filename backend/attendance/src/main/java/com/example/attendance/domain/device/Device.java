package com.example.attendance.domain.device;

import com.example.attendance.domain.academic.Room;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "devices")
public class Device {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Column(name = "secret_hash", nullable = false)
    private String secretHash;

    @Column(name = "last_seen_at")
    private LocalDateTime lastSeenAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeviceStatus status = DeviceStatus.ACTIVE;

    protected Device() {}

    public Device(Room room, String secretHash) {
        this.room = room;
        this.secretHash = secretHash;
    }

    public UUID getId() { return id; }
    public Room getRoom() { return room; }
    public String getSecretHash() { return secretHash; }
    public LocalDateTime getLastSeenAt() { return lastSeenAt; }
    public DeviceStatus getStatus() { return status; }
    public void setLastSeenAt(LocalDateTime lastSeenAt) { this.lastSeenAt = lastSeenAt; }
    public void setStatus(DeviceStatus status) { this.status = status; }
}
