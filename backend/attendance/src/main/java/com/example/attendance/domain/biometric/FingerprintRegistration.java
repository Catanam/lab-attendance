package com.example.attendance.domain.biometric;

import com.example.attendance.domain.student.Student;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "fingerprint_registrations")
public class FingerprintRegistration {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(name = "registered_at", nullable = false)
    private LocalDateTime registeredAt;

    @Column(nullable = false)
    private boolean active = true;

    @OneToMany(mappedBy = "registration", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FingerprintTemplateBlob> blobs = new ArrayList<>();

    protected FingerprintRegistration() {}

    public FingerprintRegistration(Student student) {
        this.student = student;
        this.registeredAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public Student getStudent() { return student; }
    public LocalDateTime getRegisteredAt() { return registeredAt; }
    public boolean isActive() { return active; }
    public List<FingerprintTemplateBlob> getBlobs() { return blobs; }
    public void setActive(boolean active) { this.active = active; }
}
