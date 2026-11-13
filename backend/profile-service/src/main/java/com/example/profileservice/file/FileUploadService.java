package com.example.profileservice.file;

import org.springframework.beans.factory.annotation.Autowired;
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
    private final UploadedFileRepository uploadedFileRepository;

    @Autowired
    public FileUploadService(@Value("${app.upload.dir:uploads}") String uploadDir,
                           UploadedFileRepository uploadedFileRepository) {
       this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
       this.uploadedFileRepository = uploadedFileRepository;
    }

    public FileUploadService(@Value("${app.upload.dir:uploads}") String uploadDir) {
       this(uploadDir, null);
    }

    /**
     * Validates the upload and writes it under a generated name.
     *
     * @return the URL the stored file is reachable at
     */
    public String store(MultipartFile file) {
       return store(file, null);
    }

    public String store(MultipartFile file, UUID ownerUserId) {
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

       if (uploadedFileRepository != null && ownerUserId != null) {
           uploadedFileRepository.save(new UploadedFile(ownerUserId, storedName, file.getOriginalFilename()));
       }

       return URL_PREFIX + storedName;
    }

    public Path resolve(String storedName) {
       if (storedName == null || !storedName.matches("[0-9a-fA-F-]{36}\\.(pdf|jpg|png)")) {
           throw new InvalidFileException("Tên tệp không hợp lệ");
       }
       Path resolved = uploadDir.resolve(storedName).normalize();
       if (!resolved.startsWith(uploadDir)) {
           throw new InvalidFileException("Tên tệp không hợp lệ");
       }
       if (!Files.exists(resolved)) {
           throw new ResourceNotFoundException("Tệp không tồn tại");
       }
       return resolved;
    }

    public Path resolveForOwner(String storedName, UUID requesterId) {
       Path resolved = resolve(storedName);
       if (uploadedFileRepository == null) {
           return resolved;
       }
       UploadedFile uploadedFile = uploadedFileRepository.findByStoredName(storedName)
               .orElse(null);
       if (uploadedFile == null) {
           throw new ResourceNotFoundException("Tệp không tồn tại");
       }
       if (!uploadedFile.getUserId().equals(requesterId)) {
           throw new ResourceNotFoundException("Tệp không tồn tại");
       }
       return resolved;
    }
}
