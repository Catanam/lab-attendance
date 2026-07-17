package com.example.attendance.domain.academic;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface StudentGroupRepository extends JpaRepository<StudentGroup, UUID> {
    Optional<StudentGroup> findByName(String name);
}
