package com.example.attendance.domain.academic;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CourseTeacherRepository extends JpaRepository<CourseTeacher, UUID> {
    Optional<CourseTeacher> findByCourseIdAndProfessorId(UUID courseId, UUID professorId);

    boolean existsByCourseIdAndProfessorId(UUID courseId, UUID professorId);

    @Query("SELECT ct FROM CourseTeacher ct JOIN FETCH ct.course JOIN FETCH ct.professor")
    List<CourseTeacher> findAllWithDetails();

    @Query("SELECT ct FROM CourseTeacher ct JOIN FETCH ct.professor WHERE ct.course.id = :courseId")
    List<CourseTeacher> findByCourseIdWithProfessor(UUID courseId);

    @Query("SELECT ct.course.id FROM CourseTeacher ct WHERE ct.professor.id = :professorId")
    List<UUID> findCourseIdsByProfessorId(UUID professorId);

    long countByCourseId(UUID courseId);

    @Query("""
        SELECT ct.course.id FROM CourseTeacher ct
        WHERE ct.professor.id = :professorId
          AND (SELECT COUNT(ct2) FROM CourseTeacher ct2 WHERE ct2.course.id = ct.course.id) = 1
        """)
    List<UUID> findSoleTeacherCourseIds(UUID professorId);
}
