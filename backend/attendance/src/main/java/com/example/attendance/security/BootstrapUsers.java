package com.example.attendance.security;

import com.example.attendance.domain.auth.AppUser;
import com.example.attendance.domain.auth.Role;
import com.example.attendance.domain.auth.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class BootstrapUsers implements CommandLineRunner {
    private record Bootstrap(String username, String defaultPassword, Role role) {}

    private static final List<Bootstrap> ACCOUNTS = List.of(
        new Bootstrap("superadmin", "superadmin", Role.SUPER_ADMIN),

        new Bootstrap("staff",      "staff",      Role.SUPER_ADMIN),
        new Bootstrap("prof",       "prof",       Role.PROFESSOR)
    );

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public BootstrapUsers(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        for (Bootstrap b : ACCOUNTS) {
            var existing = userRepository.findByUsername(b.username());
            if (existing.isEmpty()) {
                userRepository.save(new AppUser(b.username(),
                    passwordEncoder.encode(b.defaultPassword()), b.role()));
                System.out.println("[BootstrapUsers] Created account: " + b.username());
            } else {
                AppUser u = existing.get();
                if (u.getPasswordHash() == null || !u.getPasswordHash().startsWith("$2")) {
                    u.setPasswordHash(passwordEncoder.encode(b.defaultPassword()));
                    userRepository.save(u);
                    System.out.println("[BootstrapUsers] Repaired placeholder password for: " + b.username());
                }
            }
        }
    }
}
