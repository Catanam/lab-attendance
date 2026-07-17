package com.example.attendance.domain.attendance;

import com.example.attendance.domain.device.Device;
import com.example.attendance.domain.schedule.LabSession;
import com.example.attendance.domain.student.Student;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "scan_events",
       uniqueConstraints = @UniqueConstraint(columnNames = {"device_id", "scan_nonce"}))
public class ScanEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    @Column(name = "scan_nonce", nullable = false)
    private String scanNonce;

    @Column(name = "scanned_at", nullable = false)
    private LocalDateTime scannedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lab_session_id")
    private LabSession labSession;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id")
    private Student student;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ScanEventKind kind;

    @Column(name = "match_score", nullable = false)
    private double matchScore;

    @Column(name = "should_count", nullable = false)
    private boolean shouldCount;

    protected ScanEvent() {}

    public ScanEvent(Device device, String scanNonce, LocalDateTime scannedAt,
                     LabSession labSession, Student student, ScanEventKind kind,
                     double matchScore, boolean shouldCount) {
        this.device = device;
        this.scanNonce = scanNonce;
        this.scannedAt = scannedAt;
        this.labSession = labSession;
        this.student = student;
        this.kind = kind;
        this.matchScore = matchScore;
        this.shouldCount = shouldCount;
    }

    public UUID getId() { return id; }
    public Device getDevice() { return device; }
    public String getScanNonce() { return scanNonce; }
    public LocalDateTime getScannedAt() { return scannedAt; }
    public LabSession getLabSession() { return labSession; }
    public Student getStudent() { return student; }
    public ScanEventKind getKind() { return kind; }
    public double getMatchScore() { return matchScore; }
    public boolean isShouldCount() { return shouldCount; }
    public void setShouldCount(boolean shouldCount) { this.shouldCount = shouldCount; }
}
