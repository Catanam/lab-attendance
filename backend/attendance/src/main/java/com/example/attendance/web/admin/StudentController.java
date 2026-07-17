package com.example.attendance.web.admin;

import com.example.attendance.domain.academic.StudentGroupRepository;
import com.example.attendance.domain.biometric.BiometricConsentService;
import com.example.attendance.domain.biometric.FingerprintRegistrationRepository;
import com.example.attendance.domain.biometric.RegistrationService;
import com.example.attendance.domain.student.Student;
import com.example.attendance.domain.student.StudentRepository;
import com.example.attendance.domain.student.StudentService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/students")
public class StudentController {
    private final StudentRepository repo;
    private final FingerprintRegistrationRepository registrationRepo;
    private final StudentGroupRepository groupRepo;
    private final RegistrationService registrationService;
    private final BiometricConsentService consentService;
    private final StudentService studentService;

    public StudentController(StudentRepository repo,
                             FingerprintRegistrationRepository registrationRepo,
                             StudentGroupRepository groupRepo,
                             RegistrationService registrationService,
                             BiometricConsentService consentService,
                             StudentService studentService) {
        this.repo = repo;
        this.registrationRepo = registrationRepo;
        this.groupRepo = groupRepo;
        this.registrationService = registrationService;
        this.consentService = consentService;
        this.studentService = studentService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("students", repo.findAllWithPrimaryGroup());
        model.addAttribute("groups", groupRepo.findAll());
        model.addAttribute("form", new StudentForm());
        return "students/list";
    }

    @PostMapping
    public String create(@ModelAttribute StudentForm form, RedirectAttributes ra) {
        if (repo.existsByMatricolNumber(form.matricolNumber())) {
            ra.addFlashAttribute("error", "Matricol number already exists: " + form.matricolNumber());
            return "redirect:/students";
        }
        Student s = new Student(form.matricolNumber(), form.fullName(), form.email());
        s.setYearOfStudy(form.yearOfStudy());
        if (form.primaryGroupId() != null) {
            s.setPrimaryGroup(groupRepo.findById(form.primaryGroupId()).orElse(null));
        }
        repo.save(s);
        ra.addFlashAttribute("success", "Student added.");
        return "redirect:/students";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable UUID id, Model model) {
        Student s = repo.findByIdWithPrimaryGroup(id).orElseThrow();
        UUID currentGroupId = s.getPrimaryGroup() != null ? s.getPrimaryGroup().getId() : null;
        model.addAttribute("student", s);
        model.addAttribute("form", new StudentForm(
            s.getMatricolNumber(), s.getFullName(), s.getEmail(),
            s.getYearOfStudy(), currentGroupId));
        model.addAttribute("groups", groupRepo.findAll());
        model.addAttribute("fingerprintCount", registrationRepo.countByStudentId(id));
        return "students/edit";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable UUID id, @ModelAttribute StudentForm form, RedirectAttributes ra) {
        Student s = repo.findById(id).orElseThrow();
        s.setMatricolNumber(form.matricolNumber());
        s.setFullName(form.fullName());
        s.setEmail(form.email());
        s.setYearOfStudy(form.yearOfStudy());
        if (form.primaryGroupId() == null) {
            s.setPrimaryGroup(null);
        } else {
            s.setPrimaryGroup(groupRepo.findById(form.primaryGroupId()).orElse(null));
        }
        repo.save(s);
        ra.addFlashAttribute("success", "Student updated.");
        return "redirect:/students/" + id + "/edit";
    }

    @PostMapping("/{id}/toggle-active")
    public String toggleActive(@PathVariable UUID id) {
        Student s = repo.findById(id).orElseThrow();
        s.setActive(!s.isActive());
        repo.save(s);
        return "redirect:/students";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable UUID id, RedirectAttributes ra) {
        Student s = repo.findById(id).orElse(null);
        if (s == null) {
            ra.addFlashAttribute("error", "Student not found.");
            return "redirect:/students";
        }
        String matricol = s.getMatricolNumber();
        studentService.deleteCompletely(id);
        ra.addFlashAttribute("success", "Student " + matricol + " deleted (incl. fingerprint & scan history).");
        return "redirect:/students";
    }

    @PostMapping("/bulk-delete")
    public String bulkDelete(@RequestParam(name = "ids", required = false) List<UUID> ids,
                             RedirectAttributes ra) {
        if (ids == null || ids.isEmpty()) {
            ra.addFlashAttribute("warning", "No students selected.");
            return "redirect:/students";
        }
        int removed = studentService.deleteCompletely(ids);
        ra.addFlashAttribute("success",
            "Deleted " + removed + " student(s), including any fingerprint & scan history.");
        return "redirect:/students";
    }

    @PostMapping("/bulk-deactivate")
    public String bulkDeactivate(@RequestParam(name = "ids", required = false) List<UUID> ids,
                                 RedirectAttributes ra) {
        if (ids == null || ids.isEmpty()) {
            ra.addFlashAttribute("warning", "No students selected.");
            return "redirect:/students";
        }
        int changed = studentService.deactivate(ids);
        ra.addFlashAttribute("success", "Deactivated " + changed + " student(s).");
        return "redirect:/students";
    }

    @PostMapping("/bulk-reset-fingerprint")
    public String bulkResetFingerprint(@RequestParam(name = "ids", required = false) List<UUID> ids,
                                       RedirectAttributes ra) {
        if (ids == null || ids.isEmpty()) {
            ra.addFlashAttribute("warning", "No students selected.");
            return "redirect:/students";
        }
        int students = 0;
        int removed = 0;
        for (UUID id : ids) {
            if (!repo.existsById(id)) {
                continue;
            }
            removed += registrationService.purgeStudentFingerprints(id);
            consentService.withdrawConsent(id);
            students++;
        }
        ra.addFlashAttribute("success",
            "Reset fingerprints for " + students + " student(s) — removed " + removed +
            " registration(s). They can re-register after fresh consent.");
        return "redirect:/students";
    }

    @PostMapping("/{id}/reset-fingerprint")
    public String resetFingerprint(@PathVariable UUID id, RedirectAttributes ra) {
        Student s = repo.findById(id).orElse(null);
        if (s == null) {
            ra.addFlashAttribute("error", "Student not found.");
            return "redirect:/students";
        }
        int removed = registrationService.purgeStudentFingerprints(id);
        consentService.withdrawConsent(id);
        if (removed == 0) {
            ra.addFlashAttribute("warning", s.getMatricolNumber() + " had no fingerprint data to remove.");
        } else {
            ra.addFlashAttribute("success",
                "Removed " + removed + " fingerprint registration(s) for " + s.getMatricolNumber() +
                ". Student can re-register now.");
        }
        return "redirect:/students/" + id + "/edit";
    }

    public record StudentForm(String matricolNumber,
                              String fullName,
                              String email,
                              Integer yearOfStudy,
                              UUID primaryGroupId) {
        public StudentForm() { this("", "", "", null, null); }
        public StudentForm(String matricolNumber, String fullName, String email) {
            this(matricolNumber, fullName, email, null, null);
        }
    }
}
