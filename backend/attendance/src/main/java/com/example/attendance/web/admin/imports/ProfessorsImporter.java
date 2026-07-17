package com.example.attendance.web.admin.imports;

import com.example.attendance.domain.academic.Course;
import com.example.attendance.domain.academic.CourseRepository;
import com.example.attendance.domain.academic.CourseTeacher;
import com.example.attendance.domain.academic.CourseTeacherRepository;
import com.example.attendance.domain.academic.CourseTeacherRole;
import com.example.attendance.domain.staff.Professor;
import com.example.attendance.domain.staff.ProfessorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProfessorsImporter {
    private final ProfessorRepository profRepo;
    private final CourseRepository courseRepo;
    private final CourseTeacherRepository teacherRepo;

    public ProfessorsImporter(ProfessorRepository profRepo,
                              CourseRepository courseRepo,
                              CourseTeacherRepository teacherRepo) {
        this.profRepo = profRepo;
        this.courseRepo = courseRepo;
        this.teacherRepo = teacherRepo;
    }

    @Transactional
    public ImportResult run(List<SheetRow> rows) {
        ImportResult r = new ImportResult();
        for (SheetRow row : rows) {
            String profName  = row.get("prof_full_name");
            String courseCd  = row.get("course_code");
            String courseNm  = row.get("course_name");
            String profTitle = row.has("prof_title") ? row.get("prof_title") : "";
            String profEmail = row.has("prof_email") ? row.get("prof_email") : "";
            String roleRaw   = row.has("role")        ? row.get("role").toUpperCase() : "";

            if (profName.isEmpty() || courseCd.isEmpty() || courseNm.isEmpty()) {
                r.error(row.rowNumber(), "prof_full_name, course_code, course_name are required");
                continue;
            }

            CourseTeacherRole role;
            if (roleRaw.isEmpty() || roleRaw.equals("PRIMARY")) {
                role = CourseTeacherRole.PRIMARY;
            } else if (roleRaw.equals("ASSISTANT")) {
                role = CourseTeacherRole.ASSISTANT;
            } else {
                r.error(row.rowNumber(), "role must be PRIMARY or ASSISTANT (got: " + roleRaw + ")");
                continue;
            }

            Professor prof = upsertProfessor(profName, profTitle, profEmail);
            Course course  = upsertCourse(courseCd, courseNm);

            var existing = teacherRepo.findByCourseIdAndProfessorId(course.getId(), prof.getId());
            if (existing.isPresent()) {
                CourseTeacher ct = existing.get();
                if (ct.getRole() != role) {
                    ct.setRole(role);
                    teacherRepo.save(ct);
                    r.incUpdated();
                } else {
                    r.incSkipped();
                }
            } else {
                teacherRepo.save(new CourseTeacher(course, prof, role));
                r.incCreated();
            }
        }
        return r;
    }

    private Professor upsertProfessor(String fullName, String title, String email) {
        Professor existing = null;
        if (!email.isEmpty()) {
            existing = profRepo.findByEmail(email).orElse(null);
        }
        if (existing == null) {
            var byName = profRepo.findByFullNameIgnoreCase(fullName);
            if (!byName.isEmpty()) existing = byName.get(0);
        }
        if (existing != null) {
            if (!title.isEmpty()) existing.setTitle(title);
            if (!email.isEmpty()) existing.setEmail(email);
            existing.setFullName(fullName);
            return profRepo.save(existing);
        }
        return profRepo.save(new Professor(
            title.isEmpty() ? null : title,
            fullName,
            email.isEmpty() ? null : email,
            null, null));
    }

    private Course upsertCourse(String code, String name) {
        return courseRepo.findByCode(code).map(c -> {
            if (!c.getName().equals(name)) {
                c.setName(name);
                return courseRepo.save(c);
            }
            return c;
        }).orElseGet(() -> courseRepo.save(new Course(code, name)));
    }
}
