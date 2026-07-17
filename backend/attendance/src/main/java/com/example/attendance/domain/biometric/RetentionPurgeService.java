package com.example.attendance.domain.biometric;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class RetentionPurgeService {
    private final FingerprintRegistrationRepository registrationRepository;
    private final RegistrationService registrationService;
    private final BiometricConsentService consentService;
    private final int retentionMonths;

    public RetentionPurgeService(FingerprintRegistrationRepository registrationRepository,
                                 RegistrationService registrationService,
                                 BiometricConsentService consentService,
                                 @Value("${attendance.retention-months:6}") int retentionMonths) {
        this.registrationRepository = registrationRepository;
        this.registrationService = registrationService;
        this.consentService = consentService;
        this.retentionMonths = retentionMonths;
    }

    @Scheduled(cron = "0 0 3 * * *")
    public void scheduledPurge() {
        int purged = purgeExpired();
        if (purged > 0) {
            System.out.println("[RetentionPurge] Scheduled run purged biometric data for "
                + purged + " student(s).");
        }
    }

    public int purgeExpired() {
        LocalDate cutoff = LocalDate.now().minusMonths(retentionMonths);
        List<UUID> eligible = registrationRepository.findStudentIdsEligibleForPurge(cutoff);
        int purgedStudents = 0;
        for (UUID studentId : eligible) {
            int removed = registrationService.purgeStudentFingerprints(studentId);
            consentService.withdrawConsent(studentId);
            if (removed > 0) {
                purgedStudents++;
                System.out.println("[RetentionPurge] Purged " + removed
                    + " registration(s) for student " + studentId
                    + " (last term ended before " + cutoff + ").");
            }
        }
        return purgedStudents;
    }

    public int getRetentionMonths() { return retentionMonths; }
}
