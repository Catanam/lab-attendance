package com.example.attendance.domain.attendance;

import com.example.attendance.domain.academic.StudentGroup;
import com.example.attendance.domain.schedule.ScheduledLab;
import com.example.attendance.domain.student.Student;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InCohortTest {
    private final UUID GROUP_A = UUID.randomUUID();
    private final UUID GROUP_B = UUID.randomUUID();

    @Test
    void sameGroupSameYearIsInCohort() {
        StudentGroup g = group(GROUP_A, "1");
        assertTrue(AttendanceService.inCohort(student(g, 2), lab(g, 2)));
    }

    @Test
    void sameGroupDifferentYearIsGuest() {
        StudentGroup g = group(GROUP_A, "1");
        assertFalse(AttendanceService.inCohort(student(g, 1), lab(g, 3)));
    }

    @Test
    void differentGroupIsGuest() {
        assertFalse(AttendanceService.inCohort(
                student(group(GROUP_A, "1"), 2), lab(group(GROUP_B, "2"), 2)));
    }

    @Test
    void nullLabYearMatchesAnyYearOfTheGroup() {
        StudentGroup g = group(GROUP_A, "1");
        assertTrue(AttendanceService.inCohort(student(g, 3), lab(g, null)));
    }

    @Test
    void studentWithoutGroupIsGuest() {
        assertFalse(AttendanceService.inCohort(student(null, 2), lab(group(GROUP_A, "1"), 2)));
    }

    @Test
    void nullStudentIsGuest() {
        assertFalse(AttendanceService.inCohort(null, lab(group(GROUP_A, "1"), 2)));
    }

    private static Student student(StudentGroup group, Integer year) {
        Student s = new Student("M" + UUID.randomUUID(), "Test Student", "t@example.com");
        s.setPrimaryGroup(group);
        s.setYearOfStudy(year);
        return s;
    }

    private static ScheduledLab lab(StudentGroup group, Integer year) {
        return new ScheduledLab(null, null, group, null, "MONDAY", null, null, year);
    }

    private static StudentGroup group(UUID id, String name) {
        StudentGroup g = new StudentGroup(name, null);
        setId(g, id);
        return g;
    }

    private static void setId(StudentGroup g, UUID id) {
        try {
            Field f = StudentGroup.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(g, id);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
