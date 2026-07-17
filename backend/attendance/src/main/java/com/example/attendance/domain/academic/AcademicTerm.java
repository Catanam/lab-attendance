package com.example.attendance.domain.academic;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "academic_terms")
public class AcademicTerm {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "is_current", nullable = false)
    private boolean current = false;

    protected AcademicTerm() {}

    public AcademicTerm(String code, String name, LocalDate startDate, LocalDate endDate) {
        this.code = code;
        this.name = name;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public UUID getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public boolean isCurrent() { return current; }

    public void setCode(String v) { this.code = v; }
    public void setName(String v) { this.name = v; }
    public void setStartDate(LocalDate v) { this.startDate = v; }
    public void setEndDate(LocalDate v) { this.endDate = v; }
    public void setCurrent(boolean v) { this.current = v; }
}
