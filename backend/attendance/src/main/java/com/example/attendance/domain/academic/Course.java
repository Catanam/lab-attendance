package com.example.attendance.domain.academic;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "courses")
public class Course {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false)
    private String code;

    @Column(nullable = false)
    private String name;

    protected Course() {}

    public Course(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public UUID getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }

    public void setCode(String v) { this.code = v; }
    public void setName(String v) { this.name = v; }
}
