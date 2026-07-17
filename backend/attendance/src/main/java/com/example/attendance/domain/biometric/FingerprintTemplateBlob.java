package com.example.attendance.domain.biometric;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "fingerprint_template_blobs")
public class FingerprintTemplateBlob {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "registration_id", nullable = false)
    private FingerprintRegistration registration;

    @Column(name = "finger_index", nullable = false)
    private int fingerIndex;

    @Column(name = "encrypted_template", nullable = false)
    private byte[] encryptedTemplate;

    @Column(name = "data_key_encrypted", nullable = false)
    private byte[] dataKeyEncrypted;

    @Column(name = "iv", nullable = false)
    private byte[] iv;

    protected FingerprintTemplateBlob() {}

    public FingerprintTemplateBlob(FingerprintRegistration registration, int fingerIndex,
                                   byte[] encryptedTemplate, byte[] dataKeyEncrypted, byte[] iv) {
        this.registration = registration;
        this.fingerIndex = fingerIndex;
        this.encryptedTemplate = encryptedTemplate;
        this.dataKeyEncrypted = dataKeyEncrypted;
        this.iv = iv;
    }

    public UUID getId() { return id; }
    public FingerprintRegistration getRegistration() { return registration; }
    public int getFingerIndex() { return fingerIndex; }
    public byte[] getEncryptedTemplate() { return encryptedTemplate; }
    public byte[] getDataKeyEncrypted() { return dataKeyEncrypted; }
    public byte[] getIv() { return iv; }
}
