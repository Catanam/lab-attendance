package com.example.attendance.domain.auth;

import com.example.attendance.domain.staff.Professor;
import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "users")
public class AppUser {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "professor_id")
    private Professor professor;

    protected AppUser() {}

    public AppUser(String username, String passwordHash, Role role) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
    }

    public UUID getId() { return id; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public Role getRole() { return role; }
    public Professor getProfessor() { return professor; }

    public void setUsername(String v) { this.username = v; }
    public void setPasswordHash(String v) { this.passwordHash = v; }
    public void setRole(Role v) { this.role = v; }
    public void setProfessor(Professor v) { this.professor = v; }
}
