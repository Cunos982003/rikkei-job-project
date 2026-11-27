package com.example.profileservice.service;

import com.example.profileservice.exception.InvalidFileException;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.util.UUID;

@Service
public class MinioFileUploadService {

    public static final long MAX_SIZE_BYTES = 5L * 1024 * 1024;

    private static final Logger log = LoggerFactory.getLogger(MinioFileUploadService.class);

    private final MinioClient minioClient;
    private final String bucket;

    public MinioFileUploadService(MinioClient minioClient,
                                  @Value("${app.minio.bucket}") String bucket) {
        this.minioClient = minioClient;
        this.bucket = bucket;
    }

    /**
     * Validates the upload and writes it to MinIO.
     *
     * @return the URL the stored file is reachable at
     */
    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("Tệp tải lên rỗng");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new InvalidFileException("Tệp vượt quá dung lượng cho phép (tối đa 5MB)");
        }

        byte[] content;
        try {
            content = file.getBytes();
        } catch (Exception e) {
            throw new InvalidFileException("Không thể đọc nội dung tệp tải lên");
        }

        MagicBytesValidator.FileType type = MagicBytesValidator.detect(content)
                .orElseThrow(() -> new InvalidFileException(
                        "Định dạng tệp không được hỗ trợ. Chỉ chấp nhận JPG, PNG"));

        // Only allow images for portfolio media
        if (type != MagicBytesValidator.FileType.JPG && type != MagicBytesValidator.FileType.PNG) {
            throw new InvalidFileException("Chỉ chấp nhận JPG, PNG cho ảnh portfolio");
        }

        String storedName = UUID.randomUUID() + "." + type.extension();
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(storedName)
                            .stream(new ByteArrayInputStream(content), content.length, -1)
                            .contentType(type.contentType())
                            .build());
        } catch (Exception e) {
            throw new InvalidFileException("Không thể lưu tệp tải lên: " + e.getMessage());
        }

        return "/api/v1/files/minio/" + storedName;
    }

    public void delete(String storedName) {
        if (storedName == null) {
            throw new InvalidFileException("Tên tệp không hợp lệ");
        }
        // Remove /files/ prefix if present
        String objectName = storedName.startsWith("/files/") ? storedName.substring(7) : storedName;
        // Remove /api/v1/files/minio/ prefix if present
        objectName = objectName.startsWith("/api/v1/files/minio/") ? objectName.substring(19) : objectName;
        // Remove leading / if present (for URLs like "/uuid.png")
        objectName = objectName.startsWith("/") ? objectName.substring(1) : objectName;

        log.debug("Deleting file from MinIO. Input: {}, After stripping prefixes: {}", storedName, objectName);

        // Validate the object name (UUID + extension) - accept both 32-char (no hyphens) and 36-char (with hyphens)
        if (!objectName.matches("([0-9a-fA-F]{32}|[0-9a-fA-F-]{36})\\.(jpg|png)")) {
            log.warn("Invalid filename format: {}", objectName);
            throw new InvalidFileException("Tên tệp không hợp lệ: " + objectName);
        }
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectName)
                            .build());
        } catch (Exception e) {
            log.error("Failed to delete file from MinIO: {}", objectName, e);
            throw new InvalidFileException("Không thể xóa tệp: " + e.getMessage());
        }
    }

    /**
     * Store a file from InputStream with explicit content type.
     * @param objectName the object name/path in MinIO
     * @param inputStream the file content
     * @param contentType the MIME type
     * @return the URL the stored file is reachable at
     */
    public String store(String objectName, java.io.InputStream inputStream, String contentType) {
        if (objectName == null || objectName.isBlank()) {
            throw new InvalidFileException("Tên tệp không hợp lệ");
        }
        if (inputStream == null) {
            throw new InvalidFileException("Nội dung tệp rỗng");
        }

        byte[] content;
        try {
            content = inputStream.readAllBytes();
        } catch (Exception e) {
            throw new InvalidFileException("Không thể đọc nội dung tệp: " + e.getMessage());
        }

        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectName)
                            .stream(new ByteArrayInputStream(content), content.length, -1)
                            .contentType(contentType)
                            .build());
        } catch (Exception e) {
            throw new InvalidFileException("Không thể lưu tệp: " + e.getMessage());
        }

        return "/api/v1/files/minio/" + objectName;
    }

    /**
     * Get an InputStream for a file stored in MinIO.
     * @param storedName the stored name (e.g., "/files/{uuid}.jpg", "/api/v1/files/minio/{uuid}.jpg", or "cvs/{uuid}.pdf")
     * @return InputStream of the file
     */
    public java.io.InputStream getObject(String storedName) {
        if (storedName == null) {
            throw new InvalidFileException("Tên tệp không hợp lệ");
        }
        // Remove /files/ prefix if present
        String objectName = storedName.startsWith("/files/") ? storedName.substring(7) : storedName;
        // Remove /api/v1/files/minio/ prefix if present
        objectName = objectName.startsWith("/api/v1/files/minio/") ? objectName.substring(19) : objectName;
        // Remove leading / if present (for URLs like "/uuid.png")
        objectName = objectName.startsWith("/") ? objectName.substring(1) : objectName;

        log.debug("Getting file from MinIO. Input: {}, After stripping prefixes: {}", storedName, objectName);

        // Validate the object name:
        // - Portfolio images: UUID + .jpg|.png (32-char no hyphens or 36-char with hyphens)
        // - CV PDFs: cvs/ + UUID + .pdf (36-char with hyphens)
        if (!objectName.matches("([0-9a-fA-F]{32}|[0-9a-fA-F-]{36})\\.(jpg|png)") &&
            !objectName.matches("cvs/[0-9a-fA-F-]{36}\\.pdf")) {
            log.warn("Invalid filename format: {}", objectName);
            throw new InvalidFileException("Tên tệp không hợp lệ: " + objectName);
        }
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectName)
                            .build());
        } catch (Exception e) {
            log.error("Failed to get file from MinIO: {}", objectName, e);
            throw new InvalidFileException("Không thể đọc tệp: " + e.getMessage());
        }
    }
}