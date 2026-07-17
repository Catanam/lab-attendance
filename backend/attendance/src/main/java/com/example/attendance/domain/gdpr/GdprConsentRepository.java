package com.example.attendance.domain.gdpr;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GdprConsentRepository extends JpaRepository<GdprConsent, UUID> {
    Optional<GdprConsent> findByStudentId(UUID studentId);

    @Query("SELECT c FROM GdprConsent c JOIN FETCH c.student WHERE c.token = :token")
    Optional<GdprConsent> findByToken(@Param("token") String token);

    boolean existsByStudentIdAndStatus(UUID studentId, GdprConsentStatus status);

    @Query("SELECT c FROM GdprConsent c JOIN FETCH c.student")
    List<GdprConsent> findAllWithStudent();

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM GdprConsent c WHERE c.student.id = :studentId")
    void deleteByStudentId(@Param("studentId") UUID studentId);
}
