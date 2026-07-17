package com.example.attendance.web.admin;

import com.example.attendance.domain.academic.Course;
import com.example.attendance.domain.academic.CourseRepository;
import com.example.attendance.domain.academic.CourseTeacher;
import com.example.attendance.domain.academic.CourseTeacherRepository;
import com.example.attendance.domain.academic.CourseTeacherRole;
import com.example.attendance.domain.staff.Professor;
import com.example.attendance.domain.staff.ProfessorRepository;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/courses")
public class CourseController {
    private final CourseRepository repo;
    private final ProfessorRepository professorRepo;
    private final CourseTeacherRepository courseTeacherRepo;

    public CourseController(CourseRepository repo,
                            ProfessorRepository professorRepo,
                            CourseTeacherRepository courseTeacherRepo) {
        this.repo = repo;
        this.professorRepo = professorRepo;
        this.courseTeacherRepo = courseTeacherRepo;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("courses", repo.findAll());
        model.addAttribute("professors", professorRepo.findAll());

        Map<UUID, String> teachersByCourse = courseTeacherRepo.findAllWithDetails().stream()
            .collect(Collectors.groupingBy(
                ct -> ct.getCourse().getId(), LinkedHashMap::new,
                Collectors.mapping(ct -> ct.getProfessor().getFullName(), Collectors.joining(", "))));
        model.addAttribute("teachersByCourse", teachersByCourse);
        model.addAttribute("form", new CourseForm());
        return "courses/list";
    }

    @PostMapping
    @Transactional
    public String create(@ModelAttribute CourseForm form, RedirectAttributes ra) {
        if (form.professorId() == null) {
            ra.addFlashAttribute("error", "A course must be assigned a professor.");
            return "redirect:/courses";
        }
        Professor professor = professorRepo.findById(form.professorId()).orElse(null);
        if (professor == null) {
            ra.addFlashAttribute("error", "Selected professor not found.");
            return "redirect:/courses";
        }
        Course course = repo.save(new Course(form.code(), form.name()));
        courseTeacherRepo.save(new CourseTeacher(course, professor, CourseTeacherRole.PRIMARY));
        ra.addFlashAttribute("success", "Course added and assigned to " + professor.getFullName() + ".");
        return "redirect:/courses";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable UUID id, Model model) {
        Course c = repo.findById(id).orElseThrow();
        model.addAttribute("course", c);
        model.addAttribute("form", new CourseForm(c.getCode(), c.getName(), null));
        return "courses/edit";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable UUID id, @ModelAttribute CourseForm form, RedirectAttributes ra) {
        Course c = repo.findById(id).orElseThrow();
        c.setCode(form.code());
        c.setName(form.name());
        repo.save(c);
        ra.addFlashAttribute("success", "Course updated.");
        return "redirect:/courses";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable UUID id, RedirectAttributes ra) {
        repo.deleteById(id);
        ra.addFlashAttribute("success", "Course deleted.");
        return "redirect:/courses";
    }

    public record CourseForm(String code, String name, UUID professorId) {
        public CourseForm() { this("", "", null); }
    }
}
