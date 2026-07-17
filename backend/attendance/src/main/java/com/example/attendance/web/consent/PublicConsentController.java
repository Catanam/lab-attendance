package com.example.attendance.web.consent;

import com.example.attendance.domain.gdpr.GdprConsent;
import com.example.attendance.domain.gdpr.GdprConsentService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
public class PublicConsentController {
    private final GdprConsentService consentService;

    public PublicConsentController(GdprConsentService consentService) {
        this.consentService = consentService;
    }

    @GetMapping("/consent")
    public String show(@RequestParam(required = false) String token, Model model) {
        model.addAttribute("consentText", consentService.getConsentText());
        Optional<GdprConsent> consent = (token == null || token.isBlank())
            ? Optional.empty() : consentService.findByToken(token);
        if (consent.isEmpty()) {
            model.addAttribute("notFound", true);
            return "consent/confirm";
        }
        GdprConsent c = consent.get();
        model.addAttribute("token", token);
        model.addAttribute("studentName", c.getStudent().getFullName());
        model.addAttribute("status", c.getStatus().name());
        model.addAttribute("pending", c.isPending());
        model.addAttribute("accepted", c.isAccepted());
        return "consent/confirm";
    }

    @PostMapping("/consent/accept")
    public String accept(@RequestParam String token, RedirectAttributes ra) {
        boolean ok = consentService.accept(token).isPresent();
        ra.addFlashAttribute(ok ? "message" : "errorMessage",
            ok ? "Vă mulțumim! Consimțământul a fost înregistrat."
               : "Link invalid sau expirat.");
        return "redirect:/consent?token=" + token;
    }

    @PostMapping("/consent/decline")
    public String decline(@RequestParam String token, RedirectAttributes ra) {
        boolean ok = consentService.decline(token).isPresent();
        ra.addFlashAttribute(ok ? "message" : "errorMessage",
            ok ? "Am înregistrat refuzul dumneavoastră. Înregistrarea amprentei nu va fi posibilă."
               : "Link invalid sau expirat.");
        return "redirect:/consent?token=" + token;
    }
}
