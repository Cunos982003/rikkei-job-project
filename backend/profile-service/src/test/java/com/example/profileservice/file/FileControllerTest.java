package com.example.profileservice.file;

import com.example.profileservice.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FileControllerTest {

    @TempDir
    Path uploadDir;

    MockMvc mockMvc;

    private static final UUID USER_ID = UUID.randomUUID();

    @BeforeEach
    void setup() {
        Map<String, UploadedFile> files = new HashMap<>();
        UploadedFileRepository repository = (UploadedFileRepository) Proxy.newProxyInstance(
                UploadedFileRepository.class.getClassLoader(),
                new Class<?>[]{UploadedFileRepository.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "findByStoredName":
                            return Optional.ofNullable(files.get(args[0]));
                        case "save":
                            UploadedFile uploaded = (UploadedFile) args[0];
                            files.put(uploaded.getStoredName(), uploaded);
                            return uploaded;
                        case "findById":
                            return Optional.empty();
                        case "findAll":
                            return java.util.Collections.emptyList();
                        case "count":
                            return 0L;
                        case "existsById":
                            return false;
                        default:
                            return null;
                    }
                });
        FileUploadService service = new FileUploadService(uploadDir.toString(), repository);
        mockMvc = MockMvcBuilders.standaloneSetup(new FileController(service))
                .setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
                    @Override
                    public boolean supportsParameter(MethodParameter parameter) {
                        return parameter.hasParameterAnnotation(AuthenticationPrincipal.class)
                                || UserPrincipal.class.isAssignableFrom(parameter.getParameterType());
                    }

                    @Override
                    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                        Principal p = webRequest.getUserPrincipal();
                        if (p instanceof Authentication auth && auth.getPrincipal() instanceof UserPrincipal up) {
                            return up;
                        }
                        if (p instanceof UserPrincipal up) {
                            return up;
                        }
                        return null;
                    }
                })
                .build();
    }

    private static byte[] pdfOfSize(int size) {
        byte[] content = new byte[size];
        System.arraycopy(new byte[]{0x25, 0x50, 0x44, 0x46}, 0, content, 0, 4);
        return content;
    }

    private static UsernamePasswordAuthenticationToken principalFor(UUID userId) {
        return new UsernamePasswordAuthenticationToken(
                UserPrincipal.of(userId, "candidate", "CANDIDATE"), "token",
                UserPrincipal.of(userId, "candidate", "CANDIDATE").getAuthorities());
    }

    @Test
    void post_files_returns_url_for_valid_pdf() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "certificate.pdf", "application/pdf", pdfOfSize(1024));

        mockMvc.perform(multipart("/api/v1/files")
                        .file(file)
                        .principal(principalFor(USER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value(org.hamcrest.Matchers.startsWith("/files/")));
    }

    @Test
    void post_files_returns_400_when_too_large() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "certificate.pdf", "application/pdf", pdfOfSize(6 * 1024 * 1024));

        mockMvc.perform(multipart("/api/v1/files")
                        .file(file)
                        .principal(principalFor(USER_ID)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void post_files_returns_400_when_content_is_not_an_allowed_type() throws Exception {
        byte[] peHeader = new byte[]{0x4D, 0x5A, (byte) 0x90, 0x00, 0x03, 0x00, 0x00, 0x00};
        MockMultipartFile file = new MockMultipartFile(
                "file", "resume.pdf", "application/pdf", peHeader);

        mockMvc.perform(multipart("/api/v1/files")
                        .file(file)
                        .principal(principalFor(USER_ID)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void post_files_returns_400_when_no_file_part() throws Exception {
        mockMvc.perform(multipart("/api/v1/files")
                        .principal(principalFor(USER_ID)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void get_files_returns_stored_content_with_pdf_content_type() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "certificate.pdf", "application/pdf", pdfOfSize(512));
        String body = mockMvc.perform(multipart("/api/v1/files")
                        .file(file)
                        .principal(principalFor(USER_ID)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String storedName = body.replaceAll(".*\"/files/([^\"]+)\".*", "$1");

        mockMvc.perform(get("/api/v1/files/" + storedName)
                        .principal(principalFor(USER_ID)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(content().bytes(pdfOfSize(512)));
    }

    @Test
    void get_files_returns_400_when_stored_name_is_not_a_generated_uuid() throws Exception {
        mockMvc.perform(get("/api/v1/files/not-a-uuid.pdf")
                        .principal(principalFor(USER_ID)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void get_files_returns_400_when_stored_name_escapes_the_upload_directory() throws Exception {
        mockMvc.perform(get("/api/v1/files/..%2F..%2Fetc%2Fpasswd")
                        .principal(principalFor(USER_ID)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void get_files_returns_404_when_file_does_not_exist() throws Exception {
        mockMvc.perform(get("/api/v1/files/00000000-0000-0000-0000-000000000000.pdf")
                        .principal(principalFor(USER_ID)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").exists());
    }
}
