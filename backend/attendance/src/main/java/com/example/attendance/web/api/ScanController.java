package com.example.attendance.web.api;

import com.example.attendance.AppClock;
import com.example.attendance.domain.attendance.AttendanceService;
import com.example.attendance.domain.attendance.ScanEvent;
import com.example.attendance.domain.attendance.ScanEventKind;
import com.example.attendance.domain.attendance.ScanEventRepository;
import com.example.attendance.domain.attendance.dto.AttendanceRowDTO;
import com.example.attendance.domain.biometric.*;
import com.example.attendance.domain.device.Device;
import com.example.attendance.domain.device.DeviceRepository;
import com.example.attendance.domain.schedule.LabSession;
import com.example.attendance.domain.schedule.LabSessionRepository;
import com.example.attendance.domain.student.Student;
import com.example.attendance.domain.student.StudentRepository;
import com.example.attendance.web.sse.SseHub;
import com.machinezoo.sourceafis.FingerprintTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class ScanController {
    private final MatchService matchService;
    private final RegistrationService registrationService;
    private final RegistrationSessionHolder sessionHolder;
    private final LabSessionRepository labSessionRepository;
    private final ScanEventRepository scanEventRepository;
    private final DeviceRepository deviceRepository;
    private final StudentRepository studentRepository;
    private final SseHub sseHub;
    private final AttendanceService attendanceService;

    public ScanController(MatchService matchService,
                          RegistrationService registrationService,
                          RegistrationSessionHolder sessionHolder,
                          LabSessionRepository labSessionRepository,
                          ScanEventRepository scanEventRepository,
                          DeviceRepository deviceRepository,
                          StudentRepository studentRepository,
                          SseHub sseHub,
                          AttendanceService attendanceService) {
        this.matchService = matchService;
        this.registrationService = registrationService;
        this.sessionHolder = sessionHolder;
        this.labSessionRepository = labSessionRepository;
        this.scanEventRepository = scanEventRepository;
        this.deviceRepository = deviceRepository;
        this.studentRepository = studentRepository;
        this.sseHub = sseHub;
        this.attendanceService = attendanceService;
    }

    @PostMapping("/scan")
    public ResponseEntity<Map<String, Object>> scan(@RequestBody byte[] imageBytes,
                                                    @RequestHeader("X-Nonce") String nonce) {
        String deviceIdStr = SecurityContextHolder.getContext().getAuthentication().getName();
        UUID deviceId = UUID.fromString(deviceIdStr);

        Device device = deviceRepository.findByIdWithRoom(deviceId).orElseThrow();
        LocalDateTime now = AppClock.now();

        device.setLastSeenAt(now);
        deviceRepository.save(device);

        UUID roomId = device.getRoom().getId();

        boolean malformed = imageBytes == null
            || imageBytes.length != FingerprintImageDecoder.EXPECTED_RAW_BYTES;

        Optional<RegistrationSession> regSession = sessionHolder.get(roomId);
        if (regSession.isPresent()) {
            RegistrationSession reg = regSession.get();
            if (malformed) {
                sseHub.sendRegistrationEvent(roomId, "retry",
                    "Bad capture — keep finger still and retry", reg.getSampleCount());
                return ResponseEntity.ok(Map.of("kind", "REGISTER_RETRY"));
            }
            try {
                FingerprintTemplate template = registrationService.extractTemplate(imageBytes);
                RegistrationSession.AddResult result = reg.addSample(template);

                String studentName = studentRepository.findById(reg.getStudentId())
                    .map(Student::getFullName).orElse("Unknown");

                switch (result) {
                    case DUPLICATE -> {
                        sseHub.sendRegistrationEvent(roomId, "duplicate",
                            "Move finger — too similar to previous sample", reg.getSampleCount());
                        return ResponseEntity.ok(Map.of("kind", "REGISTER_DUPLICATE"));
                    }
                    case ADDED -> {
                        sseHub.sendRegistrationEvent(roomId, "progress",
                            "Sample " + reg.getSampleCount() + "/" + reg.getSamplesRequired(), reg.getSampleCount());
                        return ResponseEntity.ok(Map.of("kind", "REGISTER_PROGRESS",
                            "sample", reg.getSampleCount(), "required", reg.getSamplesRequired()));
                    }
                    case COMPLETE -> {
                        registrationService.persistRegistration(reg);
                        sessionHolder.clear(roomId);
                        sseHub.sendRegistrationEvent(roomId, "complete",
                            "Registration complete for " + studentName, reg.getSamplesRequired());
                        return ResponseEntity.ok(Map.of("kind", "REGISTER_COMPLETE", "name", studentName));
                    }
                }
            } catch (Exception e) {
                System.err.println("[ScanController] Registration error: " + e.getMessage());
                sseHub.sendRegistrationEvent(roomId, "retry", "Processing failed — retry", reg.getSampleCount());
                return ResponseEntity.ok(Map.of("kind", "REGISTER_RETRY"));
            }
        }

        if (malformed) {
            return ResponseEntity.ok(Map.of("kind", "UNMATCHED", "malformed", true));
        }
        if (scanEventRepository.findByDeviceIdAndScanNonce(deviceId, nonce).isPresent()) {
            return ResponseEntity.ok(Map.of("kind", "UNMATCHED", "duplicate", true));
        }

        Optional<LabSession> sessionOpt = labSessionRepository
            .findActiveSessionsInRoom(roomId, now).stream().findFirst();

        MatchService.MatchResult matchResult;
        try {
            long matchStartNanos = System.nanoTime();
            matchResult = matchService.match(imageBytes);
            long matchMillis = (System.nanoTime() - matchStartNanos) / 1_000_000;
            System.out.printf("[lat] server match: %d ms (score=%.1f)%n",
                    matchMillis, matchResult.score());
        } catch (Exception e) {
            System.err.println("[ScanController] Match error: " + e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", "match_failed"));
        }

        ScanEventKind kind;
        Student student = null;
        boolean shouldCount = false;

        if (matchResult.studentId().isEmpty()) {
            kind = ScanEventKind.UNMATCHED;
        } else {
            student = studentRepository.findByIdWithPrimaryGroup(matchResult.studentId().get()).orElse(null);
            if (student == null) {
                kind = ScanEventKind.UNMATCHED;
            } else if (sessionOpt.isPresent()) {
                boolean inCohort = AttendanceService.inCohort(
                    student, sessionOpt.get().getScheduledLab());
                kind = inCohort ? ScanEventKind.REGULAR : ScanEventKind.GUEST;
                shouldCount = inCohort;
            } else {
                kind = ScanEventKind.GUEST;
            }
        }

        ScanEvent event = new ScanEvent(
            device, nonce, now,
            sessionOpt.orElse(null),
            student, kind,
            matchResult.score(), shouldCount
        );
        scanEventRepository.save(event);

        if (event.getLabSession() != null
                && (kind == ScanEventKind.REGULAR || kind == ScanEventKind.GUEST)) {
            AttendanceRowDTO row = attendanceService.buildRowFor(event, kind == ScanEventKind.GUEST);
            sseHub.sendAttendanceEvent(event.getLabSession().getId(), "scan", row);
        }

        Map<String, Object> body = new HashMap<>();
        body.put("kind", kind.name());
        if (student != null) body.put("name", student.getFullName());
        body.put("score", matchResult.score());
        return ResponseEntity.ok(body);
    }
}
