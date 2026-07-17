package com.example.attendance.domain.biometric;

import com.example.attendance.domain.auth.AppUser;
import com.example.attendance.domain.auth.UserRepository;
import com.example.attendance.domain.student.Student;
import com.example.attendance.domain.student.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class BiometricConsentService {
    public static final String CONSENT_TEXT = """
        Prin prezenta, subsemnatul/a confirm că am fost informat/ă și îmi exprim \
        consimțământul explicit pentru prelucrarea datelor mele biometrice \
        (amprenta digitală) de către facultate, în scopul exclusiv al evidenței \
        prezenței la activitățile de laborator.

        Temei legal: art. 9 alin. (2) lit. (a) din Regulamentul (UE) 2016/679 (GDPR) \
        și Legea nr. 190/2018. Din imaginea amprentei se extrage doar un șablon \
        matematic (minuții), care este stocat criptat; imaginea brută nu este \
        păstrată. Datele biometrice sunt șterse automat la cel mult 6 luni după \
        încheierea ultimei perioade de înscriere.

        Am dreptul de a-mi retrage consimțământul în orice moment și de a solicita \
        ștergerea datelor biometrice, fără ca acest lucru să afecteze legalitatea \
        prelucrării anterioare retragerii.""";

    private final String textVersionHash;

    private final BiometricConsentRepository consentRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;

    public BiometricConsentService(BiometricConsentRepository consentRepository,
                                   StudentRepository studentRepository,
                                   UserRepository userRepository) {
        this.consentRepository = consentRepository;
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
        this.textVersionHash = sha256(CONSENT_TEXT);
    }

    public String getConsentText() { return CONSENT_TEXT; }

    public String getTextVersionHash() { return textVersionHash; }

    public boolean hasActiveConsent(UUID studentId) {
        return consentRepository.existsByStudentIdAndWithdrawnAtIsNull(studentId);
    }

    @Transactional
    public BiometricConsent recordConsent(UUID studentId, String operatorUsername) {
        var existing = consentRepository.findFirstByStudentIdAndWithdrawnAtIsNull(studentId);
        if (existing.isPresent()) {
            return existing.get();
        }
        Student student = studentRepository.findById(studentId)
            .orElseThrow(() -> new IllegalStateException("Student not found: " + studentId));
        AppUser operator = userRepository.findByUsername(operatorUsername)
            .orElseThrow(() -> new IllegalStateException(
                "Operator account not found in users table: " + operatorUsername));
        return consentRepository.save(new BiometricConsent(student, operator, textVersionHash));
    }

    @Transactional
    public boolean withdrawConsent(UUID studentId) {
        var active = consentRepository.findFirstByStudentIdAndWithdrawnAtIsNull(studentId);
        if (active.isEmpty()) return false;
        active.get().withdraw();
        consentRepository.save(active.get());
        return true;
    }

    private static String sha256(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(text.getBytes(StandardCharsets.UTF_8));
            return "v1-sha256:" + HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
