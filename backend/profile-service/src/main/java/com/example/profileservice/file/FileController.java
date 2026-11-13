package com.example.profileservice.file;

import com.example.profileservice.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileController {

    private final FileUploadService fileUploadService;

    public record FileUploadResponse(String url) {}

    @PostMapping
    public ResponseEntity<FileUploadResponse> upload(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(value = "file", required = false) MultipartFile file) {
        if (principal == null) {
            throw new AccessDeniedException("Authentication required");
        }
        if (file == null) {
            throw new InvalidFileException("Thiếu tệp tải lên");
        }
        return ResponseEntity.ok(new FileUploadResponse(fileUploadService.store(file, principal.getUserId())));
    }

    @GetMapping("/{storedName}")
    public ResponseEntity<Resource> download(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String storedName) {
        if (principal == null) {
            throw new AccessDeniedException("Authentication required");
        }
        UUID requesterId = principal.getUserId();
        Path path = fileUploadService.resolveForOwner(storedName, requesterId);
        return ResponseEntity.ok()
                .contentType(mediaTypeFor(storedName))
                .body(new FileSystemResource(path));
    }

    @ExceptionHandler(InvalidFileException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidFile(InvalidFileException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "statusCode", HttpStatus.BAD_REQUEST.value(),
                "message", ex.getMessage()));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleResourceNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "statusCode", HttpStatus.NOT_FOUND.value(),
                "message", ex.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "statusCode", HttpStatus.UNAUTHORIZED.value(),
                "message", "Authentication required"));
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
