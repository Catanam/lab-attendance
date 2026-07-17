package com.example.attendance.domain.academic;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AcademicTermRepository extends JpaRepository<AcademicTerm, UUID> {
    List<AcademicTerm> findAllByOrderByStartDateDesc();
    Optional<AcademicTerm> findByCode(String code);
    Optional<AcademicTerm> findByCurrentTrue();
}
