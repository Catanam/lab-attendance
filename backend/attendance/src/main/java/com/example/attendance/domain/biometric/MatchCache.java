package com.example.attendance.domain.biometric;

import com.machinezoo.sourceafis.FingerprintTemplate;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class MatchCache {
    private final Map<UUID, List<FingerprintTemplate>> cache = new ConcurrentHashMap<>();

    private final FingerprintTemplateBlobRepository blobRepository;
    private final BiometricCipher cipher;

    public MatchCache(FingerprintTemplateBlobRepository blobRepository, BiometricCipher cipher) {
        this.blobRepository = blobRepository;
        this.cipher = cipher;
    }

    @PostConstruct
    public void load() {
        cache.clear();
        for (FingerprintTemplateBlob blob : blobRepository.findAllActive()) {
            UUID studentId = blob.getRegistration().getStudent().getId();
            try {
                byte[] plain = cipher.decrypt(blob.getIv(), blob.getEncryptedTemplate(), blob.getDataKeyEncrypted());
                cache.computeIfAbsent(studentId, k -> new CopyOnWriteArrayList<>())
                     .add(new FingerprintTemplate(plain));
            } catch (Exception e) {
                System.err.println("[MatchCache] Failed to decrypt blob " + blob.getId() + ": " + e.getMessage());
            }
        }
        System.out.println("[MatchCache] Loaded templates for " + cache.size() + " student(s).");
    }

    public void put(UUID studentId, FingerprintTemplate template) {
        cache.computeIfAbsent(studentId, k -> new CopyOnWriteArrayList<>()).add(template);
    }

    public void remove(UUID studentId) {
        cache.remove(studentId);
    }

    public Map<UUID, List<FingerprintTemplate>> all() {
        return cache;
    }
}
