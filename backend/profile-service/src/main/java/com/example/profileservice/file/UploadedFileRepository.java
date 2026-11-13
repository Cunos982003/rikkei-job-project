package com.example.profileservice.file;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UploadedFileRepository extends JpaRepository<UploadedFile, UUID> {
    Optional<UploadedFile> findByStoredName(String storedName);
}
