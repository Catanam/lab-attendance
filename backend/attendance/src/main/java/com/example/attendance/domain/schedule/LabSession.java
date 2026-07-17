package com.example.attendance.domain.schedule;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "lab_sessions")
public class LabSession {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "scheduled_lab_id", nullable = false)
    private ScheduledLab scheduledLab;

    @Column(name = "session_date", nullable = false)
    private LocalDate sessionDate;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "end_at", nullable = false)
    private LocalDateTime endAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LabSessionStatus status = LabSessionStatus.SCHEDULED;

    protected LabSession() {}

    public LabSession(ScheduledLab scheduledLab, LocalDate sessionDate,
                      LocalDateTime startAt, LocalDateTime endAt) {
        this.scheduledLab = scheduledLab;
        this.sessionDate = sessionDate;
        this.startAt = startAt;
        this.endAt = endAt;
    }

    public UUID getId() { return id; }
    public ScheduledLab getScheduledLab() { return scheduledLab; }
    public LocalDate getSessionDate() { return sessionDate; }
    public LocalDateTime getStartAt() { return startAt; }
    public LocalDateTime getEndAt() { return endAt; }
    public LabSessionStatus getStatus() { return status; }

    public void setStatus(LabSessionStatus v) { this.status = v; }
}
