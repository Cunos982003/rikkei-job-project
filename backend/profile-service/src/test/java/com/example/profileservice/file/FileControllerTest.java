package com.example.profileservice.file;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.file.Path;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FileControllerTest {

    @TempDir
    Path uploadDir;

    MockMvc mockMvc;

    @BeforeEach
    void setup() {
        FileUploadService service = new FileUploadService(uploadDir.toString());
        mockMvc = MockMvcBuilders.standaloneSetup(new FileController(service)).build();
    }

    private static byte[] pdfOfSize(int size) {
        byte[] content = new byte[size];
        System.arraycopy(new byte[]{0x25, 0x50, 0x44, 0x46}, 0, content, 0, 4);
        return content;
    }

    @Test
    void post_files_returns_url_for_valid_pdf() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "certificate.pdf", "application/pdf", pdfOfSize(1024));

        mockMvc.perform(multipart("/api/v1/files").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value(org.hamcrest.Matchers.startsWith("/files/")));
    }

    @Test
    void post_files_returns_400_when_too_large() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "certificate.pdf", "application/pdf", pdfOfSize(6 * 1024 * 1024));

        mockMvc.perform(multipart("/api/v1/files").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void post_files_returns_400_when_content_is_not_an_allowed_type() throws Exception {
        byte[] peHeader = new byte[]{0x4D, 0x5A, (byte) 0x90, 0x00, 0x03, 0x00, 0x00, 0x00};
        MockMultipartFile file = new MockMultipartFile(
                "file", "resume.pdf", "application/pdf", peHeader);

        mockMvc.perform(multipart("/api/v1/files").file(file))
                .andExpect(status().isBadRequest());
    }

    @Test
    void post_files_returns_400_when_no_file_part() throws Exception {
        mockMvc.perform(multipart("/api/v1/files"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void get_files_returns_stored_content_with_pdf_content_type() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "certificate.pdf", "application/pdf", pdfOfSize(512));
        String body = mockMvc.perform(multipart("/api/v1/files").file(file))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String storedName = body.replaceAll(".*\"/files/([^\"]+)\".*", "$1");

        mockMvc.perform(get("/api/v1/files/" + storedName))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(content().bytes(pdfOfSize(512)));
    }

    @Test
    void get_files_returns_400_when_stored_name_is_not_a_generated_uuid() throws Exception {
        mockMvc.perform(get("/api/v1/files/not-a-uuid.pdf"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void get_files_returns_400_when_stored_name_escapes_the_upload_directory() throws Exception {
        mockMvc.perform(get("/api/v1/files/..%2F..%2Fetc%2Fpasswd"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void get_files_returns_400_when_file_does_not_exist() throws Exception {
        mockMvc.perform(get("/api/v1/files/00000000-0000-0000-0000-000000000000.pdf"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }
}
