package com.example.attendance.domain.biometric;

import com.machinezoo.sourceafis.FingerprintImage;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.ByteArrayOutputStream;

@Component
public class FingerprintImageDecoder {
    private static final int IMG_WIDTH  = 256;
    private static final int IMG_HEIGHT = 288;

    public static final int EXPECTED_RAW_BYTES = (IMG_WIDTH * IMG_HEIGHT) / 2;

    public FingerprintImage decode(byte[] raw4bit) throws Exception {
        if (raw4bit == null || raw4bit.length != EXPECTED_RAW_BYTES) {
            throw new IllegalArgumentException(
                "Incomplete fingerprint image: expected " + EXPECTED_RAW_BYTES
                + " bytes, got " + (raw4bit == null ? 0 : raw4bit.length));
        }
        int limit = EXPECTED_RAW_BYTES;

        byte[] unpacked = new byte[IMG_WIDTH * IMG_HEIGHT];
        for (int i = 0; i < limit; i++) {
            unpacked[i * 2]     = (byte) (raw4bit[i] & 0xF0);
            unpacked[i * 2 + 1] = (byte) ((raw4bit[i] & 0x0F) << 4);
        }

        BufferedImage img = new BufferedImage(IMG_WIDTH, IMG_HEIGHT, BufferedImage.TYPE_BYTE_GRAY);
        byte[] imgBuf = ((DataBufferByte) img.getRaster().getDataBuffer()).getData();
        System.arraycopy(unpacked, 0, imgBuf, 0, Math.min(unpacked.length, imgBuf.length));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", baos);
        return new FingerprintImage(baos.toByteArray());
    }
}
