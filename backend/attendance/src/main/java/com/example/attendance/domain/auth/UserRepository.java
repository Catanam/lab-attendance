package com.example.attendance.domain.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<AppUser, UUID> {
    Optional<AppUser> findByUsername(String username);
    boolean existsByUsername(String username);

    @Query("SELECT u FROM AppUser u LEFT JOIN FETCH u.professor ORDER BY u.username")
    List<AppUser> findAllWithProfessor();

    @Query("SELECT u.professor.id FROM AppUser u WHERE u.username = :username")
    Optional<UUID> findProfessorIdByUsername(String username);
}
