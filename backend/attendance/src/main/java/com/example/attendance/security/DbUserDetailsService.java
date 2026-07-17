package com.example.attendance.security;

import com.example.attendance.domain.auth.AppUser;
import com.example.attendance.domain.auth.UserRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class DbUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    public DbUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AppUser u = userRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("Unknown user: " + username));
        return User.withUsername(u.getUsername())
            .password(u.getPasswordHash())
            .roles(u.getRole().name())
            .build();
    }
}
