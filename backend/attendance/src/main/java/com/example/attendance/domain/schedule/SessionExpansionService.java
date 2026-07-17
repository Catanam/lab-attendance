package com.example.attendance.domain.schedule;

import com.example.attendance.domain.academic.AcademicTerm;
import com.example.attendance.domain.academic.AcademicTermRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class SessionExpansionService {
    private final AcademicTermRepository termRepo;
    private final ScheduledLabRepository labRepo;
    private final LabSessionRepository sessionRepo;

    public SessionExpansionService(AcademicTermRepository termRepo,
                                   ScheduledLabRepository labRepo,
                                   LabSessionRepository sessionRepo) {
        this.termRepo    = termRepo;
        this.labRepo     = labRepo;
        this.sessionRepo = sessionRepo;
    }

    @Transactional
    public int expandTerm(UUID termId) {
        AcademicTerm term = termRepo.findById(termId).orElseThrow();
        var labs = labRepo.findByTermIdWithDetails(termId);
        int created = 0;
        for (ScheduledLab lab : labs) {
            if (sessionRepo.existsByScheduledLabId(lab.getId())) continue;
            DayOfWeek dow = DayOfWeek.valueOf(lab.getWeekday());
            LocalDate cursor = nextOrSame(term.getStartDate(), dow);
            while (!cursor.isAfter(term.getEndDate())) {
                LocalDateTime start = cursor.atTime(lab.getStartTime());
                LocalDateTime end   = cursor.atTime(lab.getEndTime());
                sessionRepo.save(new LabSession(lab, cursor, start, end));
                created++;
                cursor = cursor.plusWeeks(1);
            }
        }
        return created;
    }

    private LocalDate nextOrSame(LocalDate from, DayOfWeek dow) {
        int daysUntil = (dow.getValue() - from.getDayOfWeek().getValue() + 7) % 7;
        return from.plusDays(daysUntil);
    }
}
