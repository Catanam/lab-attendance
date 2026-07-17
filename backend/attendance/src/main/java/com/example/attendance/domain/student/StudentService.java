package com.example.attendance.domain.student;

import com.example.attendance.domain.attendance.ScanEventRepository;
import com.example.attendance.domain.biometric.BiometricConsentRepository;
import com.example.attendance.domain.biometric.FingerprintRegistrationRepository;
import com.example.attendance.domain.biometric.FingerprintTemplateBlobRepository;
import com.example.attendance.domain.biometric.MatchCache;
import com.example.attendance.domain.gdpr.GdprConsentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.UUID;

@Service
public class StudentService {
    private final StudentRepository studentRepo;
    private final FingerprintTemplateBlobRepository blobRepo;
    private final FingerprintRegistrationRepository registrationRepo;
    private final ScanEventRepository scanEventRepo;
    private final BiometricConsentRepository consentRepo;
    private final GdprConsentRepository gdprConsentRepo;
    private final MatchCache matchCache;

    public StudentService(StudentRepository studentRepo,
                          FingerprintTemplateBlobRepository blobRepo,
                          FingerprintRegistrationRepository registrationRepo,
                          ScanEventRepository scanEventRepo,
                          BiometricConsentRepository consentRepo,
                          GdprConsentRepository gdprConsentRepo,
                          MatchCache matchCache) {
        this.studentRepo = studentRepo;
        this.blobRepo = blobRepo;
        this.registrationRepo = registrationRepo;
        this.scanEventRepo = scanEventRepo;
        this.consentRepo = consentRepo;
        this.gdprConsentRepo = gdprConsentRepo;
        this.matchCache = matchCache;
    }

    @Transactional
    public boolean deleteCompletely(UUID studentId) {
        if (!studentRepo.existsById(studentId)) {
            return false;
        }
        cascade(studentId);
        return true;
    }

    @Transactional
    public int deleteCompletely(Collection<UUID> studentIds) {
        int removed = 0;
        for (UUID id : studentIds) {
            if (studentRepo.existsById(id)) {
                cascade(id);
                removed++;
            }
        }
        return removed;
    }

    @Transactional
    public int deactivate(Collection<UUID> studentIds) {
        int changed = 0;
        for (UUID id : studentIds) {
            Student s = studentRepo.findById(id).orElse(null);
            if (s != null && s.isActive()) {
                s.setActive(false);
                studentRepo.save(s);
                changed++;
            }
        }
        return changed;
    }

    private void cascade(UUID studentId) {
        blobRepo.deleteByStudentId(studentId);
        registrationRepo.deleteByStudentId(studentId);
        scanEventRepo.deleteByStudentId(studentId);
        consentRepo.deleteByStudentId(studentId);
        gdprConsentRepo.deleteByStudentId(studentId);
        matchCache.remove(studentId);
        studentRepo.deleteById(studentId);
    }
}
