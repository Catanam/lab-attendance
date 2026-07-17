package com.example.attendance.domain.student;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StudentRepository extends JpaRepository<Student, UUID> {
    boolean existsByMatricolNumber(String matricolNumber);
    Optional<Student> findByMatricolNumber(String matricolNumber);

    @Query("SELECT s FROM Student s LEFT JOIN FETCH s.primaryGroup ORDER BY s.matricolNumber")
    List<Student> findAllWithPrimaryGroup();

    @Query("SELECT s FROM Student s LEFT JOIN FETCH s.primaryGroup WHERE s.id = :id")
    Optional<Student> findByIdWithPrimaryGroup(UUID id);

    @Query("SELECT s FROM Student s LEFT JOIN FETCH s.primaryGroup WHERE s.primaryGroup.id = :groupId ORDER BY s.fullName")
    List<Student> findByPrimaryGroupId(UUID groupId);

    @Query("SELECT s FROM Student s LEFT JOIN FETCH s.primaryGroup "
         + "WHERE s.primaryGroup.id = :groupId AND s.yearOfStudy = :year ORDER BY s.fullName")
    List<Student> findByPrimaryGroupIdAndYearOfStudy(UUID groupId, Integer year);

    @Query("""
        SELECT s FROM Student s LEFT JOIN FETCH s.primaryGroup
        WHERE NOT EXISTS (
            SELECT 1 FROM FingerprintRegistration r
            WHERE r.student = s AND r.active = true
        )
        ORDER BY s.fullName
        """)
    List<Student> findWithoutActiveFingerprint();
}
