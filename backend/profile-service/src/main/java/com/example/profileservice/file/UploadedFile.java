package com.example.profileservice.file;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "uploaded_files", uniqueConstraints = {
        @UniqueConstraint(name = "uk_uploaded_files_stored_name", columnNames = "stored_name")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UploadedFile {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "user_id", nullable = false, columnDefinition = "uuid")
    private UUID userId;

    @Column(name = "stored_name", nullable = false, length = 255)
    private String storedName;

    @Column(name = "original_name", length = 255)
    private String originalName;

    public UploadedFile(UUID userId, String storedName, String originalName) {
        this.userId = userId;
        this.storedName = storedName;
        this.originalName = originalName;
    }
}
