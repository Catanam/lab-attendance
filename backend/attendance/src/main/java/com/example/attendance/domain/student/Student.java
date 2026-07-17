package com.example.attendance.domain.student;

import com.example.attendance.domain.academic.StudentGroup;
import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "students")
public class Student {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "matricol_number", unique = true, nullable = false)
    private String matricolNumber;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    private String email;

    private boolean active = true;

    @Column(name = "year_of_study")
    private Integer yearOfStudy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "primary_group_id")
    private StudentGroup primaryGroup;

    protected Student() {}

    public Student(String matricolNumber, String fullName, String email) {
        this.matricolNumber = matricolNumber;
        this.fullName = fullName;
        this.email = email;
    }

    public UUID getId() { return id; }
    public String getMatricolNumber() { return matricolNumber; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public boolean isActive() { return active; }
    public Integer getYearOfStudy() { return yearOfStudy; }
    public StudentGroup getPrimaryGroup() { return primaryGroup; }

    public void setMatricolNumber(String v) { this.matricolNumber = v; }
    public void setFullName(String v) { this.fullName = v; }
    public void setEmail(String v) { this.email = v; }
    public void setActive(boolean v) { this.active = v; }
    public void setYearOfStudy(Integer v) { this.yearOfStudy = v; }
    public void setPrimaryGroup(StudentGroup v) { this.primaryGroup = v; }
}
