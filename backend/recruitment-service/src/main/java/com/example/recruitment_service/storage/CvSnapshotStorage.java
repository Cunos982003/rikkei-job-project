package com.example.recruitment_service.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CvSnapshotStorage {
    private final Path uploadDirectory;
    private final String publicBaseUrl;

    public CvSnapshotStorage(
            @Value("${app.cv.snapshot-directory:uploads/recruitments}") String directory,
            @Value("${app.public-base-url:}") String publicBaseUrl) {
        this.uploadDirectory = Path.of(directory).toAbsolutePath().normalize();
        this.publicBaseUrl = publicBaseUrl == null ? "" : publicBaseUrl.replaceAll("/+$", "");
    }

    public String store(UUID recruitmentId, byte[] pdf) {
        try {
            Files.createDirectories(uploadDirectory);
            Path temp = Files.createTempFile(uploadDirectory, recruitmentId.toString(), ".tmp");
            Path target = resolve(recruitmentId);
            try {
                Files.write(temp, pdf);
                Files.move(temp, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            } finally {
                Files.deleteIfExists(temp);
            }
            return publicBaseUrl + "/recruitments/" + recruitmentId + "/cv-snapshot";
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to persist the CV snapshot", exception);
        }
    }

    public byte[] read(UUID recruitmentId) {
        try {
            return Files.readAllBytes(resolve(recruitmentId));
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read the CV snapshot", exception);
        }
    }

    public void delete(UUID recruitmentId) {
        try {
            Files.deleteIfExists(resolve(recruitmentId));
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to remove the CV snapshot", exception);
        }
    }

    private Path resolve(UUID recruitmentId) {
        return uploadDirectory.resolve(recruitmentId + ".pdf").normalize();
    }
}
