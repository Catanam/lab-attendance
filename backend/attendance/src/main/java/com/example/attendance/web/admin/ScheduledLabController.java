package com.example.attendance.web.admin;

import com.example.attendance.domain.academic.*;
import com.example.attendance.domain.attendance.ScanEventRepository;
import com.example.attendance.domain.schedule.LabSessionRepository;
import com.example.attendance.domain.schedule.LabSubstitutionRepository;
import com.example.attendance.domain.schedule.ScheduleConflicts;
import com.example.attendance.domain.schedule.ScheduledLab;
import com.example.attendance.domain.schedule.ScheduledLabRepository;
import com.example.attendance.domain.schedule.SessionExpansionService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalTime;
import java.util.UUID;

@Controller
@RequestMapping("/scheduled-labs")
public class ScheduledLabController {
    private final ScheduledLabRepository repo;
    private final AcademicTermRepository termRepo;
    private final CourseRepository courseRepo;
    private final StudentGroupRepository groupRepo;
    private final RoomRepository roomRepo;
    private final SessionExpansionService expansionService;
    private final LabSessionRepository labSessionRepo;
    private final ScanEventRepository scanEventRepo;
    private final LabSubstitutionRepository labSubstitutionRepo;

    public ScheduledLabController(ScheduledLabRepository repo,
                                  AcademicTermRepository termRepo,
                                  CourseRepository courseRepo,
                                  StudentGroupRepository groupRepo,
                                  RoomRepository roomRepo,
                                  SessionExpansionService expansionService,
                                  LabSessionRepository labSessionRepo,
                                  ScanEventRepository scanEventRepo,
                                  LabSubstitutionRepository labSubstitutionRepo) {
        this.repo = repo;
        this.termRepo = termRepo;
        this.courseRepo = courseRepo;
        this.groupRepo = groupRepo;
        this.roomRepo = roomRepo;
        this.expansionService = expansionService;
        this.labSessionRepo = labSessionRepo;
        this.scanEventRepo = scanEventRepo;
        this.labSubstitutionRepo = labSubstitutionRepo;
    }

    @GetMapping
    public String list(@RequestParam(required = false) UUID termId, Model model) {
        var terms = termRepo.findAllByOrderByStartDateDesc();
        model.addAttribute("terms", terms);
        if (termId != null) {
            model.addAttribute("selectedTermId", termId);
            model.addAttribute("labs", repo.findByTermIdWithDetails(termId));
        } else if (!terms.isEmpty()) {
            UUID first = terms.get(0).getId();
            model.addAttribute("selectedTermId", first);
            model.addAttribute("labs", repo.findByTermIdWithDetails(first));
        } else {
            model.addAttribute("labs", java.util.List.of());
        }
        model.addAttribute("courses", courseRepo.findAll());
        model.addAttribute("groups", groupRepo.findAll());
        model.addAttribute("rooms", roomRepo.findAll());
        model.addAttribute("weekdays", new String[]{"MONDAY","TUESDAY","WEDNESDAY","THURSDAY","FRIDAY","SATURDAY"});
        model.addAttribute("form", new LabForm());
        return "scheduled-labs/list";
    }

    @PostMapping
    public String create(@ModelAttribute LabForm form, RedirectAttributes ra) {
        var term   = termRepo.findById(form.termId()).orElseThrow();
        var course = courseRepo.findById(form.courseId()).orElseThrow();
        var group  = groupRepo.findById(form.groupId()).orElseThrow();
        var room   = roomRepo.findById(form.roomId()).orElseThrow();

        if (form.startTime() == null || form.endTime() == null || !form.endTime().isAfter(form.startTime())) {
            ra.addFlashAttribute("error", "End time must be after start time.");
            return "redirect:/scheduled-labs?termId=" + form.termId();
        }

        if (form.yearOfStudy() == null) {
            ra.addFlashAttribute("error", "Year of study is required (group names are shared across years).");
            return "redirect:/scheduled-labs?termId=" + form.termId();
        }
        var clash = ScheduleConflicts.firstOverlap(
                repo.findByTermIdWithDetails(form.termId()),
                form.roomId(), form.weekday(), form.startTime(), form.endTime());
        if (clash.isPresent()) {
            ra.addFlashAttribute("error", "Room conflict: " + ScheduleConflicts.describe(clash.get())
                    + " — overlaps the lab you tried to add.");
            return "redirect:/scheduled-labs?termId=" + form.termId();
        }

        repo.save(new ScheduledLab(term, course, group, room,
                form.weekday(), form.startTime(), form.endTime(), form.yearOfStudy()));
        ra.addFlashAttribute("success", "Scheduled lab added.");
        return "redirect:/scheduled-labs?termId=" + form.termId();
    }

    @PostMapping("/expand")
    public String expand(@RequestParam UUID termId, RedirectAttributes ra) {
        int count = expansionService.expandTerm(termId);
        ra.addFlashAttribute("success", "Expanded " + count + " session(s) for term.");
        return "redirect:/scheduled-labs?termId=" + termId;
    }

    @PostMapping("/{id}/delete")
    @Transactional
    public String delete(@PathVariable UUID id, @RequestParam(required = false) UUID termId, RedirectAttributes ra) {
        String redirect = "redirect:/scheduled-labs" + (termId != null ? "?termId=" + termId : "");
        if (!repo.existsById(id)) {
            ra.addFlashAttribute("error", "Scheduled lab not found.");
            return redirect;
        }

        long sessions = labSessionRepo.countByScheduledLabId(id);
        long scans = scanEventRepo.countByScheduledLabId(id);
        scanEventRepo.deleteByScheduledLabId(id);
        labSubstitutionRepo.deleteByScheduledLabId(id);
        labSessionRepo.deleteByScheduledLabId(id);
        repo.deleteById(id);
        ra.addFlashAttribute("success", "Scheduled lab deleted"
            + (sessions > 0 ? " (and " + sessions + " session(s)" : "")
            + (scans > 0 ? ", " + scans + " recorded scan(s)" : "")
            + (sessions > 0 ? ")." : "."));
        return redirect;
    }

    public record LabForm(
            UUID termId, UUID courseId, UUID groupId, UUID roomId,
            String weekday,
            @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime startTime,
            @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime endTime,
            Integer yearOfStudy) {
        public LabForm() { this(null, null, null, null, "MONDAY", null, null, null); }
    }
}
