package com.example.profileservice.file;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class FileUploadService {

    public static final long MAX_SIZE_BYTES = 5L * 1024 * 1024;
    public static final String URL_PREFIX = "/files/";

    private final Path uploadDir;

    public FileUploadService(@Value("${app.upload.dir:uploads}") String uploadDir) {
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    /**
     * Validates the upload and writes it under a generated name.
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
        } catch (IOException e) {
            throw new InvalidFileException("Không thể đọc nội dung tệp tải lên");
        }

        MagicBytesValidator.FileType type = MagicBytesValidator.detect(content)
                .orElseThrow(() -> new InvalidFileException(
                        "Định dạng tệp không được hỗ trợ. Chỉ chấp nhận PDF, JPG, PNG"));

        String storedName = UUID.randomUUID() + "." + type.extension();
        try {
            Files.createDirectories(uploadDir);
            Files.write(uploadDir.resolve(storedName), content);
        } catch (IOException e) {
            throw new InvalidFileException("Không thể lưu tệp tải lên");
        }

        return URL_PREFIX + storedName;
    }

    public Path resolve(String storedName) {
        if (storedName == null || !storedName.matches("[0-9a-fA-F-]{36}\\.(pdf|jpg|png)")) {
            throw new InvalidFileException("Tên tệp không hợp lệ");
        }
        Path resolved = uploadDir.resolve(storedName).normalize();
        if (!resolved.startsWith(uploadDir) || !Files.exists(resolved)) {
            throw new InvalidFileException("Không tìm thấy tệp");
        }
        return resolved;
    }
}
