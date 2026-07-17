package com.example.attendance.security;

import com.example.attendance.security.hmac.DeviceCredentialResolver;
import com.example.attendance.security.hmac.HmacAuthFilter;
import com.example.attendance.security.hmac.NonceCache;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private final DeviceCredentialResolver deviceCredentialResolver;
    private final NonceCache nonceCache;

    public SecurityConfig(DeviceCredentialResolver deviceCredentialResolver, NonceCache nonceCache) {
        this.deviceCredentialResolver = deviceCredentialResolver;
        this.nonceCache = nonceCache;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        HmacAuthFilter hmacFilter = new HmacAuthFilter(deviceCredentialResolver, nonceCache);
        http
            .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))
            .addFilterBefore(hmacFilter, UsernamePasswordAuthenticationFilter.class)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/login/**", "/css/**", "/js/**", "/webjars/**").permitAll()
                .requestMatchers("/consent", "/consent/**").permitAll()
                .requestMatchers("/api/**").hasRole("DEVICE")
                .requestMatchers("/admin/**").hasRole("SUPER_ADMIN")
                .requestMatchers("/accounts/**").hasRole("SUPER_ADMIN")
                .requestMatchers("/gdpr/**").hasRole("SUPER_ADMIN")

                .requestMatchers("/students/**", "/professors/**", "/courses/**", "/groups/**",
                                 "/rooms/**", "/terms/**", "/scheduled-labs/**", "/devices/**",
                                 "/register/**", "/import/**").hasRole("SUPER_ADMIN")
                .requestMatchers("/prof/**").hasAnyRole("PROFESSOR", "SUPER_ADMIN")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .defaultSuccessUrl("/", true)
                .failureUrl("/login?error")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            );
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
