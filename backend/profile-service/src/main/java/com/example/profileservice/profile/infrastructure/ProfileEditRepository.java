package com.example.profileservice.profile.infrastructure;

import com.example.profileservice.profile.domain.ProfileEdit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProfileEditRepository extends JpaRepository<ProfileEdit, UUID> {

    List<ProfileEdit> findByUserIdOrderByCreatedAtDesc(UUID userId);
}