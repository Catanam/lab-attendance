package com.example.attendance.domain.schedule;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "lab_substitutions")
public class LabSubstitution {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private LocalDate date;

    protected LabSubstitution() {}

    public UUID getId() { return id; }
    public LocalDate getDate() { return date; }
}
