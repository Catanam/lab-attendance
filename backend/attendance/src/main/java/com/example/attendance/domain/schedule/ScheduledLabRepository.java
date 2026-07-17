package com.example.attendance.domain.schedule;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.UUID;

public interface ScheduledLabRepository extends JpaRepository<ScheduledLab, UUID> {
    @Query("""
        SELECT s FROM ScheduledLab s
        JOIN FETCH s.term JOIN FETCH s.course JOIN FETCH s.group JOIN FETCH s.room
        WHERE s.term.id = :termId
        ORDER BY s.weekday, s.startTime
        """)
    List<ScheduledLab> findByTermIdWithDetails(UUID termId);
}
