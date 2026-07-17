package com.example.attendance.domain.biometric;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface FingerprintRegistrationRepository extends JpaRepository<FingerprintRegistration, UUID> {
    List<FingerprintRegistration> findByStudentIdAndActiveTrue(UUID studentId);
    List<FingerprintRegistration> findByStudentId(UUID studentId);
    long countByStudentId(UUID studentId);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM FingerprintRegistration r WHERE r.student.id = :studentId")
    void deleteByStudentId(@Param("studentId") UUID studentId);

    @Query("""
        SELECT DISTINCT r.student.id FROM FingerprintRegistration r
        WHERE (
            SELECT MAX(t.endDate) FROM ScheduledLab sl JOIN sl.term t
            WHERE sl.group = r.student.primaryGroup
        ) < :cutoff
        """)
    List<UUID> findStudentIdsEligibleForPurge(LocalDate cutoff);
}
