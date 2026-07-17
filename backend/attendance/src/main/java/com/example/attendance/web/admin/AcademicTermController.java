package com.example.attendance.web.admin;

import com.example.attendance.domain.academic.AcademicTerm;
import com.example.attendance.domain.academic.AcademicTermRepository;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.UUID;

@Controller
@RequestMapping("/terms")
public class AcademicTermController {
    private final AcademicTermRepository repo;

    public AcademicTermController(AcademicTermRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("terms", repo.findAllByOrderByStartDateDesc());
        model.addAttribute("form", new TermForm());
        return "terms/list";
    }

    @PostMapping
    public String create(@ModelAttribute TermForm form, RedirectAttributes ra) {
        repo.save(new AcademicTerm(form.code(), form.name(), form.startDate(), form.endDate()));
        ra.addFlashAttribute("success", "Term added.");
        return "redirect:/terms";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable UUID id, Model model) {
        AcademicTerm t = repo.findById(id).orElseThrow();
        model.addAttribute("term", t);
        model.addAttribute("form", new TermForm(t.getCode(), t.getName(), t.getStartDate(), t.getEndDate()));
        return "terms/edit";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable UUID id, @ModelAttribute TermForm form, RedirectAttributes ra) {
        AcademicTerm t = repo.findById(id).orElseThrow();
        t.setCode(form.code());
        t.setName(form.name());
        t.setStartDate(form.startDate());
        t.setEndDate(form.endDate());
        repo.save(t);
        ra.addFlashAttribute("success", "Term updated.");
        return "redirect:/terms";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable UUID id, RedirectAttributes ra) {
        repo.deleteById(id);
        ra.addFlashAttribute("success", "Term deleted.");
        return "redirect:/terms";
    }

    @PostMapping("/{id}/set-current")
    @org.springframework.transaction.annotation.Transactional
    public String setCurrent(@PathVariable UUID id, RedirectAttributes ra) {
        AcademicTerm chosen = repo.findById(id).orElseThrow();
        repo.findByCurrentTrue().ifPresent(prev -> {
            if (!prev.getId().equals(id)) {
                prev.setCurrent(false);
                repo.save(prev);
                repo.flush();
            }
        });
        chosen.setCurrent(true);
        repo.save(chosen);
        ra.addFlashAttribute("success", "Current term set: " + chosen.getName() + ".");
        return "redirect:/terms";
    }

    public record TermForm(
            String code,
            String name,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        public TermForm() { this("", "", null, null); }
    }
}
