package com.example.attendance.domain.academic;

import com.example.attendance.domain.staff.Professor;
import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "course_teachers",
       uniqueConstraints = @UniqueConstraint(columnNames = {"course_id", "professor_id"}))
public class CourseTeacher {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "professor_id", nullable = false)
    private Professor professor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CourseTeacherRole role;

    protected CourseTeacher() {}

    public CourseTeacher(Course course, Professor professor, CourseTeacherRole role) {
        this.course = course;
        this.professor = professor;
        this.role = role;
    }

    public UUID getId() { return id; }
    public Course getCourse() { return course; }
    public Professor getProfessor() { return professor; }
    public CourseTeacherRole getRole() { return role; }

    public void setRole(CourseTeacherRole v) { this.role = v; }
}
