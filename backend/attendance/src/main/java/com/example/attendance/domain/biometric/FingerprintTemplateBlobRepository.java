package com.example.attendance.domain.biometric;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.UUID;

public interface FingerprintTemplateBlobRepository extends JpaRepository<FingerprintTemplateBlob, UUID> {
    @Query("SELECT b FROM FingerprintTemplateBlob b JOIN FETCH b.registration r JOIN FETCH r.student WHERE r.active = true")
    List<FingerprintTemplateBlob> findAllActive();

    List<FingerprintTemplateBlob> findByRegistrationId(UUID registrationId);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM FingerprintTemplateBlob b WHERE b.registration.id IN " +
           "(SELECT r.id FROM FingerprintRegistration r WHERE r.student.id = :studentId)")
    void deleteByStudentId(@Param("studentId") UUID studentId);
}
