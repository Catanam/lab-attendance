package com.example.attendance.domain.schedule;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface LabSubstitutionRepository extends JpaRepository<LabSubstitution, UUID> {
    @Modifying(clearAutomatically = true)
    @Query(value = "DELETE FROM lab_substitutions WHERE scheduled_lab_id = :id", nativeQuery = true)
    void deleteByScheduledLabId(@Param("id") UUID scheduledLabId);
}
