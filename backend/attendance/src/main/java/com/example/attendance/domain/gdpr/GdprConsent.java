package com.example.attendance.domain.gdpr;

import com.example.attendance.domain.student.Student;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "gdpr_consents")
public class GdprConsent {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false, unique = true)
    private Student student;

    @Column(nullable = false, unique = true)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GdprConsentStatus status = GdprConsentStatus.PENDING;

    @Column(name = "text_version_hash", nullable = false)
    private String textVersionHash;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    protected GdprConsent() {}

    public GdprConsent(Student student) {
        this.student = student;
    }

    public void issue(String token, String textVersionHash) {
        this.token = token;
        this.textVersionHash = textVersionHash;
        this.status = GdprConsentStatus.PENDING;
        this.sentAt = LocalDateTime.now();
        this.confirmedAt = null;
    }

    public void accept() {
        this.status = GdprConsentStatus.ACCEPTED;
        this.confirmedAt = LocalDateTime.now();
    }

    public void decline() {
        this.status = GdprConsentStatus.DECLINED;
        this.confirmedAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public Student getStudent() { return student; }
    public String getToken() { return token; }
    public GdprConsentStatus getStatus() { return status; }
    public String getTextVersionHash() { return textVersionHash; }
    public LocalDateTime getSentAt() { return sentAt; }
    public LocalDateTime getConfirmedAt() { return confirmedAt; }

    public boolean isAccepted() { return status == GdprConsentStatus.ACCEPTED; }
    public boolean isPending()  { return status == GdprConsentStatus.PENDING; }
}
