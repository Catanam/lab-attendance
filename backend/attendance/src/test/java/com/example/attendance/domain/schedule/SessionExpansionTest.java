package com.example.attendance.domain.schedule;

import com.example.attendance.domain.academic.AcademicTerm;
import com.example.attendance.domain.academic.AcademicTermRepository;
import com.example.attendance.domain.academic.Course;
import com.example.attendance.domain.academic.CourseRepository;
import com.example.attendance.domain.academic.Room;
import com.example.attendance.domain.academic.RoomRepository;
import com.example.attendance.domain.academic.StudentGroup;
import com.example.attendance.domain.academic.StudentGroupRepository;
import com.example.attendance.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SessionExpansionTest extends AbstractIntegrationTest {
    @Autowired AcademicTermRepository termRepo;
    @Autowired CourseRepository courseRepo;
    @Autowired StudentGroupRepository groupRepo;
    @Autowired RoomRepository roomRepo;
    @Autowired ScheduledLabRepository labRepo;
    @Autowired SessionExpansionService expansionService;

    @Test
    void expandsOneSessionPerWeekdayThenIsIdempotent() {
        String tag = UUID.randomUUID().toString().substring(0, 8);

        AcademicTerm term = termRepo.save(new AcademicTerm(
                "T-" + tag, "Test term", LocalDate.of(2025, 1, 6), LocalDate.of(2025, 1, 20)));
        Course course = courseRepo.save(new Course("C-" + tag, "Test course"));
        StudentGroup group = groupRepo.save(new StudentGroup("G-" + tag, null));
        Room room = roomRepo.save(new Room("R-" + tag, "B"));
        labRepo.save(new ScheduledLab(term, course, group, room,
                "MONDAY", LocalTime.of(10, 0), LocalTime.of(12, 0), 1));

        int firstRun = expansionService.expandTerm(term.getId());
        assertEquals(3, firstRun, "three Mondays between 2025-01-06 and 2025-01-20");

        int secondRun = expansionService.expandTerm(term.getId());
        assertEquals(0, secondRun, "re-running must not duplicate sessions");
    }
}
