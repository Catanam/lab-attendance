package com.example.attendance.web.admin;

import com.example.attendance.domain.biometric.BiometricConsentService;
import com.example.attendance.domain.biometric.RegistrationSessionHolder;
import com.example.attendance.domain.device.Device;
import com.example.attendance.domain.device.DeviceRepository;
import com.example.attendance.domain.gdpr.GdprConsentService;
import com.example.attendance.domain.student.Student;
import com.example.attendance.domain.student.StudentRepository;
import com.example.attendance.web.sse.SseHub;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.UUID;

@Controller
@RequestMapping("/register")
public class RegistrationAdminController {
    private final StudentRepository studentRepository;
    private final DeviceRepository deviceRepository;
    private final RegistrationSessionHolder sessionHolder;
    private final SseHub sseHub;
    private final BiometricConsentService consentService;
    private final GdprConsentService gdprConsentService;

    public RegistrationAdminController(StudentRepository studentRepository,
                                       DeviceRepository deviceRepository,
                                       RegistrationSessionHolder sessionHolder,
                                       SseHub sseHub,
                                       BiometricConsentService consentService,
                                       GdprConsentService gdprConsentService) {
        this.studentRepository = studentRepository;
        this.deviceRepository = deviceRepository;
        this.sessionHolder = sessionHolder;
        this.sseHub = sseHub;
        this.consentService = consentService;
        this.gdprConsentService = gdprConsentService;
    }

    @GetMapping
    public String index(Model model) {
        model.addAttribute("students", studentRepository.findWithoutActiveFingerprint());
        model.addAttribute("devices", deviceRepository.findAllWithRoom());
        return "register/index";
    }

    @PostMapping("/start")
    public String start(@RequestParam UUID studentId,
                        @RequestParam UUID deviceId,
                        Principal principal,
                        RedirectAttributes ra) {
        if (!gdprConsentService.hasAccepted(studentId)) {
            Student s = studentRepository.findById(studentId).orElse(null);
            String who = (s != null) ? s.getMatricolNumber() : "studentul selectat";
            ra.addFlashAttribute("error",
                "Înregistrarea amprentei nu este permisă: " + who +
                " nu a confirmat consimțământul GDPR. Trimiteți linkul de consimțământ din pagina GDPR " +
                "și așteptați acceptarea studentului.");
            return "redirect:/register";
        }

        consentService.recordConsent(studentId, principal.getName());

        Device device = deviceRepository.findByIdWithRoom(deviceId).orElse(null);
        if (device == null) {
            ra.addFlashAttribute("error", "Device not found.");
            return "redirect:/register";
        }
        UUID roomId = device.getRoom().getId();

        sessionHolder.clear(roomId);
        sessionHolder.start(roomId, studentId);
        return "redirect:/register/progress?roomId=" + roomId + "&studentId=" + studentId;
    }

    @GetMapping("/progress")
    public String progress(@RequestParam UUID roomId,
                           @RequestParam UUID studentId,
                           Model model) {
        studentRepository.findById(studentId).ifPresent(s -> model.addAttribute("student", s));
        model.addAttribute("roomId", roomId);
        return "register/progress";
    }

    @GetMapping(value = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ResponseBody
    public SseEmitter events(@RequestParam UUID roomId) {
        return sseHub.subscribeRegistration(roomId);
    }

    @PostMapping("/cancel")
    public String cancel(@RequestParam UUID roomId, RedirectAttributes ra) {
        sessionHolder.clear(roomId);
        ra.addFlashAttribute("warning", "Registration cancelled.");
        return "redirect:/register";
    }
}
