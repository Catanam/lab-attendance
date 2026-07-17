package com.example.attendance.web.admin;

import com.example.attendance.domain.academic.CourseTeacherRepository;
import com.example.attendance.domain.staff.Professor;
import com.example.attendance.domain.staff.ProfessorRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/professors")
public class ProfessorController {
    private final ProfessorRepository repo;
    private final CourseTeacherRepository courseTeacherRepo;

    public ProfessorController(ProfessorRepository repo, CourseTeacherRepository courseTeacherRepo) {
        this.repo = repo;
        this.courseTeacherRepo = courseTeacherRepo;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("professors", repo.findAll());
        model.addAttribute("form", new ProfessorForm());
        return "professors/list";
    }

    @PostMapping
    public String create(@ModelAttribute ProfessorForm form, RedirectAttributes ra) {
        repo.save(new Professor(form.title(), form.fullName(), form.email(), form.officeRoom(), form.officeHours()));
        ra.addFlashAttribute("success", "Professor added.");
        return "redirect:/professors";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable UUID id, Model model) {
        Professor p = repo.findById(id).orElseThrow();
        model.addAttribute("professor", p);
        model.addAttribute("form", new ProfessorForm(p.getTitle(), p.getFullName(), p.getEmail(), p.getOfficeRoom(), p.getOfficeHours()));
        return "professors/edit";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable UUID id, @ModelAttribute ProfessorForm form, RedirectAttributes ra) {
        Professor p = repo.findById(id).orElseThrow();
        p.setTitle(form.title());
        p.setFullName(form.fullName());
        p.setEmail(form.email());
        p.setOfficeRoom(form.officeRoom());
        p.setOfficeHours(form.officeHours());
        repo.save(p);
        ra.addFlashAttribute("success", "Professor updated.");
        return "redirect:/professors";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable UUID id, RedirectAttributes ra) {
        List<UUID> orphaned = courseTeacherRepo.findSoleTeacherCourseIds(id);
        if (!orphaned.isEmpty()) {
            ra.addFlashAttribute("error",
                "Cannot delete: this professor is the only teacher of " + orphaned.size()
                + " course(s). Assign another professor to those courses first.");
            return "redirect:/professors";
        }
        repo.deleteById(id);
        ra.addFlashAttribute("success", "Professor deleted.");
        return "redirect:/professors";
    }

    public record ProfessorForm(String title, String fullName, String email, String officeRoom, String officeHours) {
        public ProfessorForm() { this("", "", "", "", ""); }
    }
}
