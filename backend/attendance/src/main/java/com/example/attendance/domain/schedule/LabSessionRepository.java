package com.example.attendance.domain.schedule;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LabSessionRepository extends JpaRepository<LabSession, UUID> {
    List<LabSession> findByScheduledLabId(UUID scheduledLabId);
    boolean existsByScheduledLabId(UUID scheduledLabId);
    long countByScheduledLabId(UUID scheduledLabId);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM LabSession ls WHERE ls.scheduledLab.id = :scheduledLabId")
    void deleteByScheduledLabId(@Param("scheduledLabId") UUID scheduledLabId);

    @Query("""
        SELECT ls FROM LabSession ls
        JOIN FETCH ls.scheduledLab sl
        JOIN FETCH sl.group
        WHERE sl.room.id = :roomId
          AND ls.status = 'SCHEDULED'
          AND ls.startAt <= :now
          AND ls.endAt   >= :now
        ORDER BY ls.startAt
        """)
    List<LabSession> findActiveSessionsInRoom(@Param("roomId") UUID roomId,
                                              @Param("now") LocalDateTime now);

    @Query("SELECT ls FROM LabSession ls JOIN FETCH ls.scheduledLab sl JOIN FETCH sl.course JOIN FETCH sl.group WHERE sl.room.id = :roomId AND CAST(ls.sessionDate AS date) = CAST(:now AS date) ORDER BY ls.startAt")
    List<LabSession> findTodaySessionsInRoom(@Param("roomId") UUID roomId,
                                             @Param("now") LocalDateTime now);

    @Query("""
        SELECT ls FROM LabSession ls
        JOIN FETCH ls.scheduledLab sl
        JOIN FETCH sl.course
        JOIN FETCH sl.group
        JOIN FETCH sl.room
        WHERE CAST(ls.sessionDate AS date) = CAST(:now AS date)
        ORDER BY ls.startAt
        """)
    List<LabSession> findTodaySessions(@Param("now") LocalDateTime now);

    @Query("""
        SELECT ls FROM LabSession ls
        JOIN FETCH ls.scheduledLab sl
        JOIN FETCH sl.course
        JOIN FETCH sl.group
        JOIN FETCH sl.room
        WHERE ls.id = :id
        """)
    Optional<LabSession> findByIdWithDetails(@Param("id") UUID id);

    @Query("""
        SELECT ls FROM LabSession ls
        JOIN FETCH ls.scheduledLab sl
        JOIN FETCH sl.course
        JOIN FETCH sl.group
        JOIN FETCH sl.room
        WHERE ls.status <> 'CANCELLED'
          AND ls.sessionDate >= :from
          AND ls.sessionDate <= :to
        ORDER BY ls.sessionDate, ls.startAt
        """)
    List<LabSession> findAllWithDetails(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("SELECT sl.course.id FROM LabSession ls JOIN ls.scheduledLab sl WHERE ls.id = :id")
    Optional<UUID> findCourseIdById(@Param("id") UUID id);
}
