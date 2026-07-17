package com.example.attendance.domain.biometric;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FingerprintImageDecoderTest {
    private final FingerprintImageDecoder decoder = new FingerprintImageDecoder();

    @Test
    void rejectsShortImage() {
        assertThrows(IllegalArgumentException.class, () -> decoder.decode(new byte[100]));
    }

    @Test
    void rejectsOversizeImage() {
        assertThrows(IllegalArgumentException.class,
                () -> decoder.decode(new byte[FingerprintImageDecoder.EXPECTED_RAW_BYTES + 1]));
    }

    @Test
    void rejectsNull() {
        assertThrows(IllegalArgumentException.class, () -> decoder.decode(null));
    }

    @Test
    void acceptsExactSize() throws Exception {
        assertNotNull(decoder.decode(new byte[FingerprintImageDecoder.EXPECTED_RAW_BYTES]));
    }
}
