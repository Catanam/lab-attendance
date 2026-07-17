package com.example.attendance.domain.biometric;

import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;

@Component
public class BiometricCipher {
    private static final String ALGO      = "AES/GCM/NoPadding";
    private static final int    GCM_TAG   = 128;
    private static final int    IV_BYTES  = 12;

    private final SecretKey masterKey;
    private final SecureRandom rng = new SecureRandom();

    public BiometricCipher() {
        String hex = System.getenv("BIOMETRIC_MASTER_KEY");
        if (hex == null || hex.length() != 64) {
            hex = "0102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f20";
        }
        masterKey = new SecretKeySpec(hexToBytes(hex), "AES");
    }

    public record Encrypted(byte[] iv, byte[] ciphertext, byte[] wrappedDek) {}

    public Encrypted encrypt(byte[] plaintext) throws Exception {
        KeyGenerator kg = KeyGenerator.getInstance("AES");
        kg.init(256, rng);
        SecretKey dek = kg.generateKey();

        byte[] iv = new byte[IV_BYTES];
        rng.nextBytes(iv);

        Cipher cipher = Cipher.getInstance(ALGO);
        cipher.init(Cipher.ENCRYPT_MODE, dek, new GCMParameterSpec(GCM_TAG, iv));
        byte[] ciphertext = cipher.doFinal(plaintext);

        byte[] wrapIv = new byte[IV_BYTES];
        rng.nextBytes(wrapIv);
        Cipher wrap = Cipher.getInstance(ALGO);
        wrap.init(Cipher.ENCRYPT_MODE, masterKey, new GCMParameterSpec(GCM_TAG, wrapIv));
        byte[] encDek = wrap.doFinal(dek.getEncoded());

        byte[] wrappedDek = new byte[IV_BYTES + encDek.length];
        System.arraycopy(wrapIv, 0, wrappedDek, 0, IV_BYTES);
        System.arraycopy(encDek, 0, wrappedDek, IV_BYTES, encDek.length);

        return new Encrypted(iv, ciphertext, wrappedDek);
    }

    public byte[] decrypt(byte[] iv, byte[] ciphertext, byte[] wrappedDek) throws Exception {
        byte[] wrapIv  = new byte[IV_BYTES];
        byte[] encDek  = new byte[wrappedDek.length - IV_BYTES];
        System.arraycopy(wrappedDek, 0, wrapIv, 0, IV_BYTES);
        System.arraycopy(wrappedDek, IV_BYTES, encDek, 0, encDek.length);

        Cipher wrap = Cipher.getInstance(ALGO);
        wrap.init(Cipher.DECRYPT_MODE, masterKey, new GCMParameterSpec(GCM_TAG, wrapIv));
        SecretKey dek = new SecretKeySpec(wrap.doFinal(encDek), "AES");

        Cipher cipher = Cipher.getInstance(ALGO);
        cipher.init(Cipher.DECRYPT_MODE, dek, new GCMParameterSpec(GCM_TAG, iv));
        return cipher.doFinal(ciphertext);
    }

    private static byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] out = new byte[len / 2];
        for (int i = 0; i < len; i += 2)
            out[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                                 + Character.digit(hex.charAt(i + 1), 16));
        return out;
    }
}
