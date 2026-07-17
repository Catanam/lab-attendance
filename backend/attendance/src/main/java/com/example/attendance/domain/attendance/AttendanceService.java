package com.example.attendance.domain.attendance;

import com.example.attendance.domain.attendance.dto.AttendanceExportRowDTO;
import com.example.attendance.domain.attendance.dto.AttendanceRowDTO;
import com.example.attendance.domain.attendance.dto.SessionAttendanceDTO;
import com.example.attendance.domain.attendance.dto.TodaySessionDTO;
import com.example.attendance.domain.schedule.LabSession;
import com.example.attendance.domain.schedule.LabSessionRepository;
import com.example.attendance.domain.schedule.ScheduledLab;
import com.example.attendance.domain.student.Student;
import com.example.attendance.domain.student.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class AttendanceService {
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final LabSessionRepository labSessionRepository;
    private final ScanEventRepository scanEventRepository;
    private final StudentRepository studentRepository;
    private final GracePolicy gracePolicy;

    public AttendanceService(LabSessionRepository labSessionRepository,
                             ScanEventRepository scanEventRepository,
                             StudentRepository studentRepository,
                             GracePolicy gracePolicy) {
        this.labSessionRepository = labSessionRepository;
        this.scanEventRepository = scanEventRepository;
        this.studentRepository = studentRepository;
        this.gracePolicy = gracePolicy;
    }

    @Transactional(readOnly = true)
    public List<TodaySessionDTO> todaySessions(LocalDateTime now, Set<UUID> courseFilter) {
        return labSessionRepository.findTodaySessions(now).stream()
            .filter(ls -> courseFilter == null
                || courseFilter.contains(ls.getScheduledLab().getCourse().getId()))
            .map(ls -> {
                ScheduledLab sl = ls.getScheduledLab();
                return new TodaySessionDTO(
                    ls.getId(),
                    sl.getCourse().getCode(),
                    sl.getCourse().getName(),
                    sl.getGroup().getName(),
                    sl.getYearOfStudy(),
                    sl.getRoom().getName(),
                    ls.getStartAt(),
                    ls.getEndAt(),
                    ls.getStatus());
            })
            .toList();
    }

    private static final DateTimeFormatter START_FMT = DateTimeFormatter.ofPattern("HH:mm");

    @Transactional(readOnly = true)
    public List<AttendanceExportRowDTO> buildAllAttendance(LocalDate from, LocalDate to, Set<UUID> courseFilter) {
        List<LabSession> sessions = labSessionRepository.findAllWithDetails(from, to);
        Map<String, List<Student>> rosterCache = new HashMap<>();
        List<AttendanceExportRowDTO> rows = new ArrayList<>();

        for (LabSession session : sessions) {
            ScheduledLab sl = session.getScheduledLab();
            if (courseFilter != null && !courseFilter.contains(sl.getCourse().getId())) continue;

            String rosterKey = sl.getGroup().getId() + ":" + sl.getYearOfStudy();
            LocalDateTime start = session.getStartAt();
            String startStr = start.format(START_FMT);

            Map<UUID, ScanEvent> firstScanByStudent = new LinkedHashMap<>();
            for (ScanEvent e : scanEventRepository.findByLabSessionId(session.getId())) {
                if (e.getKind() == ScanEventKind.UNMATCHED || e.getStudent() == null) continue;
                firstScanByStudent.putIfAbsent(e.getStudent().getId(), e);
            }

            List<Student> roster = rosterCache.computeIfAbsent(rosterKey, k -> rosterFor(sl));
            Set<UUID> enrolledIds = new HashSet<>();

            for (Student s : roster) {
                enrolledIds.add(s.getId());
                ScanEvent scan = firstScanByStudent.get(s.getId());
                AttendanceStatus status = gracePolicy.classify(start, scan == null ? null : scan.getScannedAt());
                rows.add(new AttendanceExportRowDTO(
                    session.getSessionDate(), startStr,
                    sl.getCourse().getCode(), sl.getCourse().getName(),
                    sl.getGroup().getName(), sl.getRoom().getName(),
                    s.getMatricolNumber(), s.getFullName(), status,
                    scan == null ? null : scan.getScannedAt().format(TIME_FMT), false));
            }

            for (ScanEvent e : firstScanByStudent.values()) {
                Student st = e.getStudent();
                if (enrolledIds.contains(st.getId())) continue;
                AttendanceStatus status = gracePolicy.classify(start, e.getScannedAt());
                rows.add(new AttendanceExportRowDTO(
                    session.getSessionDate(), startStr,
                    sl.getCourse().getCode(), sl.getCourse().getName(),
                    sl.getGroup().getName(), sl.getRoom().getName(),
                    st.getMatricolNumber(), st.getFullName(), status,
                    e.getScannedAt().format(TIME_FMT), true));
            }
        }
        return rows;
    }

    @Transactional(readOnly = true)
    public SessionAttendanceDTO buildSessionAttendance(UUID sessionId) {
        LabSession session = labSessionRepository.findByIdWithDetails(sessionId)
            .orElseThrow(() -> new IllegalArgumentException("Lab session not found: " + sessionId));
        ScheduledLab sl = session.getScheduledLab();
        LocalDateTime start = session.getStartAt();

        List<ScanEvent> scans = scanEventRepository.findByLabSessionId(sessionId);
        Map<UUID, ScanEvent> firstScanByStudent = new LinkedHashMap<>();
        for (ScanEvent e : scans) {
            if (e.getKind() == ScanEventKind.UNMATCHED || e.getStudent() == null) continue;
            firstScanByStudent.putIfAbsent(e.getStudent().getId(), e);
        }

        List<Student> enrolledStudents = rosterFor(sl);
        List<AttendanceRowDTO> enrolled = new ArrayList<>();
        int present = 0, late = 0, absent = 0;
        for (Student s : enrolledStudents) {
            ScanEvent scan = firstScanByStudent.get(s.getId());
            AttendanceStatus status = gracePolicy.classify(start, scan == null ? null : scan.getScannedAt());
            switch (status) {
                case PRESENT -> present++;
                case LATE -> late++;
                case ABSENT -> absent++;
            }
            enrolled.add(new AttendanceRowDTO(
                s.getId(), s.getMatricolNumber(), s.getFullName(), status,
                scan == null ? null : scan.getScannedAt().format(TIME_FMT),
                false,
                scan != null && scan.isShouldCount(),
                scan == null ? null : scan.getId()));
        }

        List<AttendanceRowDTO> guests = new ArrayList<>();
        for (ScanEvent e : firstScanByStudent.values()) {
            Student st = e.getStudent();
            boolean inGroup = enrolledStudents.stream().anyMatch(es -> es.getId().equals(st.getId()));
            if (inGroup) continue;
            AttendanceStatus status = gracePolicy.classify(start, e.getScannedAt());
            guests.add(new AttendanceRowDTO(
                st.getId(), st.getMatricolNumber(), st.getFullName(), status,
                e.getScannedAt().format(TIME_FMT),
                true, e.isShouldCount(), e.getId()));
        }

        return new SessionAttendanceDTO(
            session.getId(),
            sl.getCourse().getCode(), sl.getCourse().getName(),
            sl.getGroup().getName(), sl.getYearOfStudy(), sl.getRoom().getName(),
            session.getSessionDate(), session.getStartAt(), session.getEndAt(),
            session.getStatus(),
            present, late, absent, guests.size(),
            enrolled, guests);
    }

    public AttendanceRowDTO buildRowFor(ScanEvent event, boolean guest) {
        Student s = event.getStudent();
        LocalDateTime start = event.getLabSession().getStartAt();
        AttendanceStatus status = gracePolicy.classify(start, event.getScannedAt());
        return new AttendanceRowDTO(
            s.getId(), s.getMatricolNumber(), s.getFullName(), status,
            event.getScannedAt().format(TIME_FMT),
            guest, event.isShouldCount(), event.getId());
    }

    @Transactional
    public ToggleResult toggleShouldCount(UUID scanEventId) {
        ScanEvent e = scanEventRepository.findById(scanEventId)
            .orElseThrow(() -> new IllegalArgumentException("Scan event not found: " + scanEventId));
        e.setShouldCount(!e.isShouldCount());
        ScheduledLab sl = e.getLabSession().getScheduledLab();
        boolean guest = !inCohort(e.getStudent(), sl);
        AttendanceRowDTO row = buildRowFor(e, guest);
        return new ToggleResult(e.getLabSession().getId(), row);
    }

    public record ToggleResult(UUID sessionId, AttendanceRowDTO row) {}

    private List<Student> rosterFor(ScheduledLab sl) {
        UUID groupId = sl.getGroup().getId();
        Integer year = sl.getYearOfStudy();
        return year == null
            ? studentRepository.findByPrimaryGroupId(groupId)
            : studentRepository.findByPrimaryGroupIdAndYearOfStudy(groupId, year);
    }

    public static boolean inCohort(Student s, ScheduledLab sl) {
        if (s == null || s.getPrimaryGroup() == null
            || !sl.getGroup().getId().equals(s.getPrimaryGroup().getId())) {
            return false;
        }
        return sl.getYearOfStudy() == null || sl.getYearOfStudy().equals(s.getYearOfStudy());
    }
}
