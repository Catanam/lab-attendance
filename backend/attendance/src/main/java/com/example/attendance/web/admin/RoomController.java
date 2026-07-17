package com.example.attendance.web.admin;

import com.example.attendance.domain.academic.Room;
import com.example.attendance.domain.academic.RoomRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

@Controller
@RequestMapping("/rooms")
public class RoomController {
    private final RoomRepository repo;

    public RoomController(RoomRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("rooms", repo.findAll());
        model.addAttribute("form", new RoomForm());
        return "rooms/list";
    }

    @PostMapping
    public String create(@ModelAttribute RoomForm form, RedirectAttributes ra) {
        repo.save(new Room(form.name(), form.building()));
        ra.addFlashAttribute("success", "Room added.");
        return "redirect:/rooms";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable UUID id, Model model) {
        Room r = repo.findById(id).orElseThrow();
        model.addAttribute("room", r);
        model.addAttribute("form", new RoomForm(r.getName(), r.getBuilding()));
        return "rooms/edit";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable UUID id, @ModelAttribute RoomForm form, RedirectAttributes ra) {
        Room r = repo.findById(id).orElseThrow();
        r.setName(form.name());
        r.setBuilding(form.building());
        repo.save(r);
        ra.addFlashAttribute("success", "Room updated.");
        return "redirect:/rooms";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable UUID id, RedirectAttributes ra) {
        repo.deleteById(id);
        ra.addFlashAttribute("success", "Room deleted.");
        return "redirect:/rooms";
    }

    public record RoomForm(String name, String building) {
        public RoomForm() { this("", ""); }
    }
}
