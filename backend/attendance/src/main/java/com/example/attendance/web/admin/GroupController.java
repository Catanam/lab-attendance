package com.example.attendance.web.admin;

import com.example.attendance.domain.academic.StudentGroup;
import com.example.attendance.domain.academic.StudentGroupRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

@Controller
@RequestMapping("/groups")
public class GroupController {
    private final StudentGroupRepository repo;

    public GroupController(StudentGroupRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("groups", repo.findAll());
        model.addAttribute("form", new GroupForm());
        return "groups/list";
    }

    @PostMapping
    public String create(@ModelAttribute GroupForm form, RedirectAttributes ra) {
        repo.save(new StudentGroup(form.name(), form.year()));
        ra.addFlashAttribute("success", "Group added.");
        return "redirect:/groups";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable UUID id, Model model) {
        StudentGroup g = repo.findById(id).orElseThrow();
        model.addAttribute("group", g);
        model.addAttribute("form", new GroupForm(g.getName(), g.getYear()));
        return "groups/edit";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable UUID id, @ModelAttribute GroupForm form, RedirectAttributes ra) {
        StudentGroup g = repo.findById(id).orElseThrow();
        g.setName(form.name());
        g.setYear(form.year());
        repo.save(g);
        ra.addFlashAttribute("success", "Group updated.");
        return "redirect:/groups";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable UUID id, RedirectAttributes ra) {
        repo.deleteById(id);
        ra.addFlashAttribute("success", "Group deleted.");
        return "redirect:/groups";
    }

    public record GroupForm(String name, String year) {
        public GroupForm() { this("", ""); }
    }
}
