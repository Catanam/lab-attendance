package com.example.attendance.web.admin.imports;

import com.example.attendance.domain.academic.*;
import com.example.attendance.domain.schedule.ScheduleConflicts;
import com.example.attendance.domain.schedule.ScheduledLab;
import com.example.attendance.domain.schedule.ScheduledLabRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

@Service
public class ScheduleImporter {
    private static final Set<String> WEEKDAYS = Set.of(
        "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY");

    private final AcademicTermRepository termRepo;
    private final CourseRepository courseRepo;
    private final StudentGroupRepository groupRepo;
    private final RoomRepository roomRepo;
    private final ScheduledLabRepository labRepo;

    public ScheduleImporter(AcademicTermRepository termRepo,
                            CourseRepository courseRepo,
                            StudentGroupRepository groupRepo,
                            RoomRepository roomRepo,
                            ScheduledLabRepository labRepo) {
        this.termRepo = termRepo;
        this.courseRepo = courseRepo;
        this.groupRepo = groupRepo;
        this.roomRepo = roomRepo;
        this.labRepo = labRepo;
    }

    @Transactional
    public ImportResult run(List<SheetRow> rows) {
        ImportResult r = new ImportResult();

        AcademicTerm term = termRepo.findByCurrentTrue().orElse(null);
        if (term == null) {
            r.error("No current term set. Create a term at /terms and mark it as Current before importing the schedule.");
            return r;
        }

        for (SheetRow row : rows) {
            String groupName = row.get("group_name");
            String courseCd  = row.get("course_code");
            String weekday   = row.get("weekday").toUpperCase(Locale.ROOT);
            String roomName  = row.get("room_name");
            LocalTime start, end;
            try {
                start = row.getTime("start_time");
                end   = row.getTime("end_time");
            } catch (Exception e) {
                r.error(row.rowNumber(), "start_time/end_time must be HH:mm (got: '" +
                    row.get("start_time") + "' / '" + row.get("end_time") + "')");
                continue;
            }
            Integer year;
            try {
                year = row.getInt("year_of_study");
            } catch (NumberFormatException e) {
                r.error(row.rowNumber(), "year_of_study must be a number");
                continue;
            }

            if (groupName.isEmpty() || courseCd.isEmpty() || weekday.isEmpty()
                || roomName.isEmpty() || start == null || end == null || year == null) {
                r.error(row.rowNumber(), "all of group_name, year_of_study, course_code, weekday, start_time, end_time, room_name are required");
                continue;
            }
            if (!WEEKDAYS.contains(weekday)) {
                r.error(row.rowNumber(), "weekday must be MONDAY..SUNDAY (got: " + weekday + ")");
                continue;
            }
            if (!end.isAfter(start)) {
                r.error(row.rowNumber(), "end_time must be after start_time");
                continue;
            }

            Course course = courseRepo.findByCode(courseCd).orElse(null);
            if (course == null) {
                r.error(row.rowNumber(), "unknown course_code: " + courseCd + " (upload professors.* first)");
                continue;
            }
            StudentGroup group = groupRepo.findByName(groupName)
                .orElseGet(() -> groupRepo.save(new StudentGroup(groupName, null)));
            Room room = roomRepo.findByName(roomName)
                .orElseGet(() -> roomRepo.save(new Room(roomName, null)));

            List<ScheduledLab> termLabs = labRepo.findByTermIdWithDetails(term.getId());

            boolean exists = termLabs.stream().anyMatch(l ->
                l.getCourse().getId().equals(course.getId()) &&
                l.getGroup().getId().equals(group.getId()) &&
                Objects.equals(l.getYearOfStudy(), year) &&
                l.getWeekday().equals(weekday) &&
                l.getStartTime().equals(start));
            if (exists) {
                r.incSkipped();
                continue;
            }

            var clash = ScheduleConflicts.firstOverlap(termLabs, room.getId(), weekday, start, end);
            if (clash.isPresent()) {
                r.error(row.rowNumber(), "room conflict: " + ScheduleConflicts.describe(clash.get()));
                continue;
            }

            labRepo.save(new ScheduledLab(term, course, group, room, weekday, start, end, year));
            r.incCreated();
        }
        return r;
    }
}
