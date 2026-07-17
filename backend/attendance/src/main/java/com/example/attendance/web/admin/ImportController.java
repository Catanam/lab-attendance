package com.example.attendance.web.admin;

import com.example.attendance.domain.academic.AcademicTerm;
import com.example.attendance.domain.academic.AcademicTermRepository;
import com.example.attendance.web.admin.imports.*;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/import")
public class ImportController {
    private final AcademicTermRepository termRepo;
    private final StudentsImporter studentsImporter;
    private final ProfessorsImporter professorsImporter;
    private final ScheduleImporter scheduleImporter;

    public ImportController(AcademicTermRepository termRepo,
                            StudentsImporter studentsImporter,
                            ProfessorsImporter professorsImporter,
                            ScheduleImporter scheduleImporter) {
        this.termRepo = termRepo;
        this.studentsImporter = studentsImporter;
        this.professorsImporter = professorsImporter;
        this.scheduleImporter = scheduleImporter;
    }

    @GetMapping
    public String index(Model model) {
        AcademicTerm current = termRepo.findByCurrentTrue().orElse(null);
        model.addAttribute("currentTerm", current);
        return "import/index";
    }

    @PostMapping("/students")
    public String importStudents(@RequestParam("file") MultipartFile file, RedirectAttributes ra) {
        return runImport(file, "student", studentsImporter::run, ra);
    }

    @PostMapping("/professors")
    public String importProfessors(@RequestParam("file") MultipartFile file, RedirectAttributes ra) {
        return runImport(file, "course-teacher mapping", professorsImporter::run, ra);
    }

    @PostMapping("/schedule")
    public String importSchedule(@RequestParam("file") MultipartFile file, RedirectAttributes ra) {
        return runImport(file, "scheduled lab", scheduleImporter::run, ra);
    }

    private String runImport(MultipartFile file, String unit,
                             java.util.function.Function<List<SheetRow>, ImportResult> importer,
                             RedirectAttributes ra) {
        if (file == null || file.isEmpty()) {
            ra.addFlashAttribute("error", "No file selected.");
            return "redirect:/import";
        }
        List<SheetRow> rows;
        try {
            rows = SheetReader.read(file);
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Could not read file: " + e.getMessage());
            return "redirect:/import";
        }
        ImportResult r = importer.apply(rows);
        ra.addFlashAttribute(r.hasErrors() ? "warning" : "success", r.summary(unit));
        return "redirect:/import";
    }

    @GetMapping("/templates/students.csv")
    public ResponseEntity<ByteArrayResource> studentsTemplate() {
        return csv("students.csv",
            "matricol_number,full_name,email,year_of_study,group_name\n" +
            "B12345,Doe John,john.doe@example.com,1,INFO-1A\n");
    }

    @GetMapping("/templates/professors.csv")
    public ResponseEntity<ByteArrayResource> professorsTemplate() {
        return csv("professors.csv",
            "prof_full_name,prof_title,prof_email,course_code,course_name,role\n" +
            "Popescu Maria,Conf. dr.,maria.popescu@example.com,ALG101,Algorithms,PRIMARY\n" +
            "Ionescu Andrei,Asist. dr.,andrei.ionescu@example.com,ALG101,Algorithms,ASSISTANT\n");
    }

    @GetMapping("/templates/schedule.csv")
    public ResponseEntity<ByteArrayResource> scheduleTemplate() {
        return csv("schedule.csv",
            "group_name,year_of_study,course_code,weekday,start_time,end_time,room_name\n" +
            "1,1,ALG101,MONDAY,10:00,12:00,A101\n" +
            "1,3,DB301,TUESDAY,14:00,16:00,A101\n");
    }

    private static ResponseEntity<ByteArrayResource> csv(String filename, String body) {
        byte[] bytes = body.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
            .contentType(MediaType.parseMediaType("text/csv; charset=utf-8"))
            .contentLength(bytes.length)
            .body(new ByteArrayResource(bytes));
    }
}
