package com.example.attendance.domain.attendance;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ScanEventRepository extends JpaRepository<ScanEvent, UUID> {
    Optional<ScanEvent> findByDeviceIdAndScanNonce(UUID deviceId, String scanNonce);
    long countByStudentId(UUID studentId);

    @Query("SELECT COUNT(e) FROM ScanEvent e WHERE e.labSession.scheduledLab.id = :scheduledLabId")
    long countByScheduledLabId(@Param("scheduledLabId") UUID scheduledLabId);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM ScanEvent e WHERE e.student.id = :studentId")
    void deleteByStudentId(@Param("studentId") UUID studentId);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM ScanEvent e WHERE e.labSession.id IN "
         + "(SELECT ls.id FROM LabSession ls WHERE ls.scheduledLab.id = :scheduledLabId)")
    void deleteByScheduledLabId(@Param("scheduledLabId") UUID scheduledLabId);

    @Query("""
        SELECT e FROM ScanEvent e
        LEFT JOIN FETCH e.student
        WHERE e.labSession.id = :sessionId
        ORDER BY e.scannedAt
        """)
    List<ScanEvent> findByLabSessionId(@Param("sessionId") UUID sessionId);
}
