package com.example.attendance.web.admin;

import com.example.attendance.domain.auth.AppUser;
import com.example.attendance.domain.auth.Role;
import com.example.attendance.domain.auth.UserRepository;
import com.example.attendance.domain.biometric.BiometricConsentRepository;
import com.example.attendance.domain.staff.ProfessorRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.UUID;

@Controller
@RequestMapping("/accounts")
public class AccountAdminController {
    private final UserRepository userRepository;
    private final ProfessorRepository professorRepository;
    private final BiometricConsentRepository consentRepository;
    private final PasswordEncoder passwordEncoder;

    public AccountAdminController(UserRepository userRepository,
                                 ProfessorRepository professorRepository,
                                 BiometricConsentRepository consentRepository,
                                 PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.professorRepository = professorRepository;
        this.consentRepository = consentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("accounts", userRepository.findAllWithProfessor());
        model.addAttribute("professors", professorRepository.findAll());
        model.addAttribute("roles", Role.values());
        model.addAttribute("form", new AccountForm());
        return "accounts/list";
    }

    @PostMapping
    public String create(@ModelAttribute AccountForm form, RedirectAttributes ra) {
        String username = form.username() == null ? "" : form.username().trim();
        if (username.isEmpty() || form.password() == null || form.password().isEmpty()) {
            ra.addFlashAttribute("error", "Username and password are required.");
            return "redirect:/accounts";
        }
        if (userRepository.existsByUsername(username)) {
            ra.addFlashAttribute("error", "Username already exists: " + username);
            return "redirect:/accounts";
        }
        Role role;
        try {
            role = Role.valueOf(form.role());
        } catch (IllegalArgumentException | NullPointerException e) {
            ra.addFlashAttribute("error", "Invalid role.");
            return "redirect:/accounts";
        }
        AppUser user = new AppUser(username, passwordEncoder.encode(form.password()), role);
        if (form.professorId() != null) {
            professorRepository.findById(form.professorId()).ifPresent(user::setProfessor);
        }
        userRepository.save(user);
        ra.addFlashAttribute("success", "Account created: " + username + " (" + role + ").");
        return "redirect:/accounts";
    }

    @PostMapping("/{id}/reset-password")
    public String resetPassword(@PathVariable UUID id,
                                @RequestParam String newPassword,
                                RedirectAttributes ra) {
        AppUser user = userRepository.findById(id).orElse(null);
        if (user == null) {
            ra.addFlashAttribute("error", "Account not found.");
            return "redirect:/accounts";
        }
        if (newPassword == null || newPassword.isEmpty()) {
            ra.addFlashAttribute("error", "New password cannot be empty.");
            return "redirect:/accounts";
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        ra.addFlashAttribute("success", "Password reset for " + user.getUsername() + ".");
        return "redirect:/accounts";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable UUID id, Principal principal, RedirectAttributes ra) {
        AppUser user = userRepository.findById(id).orElse(null);
        if (user == null) {
            ra.addFlashAttribute("error", "Account not found.");
            return "redirect:/accounts";
        }
        if (user.getUsername().equals(principal.getName())) {
            ra.addFlashAttribute("error", "You cannot delete the account you are logged in with.");
            return "redirect:/accounts";
        }
        if (consentRepository.existsByOperatorId(id)) {
            ra.addFlashAttribute("error", String.format(
                "Cannot delete %s — it is recorded as the operator on biometric consent(s). "
                + "Accounts referenced by audit records must be kept.", user.getUsername()));
            return "redirect:/accounts";
        }
        String username = user.getUsername();
        userRepository.delete(user);
        ra.addFlashAttribute("success", "Account " + username + " deleted.");
        return "redirect:/accounts";
    }

    public record AccountForm(String username, String password, String role, UUID professorId) {
        public AccountForm() { this("", "", Role.PROFESSOR.name(), null); }
    }
}
