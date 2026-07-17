package com.example.attendance.web.admin;

import com.example.attendance.domain.gdpr.ConsentNotifier;
import com.example.attendance.domain.gdpr.GdprConsent;
import com.example.attendance.domain.gdpr.GdprConsentRepository;
import com.example.attendance.domain.gdpr.GdprConsentService;
import com.example.attendance.domain.student.Student;
import com.example.attendance.domain.student.StudentRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/gdpr")
public class GdprConsentAdminController {
    private final StudentRepository studentRepo;
    private final GdprConsentRepository consentRepo;
    private final GdprConsentService consentService;
    private final ConsentNotifier notifier;

    public GdprConsentAdminController(StudentRepository studentRepo,
                                      GdprConsentRepository consentRepo,
                                      GdprConsentService consentService,
                                      ConsentNotifier notifier) {
        this.studentRepo = studentRepo;
        this.consentRepo = consentRepo;
        this.consentService = consentService;
        this.notifier = notifier;
    }

    @GetMapping
    public String index(Model model) {
        Map<UUID, GdprConsent> byStudent = consentRepo.findAllWithStudent().stream()
            .collect(Collectors.toMap(c -> c.getStudent().getId(), Function.identity(), (a, b) -> a));
        List<Row> rows = new ArrayList<>();
        for (Student s : studentRepo.findAllWithPrimaryGroup()) {
            GdprConsent c = byStudent.get(s.getId());
            String link = (c == null) ? null : confirmLink(c.getToken());
            rows.add(new Row(s, c, link));
        }
        model.addAttribute("rows", rows);
        model.addAttribute("consentText", consentService.getConsentText());
        return "gdpr/list";
    }

    @PostMapping("/send")
    public String sendAll(RedirectAttributes ra) {
        int issued = 0, skippedNoEmail = 0;
        for (Student s : studentRepo.findAll()) {
            if (s.getEmail() == null || s.getEmail().isBlank()) {
                skippedNoEmail++;
                continue;
            }
            if (consentService.hasAccepted(s.getId())) {
                continue;
            }
            GdprConsent c = consentService.issue(s);
            notifier.sendConsentRequest(s, confirmLink(c.getToken()));
            issued++;
        }
        ra.addFlashAttribute("success",
            "Issued " + issued + " consent link(s)." +
            (skippedNoEmail > 0 ? " Skipped " + skippedNoEmail + " student(s) without an email." : ""));
        return "redirect:/gdpr";
    }

    @PostMapping("/send/{studentId}")
    public String sendOne(@PathVariable UUID studentId, RedirectAttributes ra) {
        Student s = studentRepo.findById(studentId).orElse(null);
        if (s == null) {
            ra.addFlashAttribute("error", "Student not found.");
            return "redirect:/gdpr";
        }
        if (s.getEmail() == null || s.getEmail().isBlank()) {
            ra.addFlashAttribute("warning",
                s.getMatricolNumber() + " has no email — add one before issuing a consent link.");
            return "redirect:/gdpr";
        }
        GdprConsent c = consentService.issue(s);
        notifier.sendConsentRequest(s, confirmLink(c.getToken()));
        ra.addFlashAttribute("success", "Consent link issued for " + s.getMatricolNumber() + ".");
        return "redirect:/gdpr";
    }

    private String confirmLink(String token) {
        return ServletUriComponentsBuilder.fromCurrentContextPath()
            .path("/consent").queryParam("token", token).build().toUriString();
    }

    public record Row(Student student, GdprConsent consent, String link) {
        public String statusLabel() {
            if (consent == null) return "NOT SENT";
            return consent.getStatus().name();
        }
    }
}
