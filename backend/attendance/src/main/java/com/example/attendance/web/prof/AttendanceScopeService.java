package com.example.attendance.web.prof;

import com.example.attendance.domain.academic.Course;
import com.example.attendance.domain.academic.CourseRepository;
import com.example.attendance.domain.academic.CourseTeacherRepository;
import com.example.attendance.domain.auth.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class AttendanceScopeService {
    private final UserRepository userRepository;
    private final CourseTeacherRepository courseTeacherRepository;
    private final CourseRepository courseRepository;

    public AttendanceScopeService(UserRepository userRepository,
                                  CourseTeacherRepository courseTeacherRepository,
                                  CourseRepository courseRepository) {
        this.userRepository = userRepository;
        this.courseTeacherRepository = courseTeacherRepository;
        this.courseRepository = courseRepository;
    }

    public Scope resolve(Authentication auth) {
        if (hasRole(auth, "ROLE_SUPER_ADMIN")) {
            return Scope.all();
        }
        Set<UUID> courseIds = userRepository.findProfessorIdByUsername(auth.getName())
            .map(courseTeacherRepository::findCourseIdsByProfessorId)
            .<Set<UUID>>map(HashSet::new)
            .orElseGet(HashSet::new);
        return Scope.scoped(courseIds);
    }

    private static boolean hasRole(Authentication auth, String role) {
        for (GrantedAuthority a : auth.getAuthorities()) {
            if (role.equals(a.getAuthority())) return true;
        }
        return false;
    }

    public List<CourseOption> selectableCourses(Scope scope) {
        List<Course> courses = scope.seeAll()
            ? courseRepository.findAll()
            : courseRepository.findAllById(scope.courseIds());
        return courses.stream()
            .sorted(Comparator.comparing(Course::getCode, String.CASE_INSENSITIVE_ORDER))
            .map(c -> new CourseOption(c.getId(), c.getCode(), c.getName()))
            .toList();
    }

    public record CourseOption(UUID id, String code, String name) {}

    public record Scope(boolean seeAll, Set<UUID> courseIds) {
        public static Scope all() { return new Scope(true, null); }
        public static Scope scoped(Set<UUID> ids) { return new Scope(false, ids); }

        public Set<UUID> courseFilter() { return seeAll ? null : courseIds; }

        public boolean allows(UUID courseId) {
            return seeAll || (courseIds != null && courseIds.contains(courseId));
        }
    }
}
