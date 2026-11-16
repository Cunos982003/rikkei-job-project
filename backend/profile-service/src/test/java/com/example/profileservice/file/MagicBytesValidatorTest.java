package com.example.profileservice.file;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class MagicBytesValidatorTest {

    @Test
    void detects_pdf_from_percent_pdf_header() {
        byte[] content = "%PDF-1.7\n...".getBytes(StandardCharsets.US_ASCII);

        Optional<MagicBytesValidator.FileType> detected = MagicBytesValidator.detect(content);

        assertThat(detected).contains(MagicBytesValidator.FileType.PDF);
    }

    @Test
    void detects_jpg_from_ff_d8_ff_header() {
        byte[] content = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10};

        Optional<MagicBytesValidator.FileType> detected = MagicBytesValidator.detect(content);

        assertThat(detected).contains(MagicBytesValidator.FileType.JPG);
    }

    @Test
    void detects_png_from_8_byte_signature() {
        byte[] content = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00};

        Optional<MagicBytesValidator.FileType> detected = MagicBytesValidator.detect(content);

        assertThat(detected).contains(MagicBytesValidator.FileType.PNG);
    }

    @Test
    void rejects_windows_executable_header() {
        // "MZ" - DOS/PE executable header
        byte[] content = new byte[]{0x4D, 0x5A, (byte) 0x90, 0x00, 0x03, 0x00, 0x00, 0x00};

        assertThat(MagicBytesValidator.detect(content)).isEmpty();
    }

    @Test
    void rejects_plain_text_content() {
        byte[] content = "This is not a document".getBytes(StandardCharsets.UTF_8);

        assertThat(MagicBytesValidator.detect(content)).isEmpty();
    }

    @Test
    void rejects_short_content_that_cannot_hold_a_signature() {
        byte[] content = new byte[]{(byte) 0x89, 0x50};

        assertThat(MagicBytesValidator.detect(content)).isEmpty();
    }

    @Test
    void rejects_null_content() {
        assertThat(MagicBytesValidator.detect(null)).isEmpty();
    }

    @Test
    void rejects_empty_content() {
        assertThat(MagicBytesValidator.detect(new byte[0])).isEmpty();
    }
}
