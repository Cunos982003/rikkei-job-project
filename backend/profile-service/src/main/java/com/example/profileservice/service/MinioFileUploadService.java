package com.example.profileservice.service;

import com.example.profileservice.exception.InvalidFileException;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.util.UUID;

@Service
public class MinioFileUploadService {

    public static final long MAX_SIZE_BYTES = 5L * 1024 * 1024;

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

        return "/files/" + storedName;
    }

    public void delete(String storedName) {
        if (storedName == null) {
            throw new InvalidFileException("Tên tệp không hợp lệ");
        }
        // Remove /files/ prefix if present
        String objectName = storedName.startsWith("/files/") ? storedName.substring(7) : storedName;
        // Validate the object name (UUID + extension)
        if (!objectName.matches("[0-9a-fA-F-]{36}\\.(jpg|png)")) {
            throw new InvalidFileException("Tên tệp không hợp lệ");
        }
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectName)
                            .build());
        } catch (Exception e) {
            throw new InvalidFileException("Không thể xóa tệp: " + e.getMessage());
        }
    }
}