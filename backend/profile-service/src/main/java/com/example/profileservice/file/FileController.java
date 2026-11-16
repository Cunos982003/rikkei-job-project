package com.example.profileservice.file;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileController {

    private final FileUploadService fileUploadService;

    public record FileUploadResponse(String url) {}

    @PostMapping
    public ResponseEntity<FileUploadResponse> upload(
            @RequestParam(value = "file", required = false) MultipartFile file) {
        if (file == null) {
            throw new InvalidFileException("Thiếu tệp tải lên");
        }
        return ResponseEntity.ok(new FileUploadResponse(fileUploadService.store(file)));
    }

    @GetMapping("/{storedName}")
    public ResponseEntity<Resource> download(@PathVariable String storedName) {
        Path path = fileUploadService.resolve(storedName);
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
