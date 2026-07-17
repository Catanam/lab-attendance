package com.example.attendance.domain.gdpr;

import com.example.attendance.domain.student.Student;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@Service
public class GdprConsentService {
    public static final String CONSENT_TEXT = """
        Prin confirmarea de mai jos, îmi exprim consimțământul pentru prelucrarea \
        datelor mele cu caracter personal de către facultate, în scopul evidenței \
        prezenței la activitățile de laborator.

        Categorii de date prelucrate: nume și prenume, număr matricol, adresă de \
        e-mail, grupă și an de studiu, înregistrările de prezență, precum și datele \
        biometrice (amprenta digitală) folosite exclusiv pentru identificarea la \
        prezență. Din imaginea amprentei se extrage doar un șablon matematic, stocat \
        criptat; imaginea brută nu este păstrată.

        Temei legal: art. 6 alin. (1) lit. (a) și art. 9 alin. (2) lit. (a) din \
        Regulamentul (UE) 2016/679 (GDPR) și Legea nr. 190/2018. Datele biometrice \
        sunt șterse automat la cel mult 6 luni după încheierea ultimei perioade de \
        înscriere.

        Am dreptul de a-mi retrage consimțământul în orice moment, de a solicita \
        accesul, rectificarea sau ștergerea datelor, fără ca acest lucru să afecteze \
        legalitatea prelucrării efectuate înainte de retragere. Înregistrarea amprentei \
        este posibilă numai după acceptarea acestui consimțământ.""";

    private static final SecureRandom RANDOM = new SecureRandom();

    private final String textVersionHash;
    private final GdprConsentRepository repo;

    public GdprConsentService(GdprConsentRepository repo) {
        this.repo = repo;
        this.textVersionHash = sha256(CONSENT_TEXT);
    }

    public String getConsentText() { return CONSENT_TEXT; }

    public String getTextVersionHash() { return textVersionHash; }

    public boolean hasAccepted(UUID studentId) {
        return repo.existsByStudentIdAndStatus(studentId, GdprConsentStatus.ACCEPTED);
    }

    public Optional<GdprConsent> findForStudent(UUID studentId) {
        return repo.findByStudentId(studentId);
    }

    public Optional<GdprConsent> findByToken(String token) {
        return repo.findByToken(token);
    }

    @Transactional
    public GdprConsent issue(Student student) {
        GdprConsent c = repo.findByStudentId(student.getId())
            .orElseGet(() -> new GdprConsent(student));
        c.issue(newToken(), textVersionHash);
        return repo.save(c);
    }

    @Transactional
    public Optional<GdprConsent> accept(String token) {
        Optional<GdprConsent> c = repo.findByToken(token);
        c.ifPresent(x -> { x.accept(); repo.save(x); });
        return c;
    }

    @Transactional
    public Optional<GdprConsent> decline(String token) {
        Optional<GdprConsent> c = repo.findByToken(token);
        c.ifPresent(x -> { x.decline(); repo.save(x); });
        return c;
    }

    private static String newToken() {
        byte[] bytes = new byte[24];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
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
