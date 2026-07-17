package com.example.attendance.domain.gdpr;

import com.example.attendance.domain.student.Student;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class ConsentNotifier {
    private static final Logger log = LoggerFactory.getLogger(ConsentNotifier.class);

    private final ObjectProvider<JavaMailSender> mailSender;
    private final boolean enabled;
    private final String from;

    public ConsentNotifier(ObjectProvider<JavaMailSender> mailSender,
                           @Value("${app.mail.enabled:false}") boolean enabled,
                           @Value("${app.mail.from:}") String from) {
        this.mailSender = mailSender;
        this.enabled = enabled;
        this.from = from;
    }

    public void sendConsentRequest(Student student, String confirmationLink) {
        String email = student.getEmail();
        JavaMailSender sender = mailSender.getIfAvailable();
        if (!enabled || from.isBlank() || sender == null
                || email == null || email.isBlank()) {
            log.info("[GDPR] consent link for {} <{}>: {}",
                student.getMatricolNumber(),
                email == null || email.isBlank() ? "(no email)" : email,
                confirmationLink);
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(email);
        message.setSubject("Consimțământ prelucrare date — Prezență laborator");
        message.setText(body(student, confirmationLink));
        sender.send(message);
        log.info("[GDPR] consent email sent to {} <{}>", student.getMatricolNumber(), email);
    }

    private static String body(Student student, String confirmationLink) {
        return """
            Bună ziua, %s,

            Pentru a putea înregistra prezența la laborator pe baza amprentei digitale, \
            avem nevoie de consimțământul dumneavoastră pentru prelucrarea datelor cu \
            caracter personal.

            Vă rugăm să accesați linkul de mai jos și să confirmați:

            %s

            Înregistrarea amprentei este posibilă numai după acceptarea consimțământului. \
            Vă puteți retrage consimțământul în orice moment.

            Acest mesaj a fost generat automat. Vă rugăm să nu răspundeți.""".formatted(
                student.getFullName(), confirmationLink);
    }
}
