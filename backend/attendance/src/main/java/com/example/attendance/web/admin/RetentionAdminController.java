package com.example.attendance.web.admin;

import com.example.attendance.domain.biometric.RetentionPurgeService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/retention")
public class RetentionAdminController {
    private final RetentionPurgeService retentionPurgeService;

    public RetentionAdminController(RetentionPurgeService retentionPurgeService) {
        this.retentionPurgeService = retentionPurgeService;
    }

    @PostMapping("/run")
    public String run(RedirectAttributes ra) {
        int purged = retentionPurgeService.purgeExpired();
        ra.addFlashAttribute("success", "Retention purge complete — biometric data removed for "
            + purged + " student(s) past the "
            + retentionPurgeService.getRetentionMonths() + "-month window.");
        return "redirect:/";
    }
}
