package com.example.profileservice.file;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileUploadServiceTest {

    @TempDir
    Path uploadDir;

    private FileUploadService service() {
        return new FileUploadService(uploadDir.toString());
    }

    /** Builds content of {@code size} bytes whose first 4 bytes are the PDF signature. */
    private static byte[] pdfOfSize(int size) {
        byte[] content = new byte[size];
        System.arraycopy(new byte[]{0x25, 0x50, 0x44, 0x46}, 0, content, 0, 4); // %PDF
        return content;
    }

    private static byte[] pngOfSize(int size) {
        byte[] content = new byte[size];
        System.arraycopy(new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A},
                0, content, 0, 8);
        return content;
    }

    @Test
    void uploads_pdf_of_4mb() {
        int fourMb = 4 * 1024 * 1024;
        MockMultipartFile file = new MockMultipartFile(
                "file", "certificate.pdf", "application/pdf", pdfOfSize(fourMb));

        String url = service().store(file);

        assertThat(url).startsWith("/files/").endsWith(".pdf");
        String storedName = url.substring("/files/".length());
        assertThat(Files.exists(uploadDir.resolve(storedName))).isTrue();
    }

    @Test
    void rejects_pdf_larger_than_5mb() {
        int sixMb = 6 * 1024 * 1024;
        MockMultipartFile file = new MockMultipartFile(
                "file", "certificate.pdf", "application/pdf", pdfOfSize(sixMb));

        assertThatThrownBy(() -> service().store(file))
                .isInstanceOf(InvalidFileException.class)
                .hasMessageContaining("5MB");
    }

    @Test
    void rejects_executable_renamed_to_pdf() {
        byte[] peHeader = new byte[]{0x4D, 0x5A, (byte) 0x90, 0x00, 0x03, 0x00, 0x00, 0x00, 0x04, 0x00};
        MockMultipartFile file = new MockMultipartFile(
                "file", "resume.pdf", "application/pdf", peHeader);

        assertThatThrownBy(() -> service().store(file))
                .isInstanceOf(InvalidFileException.class)
                .hasMessageContaining("PDF");
    }

    @Test
    void uploads_png() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "certificate.png", "image/png", pngOfSize(2048));

        String url = service().store(file);

        assertThat(url).startsWith("/files/").endsWith(".png");
    }

    @Test
    void rejects_unsupported_type() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "notes.txt", "text/plain", "plain text".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> service().store(file))
                .isInstanceOf(InvalidFileException.class);
    }

    @Test
    void rejects_empty_file() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "empty.pdf", "application/pdf", new byte[0]);

        assertThatThrownBy(() -> service().store(file))
                .isInstanceOf(InvalidFileException.class);
    }

    @Test
    void stored_filename_does_not_reuse_client_supplied_name() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "../../etc/passwd.pdf", "application/pdf", pdfOfSize(64));

        String url = service().store(file);

        assertThat(url).doesNotContain("..");
        assertThat(url).doesNotContain("passwd");
    }
}
