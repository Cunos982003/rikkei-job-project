package com.example.profileservice.controller;

import com.example.profileservice.common.ApiResponse;
import com.example.profileservice.service.CVService;
import com.example.profileservice.service.FileUploadService;
import com.example.profileservice.service.MinioFileUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileController {

    private final FileUploadService fileUploadService;
    private final MinioFileUploadService minioFileUploadService;
    private final CVService cvService;

    public record FileUploadResponse(String url) {}

    @PostMapping
    public ResponseEntity<ApiResponse<FileUploadResponse>> upload(
            @RequestParam(value = "file", required = false) MultipartFile file) {
        if (file == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(HttpStatus.BAD_REQUEST, "Thiếu tệp tải lên"));
        }
        String url = fileUploadService.store(file);
        return ResponseEntity.ok(ApiResponse.success("Tải tệp lên thành công", new FileUploadResponse(url)));
    }

    @GetMapping("/{storedName}")
    public ResponseEntity<Resource> download(@PathVariable String storedName) {
        Path path = fileUploadService.resolve(storedName);
        return ResponseEntity.ok()
                .contentType(mediaTypeFor(storedName))
                .body(new FileSystemResource(path));
    }

    @GetMapping("/minio/{storedName:.+}")
    public ResponseEntity<Resource> downloadFromMinio(
            @PathVariable String storedName,
            @RequestHeader(value = "X-User-Id", required = false) Object userIdHeader) {
        // Check authorization for CV PDFs (stored as "cvs/{uuid}.pdf")
        if (storedName.startsWith("cvs/") && storedName.endsWith(".pdf")) {
            UUID userId = resolveUserId(userIdHeader);
            String cvIdStr = storedName.substring(4, storedName.length() - 4); // Remove "cvs/" and ".pdf"
            UUID cvId;
            try {
                cvId = UUID.fromString(cvIdStr);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }

            // Check if user has access to this CV PDF
            if (!cvService.canAccessCVPdf(userId, cvId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        }

        InputStream inputStream = minioFileUploadService.getObject("/files/" + storedName);
        return ResponseEntity.ok()
                .contentType(mediaTypeFor(storedName))
                .body(new InputStreamResource(inputStream));
    }

    private UUID resolveUserId(Object headerUserId) {
        if (headerUserId instanceof UUID uuid) {
            return uuid;
        }
        if (headerUserId instanceof String str && !str.isBlank()) {
            try {
                return UUID.fromString(str);
            } catch (Exception ignored) {}
        }
        return null;
    }

    private static MediaType mediaTypeFor(String storedName) {
        if (storedName.endsWith(".pdf")) {
            return MediaType.APPLICATION_PDF;
        }
        if (storedName.endsWith(".png")) {
            return MediaType.IMAGE_PNG;
        }
        return MediaType.IMAGE_JPEG;
    }
}
