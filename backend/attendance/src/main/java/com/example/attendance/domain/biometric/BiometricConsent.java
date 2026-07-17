package com.example.attendance.domain.biometric;

import com.example.attendance.domain.auth.AppUser;
import com.example.attendance.domain.student.Student;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "biometric_consents")
public class BiometricConsent {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(name = "consented_at", nullable = false)
    private LocalDateTime consentedAt;

    @Column(name = "text_version_hash", nullable = false)
    private String textVersionHash;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "operator_id", nullable = false)
    private AppUser operator;

    @Column(name = "withdrawn_at")
    private LocalDateTime withdrawnAt;

    protected BiometricConsent() {}

    public BiometricConsent(Student student, AppUser operator, String textVersionHash) {
        this.student = student;
        this.operator = operator;
        this.textVersionHash = textVersionHash;
        this.consentedAt = LocalDateTime.now();
    }

    public void withdraw() {
        if (this.withdrawnAt == null) {
            this.withdrawnAt = LocalDateTime.now();
        }
    }

    public UUID getId() { return id; }
    public Student getStudent() { return student; }
    public LocalDateTime getConsentedAt() { return consentedAt; }
    public String getTextVersionHash() { return textVersionHash; }
    public AppUser getOperator() { return operator; }
    public LocalDateTime getWithdrawnAt() { return withdrawnAt; }
    public boolean isWithdrawn() { return withdrawnAt != null; }
}
