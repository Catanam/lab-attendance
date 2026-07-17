package com.example.attendance.domain.academic;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "student_groups")
public class StudentGroup {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    private String year;

    protected StudentGroup() {}

    public StudentGroup(String name, String year) {
        this.name = name;
        this.year = year;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getYear() { return year; }

    public void setName(String v) { this.name = v; }
    public void setYear(String v) { this.year = v; }
}
