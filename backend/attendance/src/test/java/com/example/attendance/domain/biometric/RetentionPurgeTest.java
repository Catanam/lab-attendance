package com.example.attendance.domain.biometric;

import com.example.attendance.domain.academic.AcademicTerm;
import com.example.attendance.domain.academic.AcademicTermRepository;
import com.example.attendance.domain.academic.Course;
import com.example.attendance.domain.academic.CourseRepository;
import com.example.attendance.domain.academic.Room;
import com.example.attendance.domain.academic.RoomRepository;
import com.example.attendance.domain.academic.StudentGroup;
import com.example.attendance.domain.academic.StudentGroupRepository;
import com.example.attendance.domain.attendance.ScanEvent;
import com.example.attendance.domain.attendance.ScanEventKind;
import com.example.attendance.domain.attendance.ScanEventRepository;
import com.example.attendance.domain.device.Device;
import com.example.attendance.domain.device.DeviceRepository;
import com.example.attendance.domain.schedule.ScheduledLab;
import com.example.attendance.domain.schedule.ScheduledLabRepository;
import com.example.attendance.domain.student.Student;
import com.example.attendance.domain.student.StudentRepository;
import com.example.attendance.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RetentionPurgeTest extends AbstractIntegrationTest {
    @Autowired StudentRepository studentRepo;
    @Autowired StudentGroupRepository groupRepo;
    @Autowired AcademicTermRepository termRepo;
    @Autowired CourseRepository courseRepo;
    @Autowired RoomRepository roomRepo;
    @Autowired ScheduledLabRepository labRepo;
    @Autowired DeviceRepository deviceRepo;
    @Autowired FingerprintRegistrationRepository registrationRepo;
    @Autowired FingerprintTemplateBlobRepository blobRepo;
    @Autowired ScanEventRepository scanEventRepo;
    @Autowired RetentionPurgeService retentionPurgeService;

    @Test
    void purgesExpiredStudentKeepsCurrentOneAndPreservesScanAudit() {
        String tag = UUID.randomUUID().toString().substring(0, 8);
        Course course = courseRepo.save(new Course("RC-" + tag, "Retention course"));
        Room room = roomRepo.save(new Room("RR-" + tag, "B"));
        Device device = deviceRepo.save(new Device(room, "rsecret-" + tag));

        StudentGroup oldGroup = groupRepo.save(new StudentGroup("OLD-" + tag, null));
        AcademicTerm oldTerm = termRepo.save(new AcademicTerm("OT-" + tag, "Old term",
                LocalDate.now().minusMonths(12), LocalDate.now().minusMonths(10)));
        labRepo.save(new ScheduledLab(oldTerm, course, oldGroup, room,
                "MONDAY", LocalTime.of(10, 0), LocalTime.of(12, 0), 1));
        Student expired = studentInGroup(tag + "-old", oldGroup);
        FingerprintRegistration expiredReg = registrationRepo.save(new FingerprintRegistration(expired));
        FingerprintTemplateBlob expiredBlob = blobRepo.save(blobFor(expiredReg));
        ScanEvent expiredScan = scanEventRepo.save(new ScanEvent(
                device, "old-nonce-" + tag, LocalDateTime.now().minusMonths(11), null, expired,
                ScanEventKind.REGULAR, 21.0, true));

        StudentGroup curGroup = groupRepo.save(new StudentGroup("CUR-" + tag, null));
        AcademicTerm curTerm = termRepo.save(new AcademicTerm("CT-" + tag, "Current term",
                LocalDate.now().minusMonths(1), LocalDate.now().plusMonths(2)));
        labRepo.save(new ScheduledLab(curTerm, course, curGroup, room,
                "MONDAY", LocalTime.of(10, 0), LocalTime.of(12, 0), 1));
        Student current = studentInGroup(tag + "-cur", curGroup);
        FingerprintRegistration currentReg = registrationRepo.save(new FingerprintRegistration(current));
        FingerprintTemplateBlob currentBlob = blobRepo.save(blobFor(currentReg));

        int purgedStudents = retentionPurgeService.purgeExpired();

        assertTrue(purgedStudents >= 1, "at least the expired student was purged");

        assertFalse(registrationRepo.existsById(expiredReg.getId()), "expired registration purged");
        assertFalse(blobRepo.existsById(expiredBlob.getId()), "expired template purged");

        assertTrue(scanEventRepo.existsById(expiredScan.getId()), "scan audit preserved");
        assertTrue(studentRepo.existsById(expired.getId()), "student row preserved");

        assertTrue(registrationRepo.existsById(currentReg.getId()), "current registration kept");
        assertTrue(blobRepo.existsById(currentBlob.getId()), "current template kept");
    }

    private Student studentInGroup(String tag, StudentGroup group) {
        Student s = new Student("M-" + tag, "Student " + tag, tag + "@example.com");
        s.setPrimaryGroup(group);
        return studentRepo.save(s);
    }

    private FingerprintTemplateBlob blobFor(FingerprintRegistration reg) {
        return new FingerprintTemplateBlob(reg, 0, new byte[]{1}, new byte[]{2}, new byte[]{3});
    }
}
