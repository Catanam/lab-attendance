package com.example.attendance.web.prof;

import com.example.attendance.AppClock;
import com.example.attendance.domain.attendance.AttendanceService;
import com.example.attendance.domain.attendance.AttendanceStatus;
import com.example.attendance.domain.attendance.dto.AttendanceExportRowDTO;
import com.example.attendance.domain.attendance.dto.AttendancePivotDTO;
import com.example.attendance.domain.attendance.dto.AttendanceRowDTO;
import com.example.attendance.domain.attendance.dto.SessionAttendanceDTO;
import com.example.attendance.domain.schedule.LabSession;
import com.example.attendance.domain.schedule.LabSessionRepository;
import com.example.attendance.domain.schedule.LabSessionStatus;
import com.example.attendance.web.sse.SseHub;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

@Controller
@RequestMapping("/prof")
public class ProfAttendanceController {
    private final AttendanceService attendanceService;
    private final LabSessionRepository labSessionRepository;
    private final SseHub sseHub;
    private final AttendanceScopeService scopeService;

    public ProfAttendanceController(AttendanceService attendanceService,
                                    LabSessionRepository labSessionRepository,
                                    SseHub sseHub,
                                    AttendanceScopeService scopeService) {
        this.attendanceService = attendanceService;
        this.labSessionRepository = labSessionRepository;
        this.sseHub = sseHub;
        this.scopeService = scopeService;
    }

    @GetMapping
    public String today(@RequestParam(required = false) UUID courseId,
                        Model model, Authentication auth) {
        AttendanceScopeService.Scope scope = scopeService.resolve(auth);
        List<AttendanceScopeService.CourseOption> courses = scopeService.selectableCourses(scope);
        CourseSelection sel = courseSelection(scope, courseId, courses);
        model.addAttribute("sessions",
            attendanceService.todaySessions(AppClock.now(), sel.filter()));
        model.addAttribute("courses", courses);
        model.addAttribute("selectedCourseId", sel.selected());
        return "prof/today";
    }

    private CourseSelection courseSelection(AttendanceScopeService.Scope scope, UUID courseId,
                                            List<AttendanceScopeService.CourseOption> courses) {
        UUID selected = (courseId != null && scope.allows(courseId)) ? courseId : null;
        if (selected == null && !courses.isEmpty()) {
            selected = courses.get(0).id();
        }
        Set<UUID> filter = selected != null ? Set.of(selected) : Set.of();
        return new CourseSelection(selected, filter);
    }

    private record CourseSelection(UUID selected, Set<UUID> filter) {}

    @GetMapping("/sessions/{id}")
    public String session(@PathVariable UUID id, Model model, Authentication auth, RedirectAttributes ra) {
        if (!canAccessSession(id, auth)) {
            ra.addFlashAttribute("error", "That session belongs to a course you don't teach.");
            return "redirect:/prof";
        }
        model.addAttribute("att", attendanceService.buildSessionAttendance(id));
        return "prof/session";
    }

    private boolean canAccessSession(UUID sessionId, Authentication auth) {
        AttendanceScopeService.Scope scope = scopeService.resolve(auth);
        if (scope.seeAll()) return true;
        UUID courseId = labSessionRepository.findCourseIdById(sessionId).orElse(null);
        return courseId != null && scope.allows(courseId);
    }

    private static final LocalDate MIN_DATE = LocalDate.of(2000, 1, 1);
    private static final LocalDate MAX_DATE = LocalDate.of(2100, 1, 1);

    private static LocalDate parseOr(String s, LocalDate fallback) {
        if (s == null || s.isBlank()) return fallback;
        try { return LocalDate.parse(s.trim()); } catch (Exception e) { return fallback; }
    }

    private static LocalDate capToToday(LocalDate to) {
        LocalDate today = AppClock.now().toLocalDate();
        return to.isAfter(today) ? today : to;
    }

    @GetMapping("/attendance")
    public String allAttendance(@RequestParam(required = false) String from,
                                @RequestParam(required = false) String to,
                                @RequestParam(required = false) UUID courseId,
                                Model model, Authentication auth) {
        AttendanceScopeService.Scope scope = scopeService.resolve(auth);
        List<AttendanceScopeService.CourseOption> courses = scopeService.selectableCourses(scope);
        CourseSelection sel = courseSelection(scope, courseId, courses);
        List<AttendanceExportRowDTO> rows = attendanceService.buildAllAttendance(
            parseOr(from, MIN_DATE), capToToday(parseOr(to, MAX_DATE)), sel.filter());
        model.addAttribute("pivot", pivot(rows));
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        model.addAttribute("courses", courses);
        model.addAttribute("selectedCourseId", sel.selected());
        return "prof/attendance";
    }

    private static AttendancePivotDTO pivot(List<AttendanceExportRowDTO> rows) {
        TreeSet<LocalDate> days = new TreeSet<>();
        Map<String, String> nameByKey = new LinkedHashMap<>();
        Map<String, Map<LocalDate, AttendanceStatus>> byKey = new LinkedHashMap<>();
        for (AttendanceExportRowDTO r : rows) {
            days.add(r.date());
            String key = (r.matricolNumber() == null || r.matricolNumber().isBlank())
                ? "name:" + r.fullName() : r.matricolNumber();
            nameByKey.putIfAbsent(key, r.fullName());
            byKey.computeIfAbsent(key, k -> new LinkedHashMap<>())
                 .merge(r.date(), r.status(),
                        (a, b) -> a.ordinal() <= b.ordinal() ? a : b);
        }
        List<AttendancePivotDTO.Row> students = byKey.entrySet().stream()
            .map(e -> new AttendancePivotDTO.Row(e.getKey(), nameByKey.get(e.getKey()), e.getValue()))
            .sorted(Comparator.comparing(AttendancePivotDTO.Row::fullName,
                                         Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
            .toList();
        return new AttendancePivotDTO(new ArrayList<>(days), students);
    }

    @GetMapping("/attendance/export.csv")
    public void exportAllCsv(@RequestParam(required = false) String from,
                             @RequestParam(required = false) String to,
                             @RequestParam(required = false) UUID courseId,
                             HttpServletResponse response, Authentication auth) throws IOException {
        AttendanceScopeService.Scope scope = scopeService.resolve(auth);
        List<AttendanceScopeService.CourseOption> courses = scopeService.selectableCourses(scope);
        CourseSelection sel = courseSelection(scope, courseId, courses);
        List<AttendanceExportRowDTO> rows = attendanceService.buildAllAttendance(
            parseOr(from, MIN_DATE), capToToday(parseOr(to, MAX_DATE)), sel.filter());
        AttendancePivotDTO p = pivot(rows);

        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition",
            contentDisposition(exportFilename(courseName(courses, sel.selected()), "csv")));
        PrintWriter w = response.getWriter();

        StringBuilder header = new StringBuilder("matricol,name");
        for (LocalDate day : p.days()) header.append(',').append(day);
        w.println(header);

        for (AttendancePivotDTO.Row r : p.students()) {
            String matricol = (r.matricolNumber() == null || r.matricolNumber().startsWith("name:"))
                ? "" : r.matricolNumber();
            StringBuilder line = new StringBuilder();
            line.append(csv(matricol)).append(',').append(csv(r.fullName()));
            for (LocalDate day : p.days()) {
                AttendanceStatus st = r.byDay().get(day);
                line.append(',').append(st == null ? "" : statusWord(st));
            }
            w.println(line);
        }
        w.flush();
    }

    @GetMapping("/attendance/export.xlsx")
    public void exportAllXlsx(@RequestParam(required = false) String from,
                              @RequestParam(required = false) String to,
                              @RequestParam(required = false) UUID courseId,
                              HttpServletResponse response, Authentication auth) throws IOException {
        AttendanceScopeService.Scope scope = scopeService.resolve(auth);
        List<AttendanceScopeService.CourseOption> courses = scopeService.selectableCourses(scope);
        CourseSelection sel = courseSelection(scope, courseId, courses);
        List<AttendanceExportRowDTO> rows = attendanceService.buildAllAttendance(
            parseOr(from, MIN_DATE), capToToday(parseOr(to, MAX_DATE)), sel.filter());
        AttendancePivotDTO p = pivot(rows);

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Attendance");

            CellStyle headerStyle = wb.createCellStyle();
            XSSFFont headerFont = wb.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            CellStyle presentStyle = statusStyle(wb, 0xC6, 0xEF, 0xCE, 0x00, 0x61, 0x00);
            CellStyle lateStyle    = statusStyle(wb, 0xFF, 0xEB, 0x9C, 0x9C, 0x65, 0x00);
            CellStyle absentStyle  = statusStyle(wb, 0xFF, 0xC7, 0xCE, 0x9C, 0x00, 0x06);

            Row head = sheet.createRow(0);
            cell(head, 0, "matricol", headerStyle);
            cell(head, 1, "name", headerStyle);
            int hc = 2;
            for (LocalDate day : p.days()) cell(head, hc++, day.toString(), headerStyle);

            int rowIdx = 1;
            for (AttendancePivotDTO.Row r : p.students()) {
                Row xr = sheet.createRow(rowIdx++);
                String matricol = (r.matricolNumber() == null || r.matricolNumber().startsWith("name:"))
                    ? "" : r.matricolNumber();
                cell(xr, 0, matricol, null);
                cell(xr, 1, r.fullName(), null);
                int c = 2;
                for (LocalDate day : p.days()) {
                    AttendanceStatus st = r.byDay().get(day);
                    Cell xc = xr.createCell(c++);
                    if (st != null) {
                        xc.setCellValue(statusWord(st));
                        xc.setCellStyle(switch (st) {
                            case PRESENT -> presentStyle;
                            case LATE -> lateStyle;
                            case ABSENT -> absentStyle;
                        });
                    }
                }
            }

            sheet.createFreezePane(2, 1);
            sheet.setColumnWidth(0, 14 * 256);
            sheet.setColumnWidth(1, 28 * 256);
            for (int i = 2; i < 2 + p.days().size(); i++) sheet.setColumnWidth(i, 11 * 256);

            String fname = exportFilename(courseName(courses, sel.selected()), "xlsx");
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", contentDisposition(fname));

            try (OutputStream out = response.getOutputStream()) {
                wb.write(out);
                out.flush();
            }
        }
    }

    private static String statusWord(AttendanceStatus s) {
        return switch (s) {
            case PRESENT -> "Present";
            case LATE -> "Late";
            case ABSENT -> "Absent";
        };
    }

    private static CellStyle statusStyle(XSSFWorkbook wb,
                                         int fr, int fg, int fb, int tr, int tg, int tb) {
        XSSFCellStyle st = wb.createCellStyle();
        st.setFillForegroundColor(new XSSFColor(new byte[]{(byte) fr, (byte) fg, (byte) fb}, null));
        st.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        XSSFFont font = wb.createFont();
        font.setColor(new XSSFColor(new byte[]{(byte) tr, (byte) tg, (byte) tb}, null));
        st.setFont(font);
        return st;
    }

    private static void cell(Row row, int col, String value, CellStyle style) {
        Cell c = row.createCell(col);
        c.setCellValue(value == null ? "" : value);
        if (style != null) c.setCellStyle(style);
    }

    private static String courseName(List<AttendanceScopeService.CourseOption> courses, UUID selected) {
        if (selected == null) return "";
        return courses.stream()
            .filter(c -> selected.equals(c.id()))
            .map(AttendanceScopeService.CourseOption::name)
            .findFirst().orElse("");
    }

    private static String exportFilename(String courseName, String ext) {
        String base = (courseName == null || courseName.isBlank()) ? "attendance" : courseName + " attendance";
        return base + "." + ext;
    }

    private static String contentDisposition(String filename) {
        String ascii = filename.replaceAll("[^\\x20-\\x7E]", "_").replace("\"", "");
        String utf8 = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        return "attachment; filename=\"" + ascii + "\"; filename*=UTF-8''" + utf8;
    }

    @GetMapping(value = "/sessions/{id}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ResponseBody
    public SseEmitter events(@PathVariable UUID id, Authentication auth) {
        if (!canAccessSession(id, auth)) {
            SseEmitter closed = new SseEmitter(0L);
            closed.complete();
            return closed;
        }
        return sseHub.subscribeAttendance(id);
    }

    @GetMapping("/sessions/{id}/export.csv")
    public void exportCsv(@PathVariable UUID id, HttpServletResponse response) throws IOException {
        SessionAttendanceDTO s = attendanceService.buildSessionAttendance(id);
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition",
            "attachment; filename=attendance-" + id + ".csv");
        PrintWriter w = response.getWriter();
        w.println("matricol,name,status,scan_time,guest,should_count");
        for (AttendanceRowDTO r : s.enrolled()) writeRow(w, r);
        for (AttendanceRowDTO r : s.guests()) writeRow(w, r);
        w.flush();
    }

    private void writeRow(PrintWriter w, AttendanceRowDTO r) {
        w.printf("%s,%s,%s,%s,%s,%s%n",
            csv(r.matricolNumber()), csv(r.fullName()), r.status().name(),
            r.scanTime() == null ? "" : r.scanTime(), r.guest(), r.shouldCount());
    }

    private String csv(String v) {
        if (v == null) return "";
        if (v.contains(",") || v.contains("\"") || v.contains("\n")) {
            return "\"" + v.replace("\"", "\"\"") + "\"";
        }
        return v;
    }

    @PostMapping("/scan-events/{id}/toggle-count")
    public String toggleCount(@PathVariable UUID id, @RequestParam UUID sessionId,
                              Authentication auth, RedirectAttributes ra) {
        if (!canAccessSession(sessionId, auth)) {
            ra.addFlashAttribute("error", "That session belongs to a course you don't teach.");
            return "redirect:/prof";
        }
        AttendanceService.ToggleResult result = attendanceService.toggleShouldCount(id);
        sseHub.sendAttendanceEvent(result.sessionId(), "toggle", result.row());
        ra.addFlashAttribute("success", "Updated should-count flag.");
        return "redirect:/prof/sessions/" + sessionId;
    }

    @PostMapping("/sessions/{id}/cancel")
    public String cancel(@PathVariable UUID id, Authentication auth, RedirectAttributes ra) {
        if (!canAccessSession(id, auth)) {
            ra.addFlashAttribute("error", "That session belongs to a course you don't teach.");
            return "redirect:/prof";
        }
        LabSession session = labSessionRepository.findById(id).orElse(null);
        if (session == null) {
            ra.addFlashAttribute("error", "Session not found.");
            return "redirect:/prof";
        }
        session.setStatus(LabSessionStatus.CANCELLED);
        labSessionRepository.save(session);
        ra.addFlashAttribute("warning", "Session cancelled.");
        return "redirect:/prof";
    }
}
