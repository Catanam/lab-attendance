package com.example.attendance.domain.student;

import com.example.attendance.domain.academic.Room;
import com.example.attendance.domain.academic.RoomRepository;
import com.example.attendance.domain.attendance.ScanEvent;
import com.example.attendance.domain.attendance.ScanEventKind;
import com.example.attendance.domain.attendance.ScanEventRepository;
import com.example.attendance.domain.biometric.FingerprintRegistration;
import com.example.attendance.domain.biometric.FingerprintRegistrationRepository;
import com.example.attendance.domain.biometric.FingerprintTemplateBlob;
import com.example.attendance.domain.biometric.FingerprintTemplateBlobRepository;
import com.example.attendance.domain.device.Device;
import com.example.attendance.domain.device.DeviceRepository;
import com.example.attendance.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StudentErasureTest extends AbstractIntegrationTest {
    @Autowired StudentRepository studentRepo;
    @Autowired RoomRepository roomRepo;
    @Autowired DeviceRepository deviceRepo;
    @Autowired FingerprintRegistrationRepository registrationRepo;
    @Autowired FingerprintTemplateBlobRepository blobRepo;
    @Autowired ScanEventRepository scanEventRepo;
    @Autowired StudentService studentService;

    @Test
    void deleteCompletelyErasesStudentAndAllDependents() {
        String tag = UUID.randomUUID().toString().substring(0, 8);
        Student student = studentRepo.save(new Student("M-" + tag, "Erasable Student", "e@example.com"));
        Room room = roomRepo.save(new Room("ER-" + tag, "B"));
        Device device = deviceRepo.save(new Device(room, "secret-" + tag));

        FingerprintRegistration reg = registrationRepo.save(new FingerprintRegistration(student));
        FingerprintTemplateBlob blob = blobRepo.save(new FingerprintTemplateBlob(
                reg, 0, new byte[]{1, 2, 3}, new byte[]{4, 5, 6}, new byte[]{7, 8, 9}));
        ScanEvent scan = scanEventRepo.save(new ScanEvent(
                device, "nonce-" + tag, LocalDateTime.now(), null, student,
                ScanEventKind.REGULAR, 22.5, true));

        assertTrue(studentRepo.existsById(student.getId()));
        assertTrue(registrationRepo.existsById(reg.getId()));
        assertTrue(blobRepo.existsById(blob.getId()));
        assertTrue(scanEventRepo.existsById(scan.getId()));

        boolean removed = studentService.deleteCompletely(student.getId());

        assertTrue(removed);
        assertFalse(studentRepo.existsById(student.getId()), "student row erased");
        assertFalse(registrationRepo.existsById(reg.getId()), "fingerprint registration erased");
        assertFalse(blobRepo.existsById(blob.getId()), "encrypted template blob erased");
        assertFalse(scanEventRepo.existsById(scan.getId()), "scan events erased (hard delete)");
    }
}
