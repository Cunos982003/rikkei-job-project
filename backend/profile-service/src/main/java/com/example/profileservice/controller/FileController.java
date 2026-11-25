package com.example.profileservice.controller;

import com.example.profileservice.common.ApiResponse;
import com.example.profileservice.exception.InvalidFileException;
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

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileController {

    private final FileUploadService fileUploadService;
    private final MinioFileUploadService minioFileUploadService;

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
