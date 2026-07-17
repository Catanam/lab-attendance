package com.example.attendance.web.admin;

import com.example.attendance.domain.academic.Room;
import com.example.attendance.domain.academic.RoomRepository;
import com.example.attendance.domain.biometric.RegistrationSessionHolder;
import com.example.attendance.domain.device.Device;
import com.example.attendance.domain.device.DeviceRepository;
import com.example.attendance.domain.gdpr.GdprConsent;
import com.example.attendance.domain.gdpr.GdprConsentService;
import com.example.attendance.domain.student.Student;
import com.example.attendance.domain.student.StudentRepository;
import com.example.attendance.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WithMockUser(username = "staff", roles = "SUPER_ADMIN")
class ConsentGateTest extends AbstractIntegrationTest {
    @Autowired StudentRepository studentRepo;
    @Autowired RoomRepository roomRepo;
    @Autowired DeviceRepository deviceRepo;
    @Autowired GdprConsentService gdprConsentService;
    @Autowired RegistrationSessionHolder sessionHolder;

    @Test
    void startIsBlockedWhenStudentHasNotConsented() throws Exception {
        String tag = UUID.randomUUID().toString().substring(0, 8);
        Student student = studentRepo.save(new Student("M-" + tag, "Unconsented Student", tag + "@example.com"));
        Room room = roomRepo.save(new Room("CG-" + tag, "B"));
        Device device = deviceRepo.save(new Device(room, "secret-" + tag));

        mockMvc.perform(post("/register/start")
                        .param("studentId", student.getId().toString())
                        .param("deviceId", device.getId().toString())
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/register"));

        assertFalse(sessionHolder.isActive(room.getId()),
                "no registration session may start for a student who has not consented");
    }

    @Test
    void startIsAllowedOnceStudentHasAccepted() throws Exception {
        String tag = UUID.randomUUID().toString().substring(0, 8);
        Student student = studentRepo.save(new Student("M-" + tag, "Consented Student", tag + "@example.com"));
        Room room = roomRepo.save(new Room("CG-" + tag, "B"));
        Device device = deviceRepo.save(new Device(room, "secret-" + tag));

        GdprConsent consent = gdprConsentService.issue(student);
        gdprConsentService.accept(consent.getToken());

        mockMvc.perform(post("/register/start")
                        .param("studentId", student.getId().toString())
                        .param("deviceId", device.getId().toString())
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/register/progress*"));

        assertTrue(sessionHolder.isActive(room.getId()),
                "an accepted student opens a registration session in the device's room");
    }
}
