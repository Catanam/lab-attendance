package com.example.attendance.web.admin.imports;

import com.example.attendance.domain.academic.StudentGroup;
import com.example.attendance.domain.academic.StudentGroupRepository;
import com.example.attendance.domain.student.Student;
import com.example.attendance.domain.student.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class StudentsImporter {
    private final StudentRepository repo;
    private final StudentGroupRepository groupRepo;

    public StudentsImporter(StudentRepository repo, StudentGroupRepository groupRepo) {
        this.repo = repo;
        this.groupRepo = groupRepo;
    }

    @Transactional
    public ImportResult run(List<SheetRow> rows) {
        ImportResult r = new ImportResult();
        for (SheetRow row : rows) {
            String matricol  = row.get("matricol_number");
            String name      = row.get("full_name");
            String email     = row.get("email");
            String groupName = row.get("group_name");
            String yearRaw   = row.get("year_of_study");

            if (matricol.isEmpty() || name.isEmpty() || email.isEmpty()
                || groupName.isEmpty() || yearRaw.isEmpty()) {
                r.error(row.rowNumber(),
                    "all of matricol_number, full_name, email, year_of_study, group_name are required");
                continue;
            }
            Integer year;
            try {
                year = row.getInt("year_of_study");
            } catch (NumberFormatException e) {
                r.error(row.rowNumber(), "year_of_study must be a number");
                continue;
            }

            StudentGroup group = upsertGroup(groupName);

            Optional<Student> existing = repo.findByMatricolNumber(matricol);
            Student s;
            if (existing.isPresent()) {
                s = existing.get();
                s.setFullName(name);
                s.setEmail(email);
                s.setYearOfStudy(year);
                s.setPrimaryGroup(group);
                repo.save(s);
                r.incUpdated();
            } else {
                s = new Student(matricol, name, email);
                s.setYearOfStudy(year);
                s.setPrimaryGroup(group);
                repo.save(s);
                r.incCreated();
            }
        }
        return r;
    }

    private StudentGroup upsertGroup(String name) {
        return groupRepo.findByName(name).orElseGet(() -> groupRepo.save(new StudentGroup(name, null)));
    }
}
