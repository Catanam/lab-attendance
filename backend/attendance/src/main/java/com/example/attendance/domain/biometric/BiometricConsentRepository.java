package com.example.attendance.domain.biometric;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface BiometricConsentRepository extends JpaRepository<BiometricConsent, UUID> {
    Optional<BiometricConsent> findFirstByStudentIdAndWithdrawnAtIsNull(UUID studentId);

    boolean existsByStudentIdAndWithdrawnAtIsNull(UUID studentId);

    boolean existsByOperatorId(UUID operatorId);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM BiometricConsent c WHERE c.student.id = :studentId")
    void deleteByStudentId(@Param("studentId") UUID studentId);
}
