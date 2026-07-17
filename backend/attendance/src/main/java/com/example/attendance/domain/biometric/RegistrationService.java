package com.example.attendance.domain.biometric;

import com.machinezoo.sourceafis.FingerprintTemplate;
import com.example.attendance.domain.student.Student;
import com.example.attendance.domain.student.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class RegistrationService {
    private final FingerprintImageDecoder imageDecoder;
    private final BiometricCipher cipher;
    private final FingerprintRegistrationRepository registrationRepository;
    private final FingerprintTemplateBlobRepository blobRepository;
    private final MatchCache matchCache;
    private final StudentRepository studentRepository;

    public RegistrationService(FingerprintImageDecoder imageDecoder,
                                BiometricCipher cipher,
                                FingerprintRegistrationRepository registrationRepository,
                                FingerprintTemplateBlobRepository blobRepository,
                                MatchCache matchCache,
                                StudentRepository studentRepository) {
        this.imageDecoder = imageDecoder;
        this.cipher = cipher;
        this.registrationRepository = registrationRepository;
        this.blobRepository = blobRepository;
        this.matchCache = matchCache;
        this.studentRepository = studentRepository;
    }

    public FingerprintTemplate extractTemplate(byte[] raw4bit) throws Exception {
        return new FingerprintTemplate(imageDecoder.decode(raw4bit));
    }

    @Transactional
    public int purgeStudentFingerprints(UUID studentId) {
        var regs = registrationRepository.findByStudentId(studentId);

        registrationRepository.deleteAll(regs);
        matchCache.remove(studentId);
        return regs.size();
    }

    @Transactional
    public void persistRegistration(RegistrationSession session) throws Exception {
        UUID studentId = session.getStudentId();
        Student student = studentRepository.findById(studentId)
            .orElseThrow(() -> new IllegalStateException("Student not found: " + studentId));

        for (FingerprintRegistration old : registrationRepository.findByStudentIdAndActiveTrue(studentId)) {
            old.setActive(false);
            registrationRepository.save(old);
        }

        FingerprintRegistration reg = new FingerprintRegistration(student);
        registrationRepository.save(reg);

        int idx = 0;
        for (FingerprintTemplate template : session.getSamples()) {
            BiometricCipher.Encrypted enc = cipher.encrypt(template.toByteArray());
            FingerprintTemplateBlob blob = new FingerprintTemplateBlob(
                reg, idx++, enc.ciphertext(), enc.wrappedDek(), enc.iv());
            blobRepository.save(blob);
            matchCache.put(studentId, template);
        }
    }
}
