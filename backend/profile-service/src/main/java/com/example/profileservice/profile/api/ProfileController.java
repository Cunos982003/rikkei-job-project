package com.example.profileservice.profile.api;

import com.example.profileservice.profile.api.dto.SelfSkillDto;
import com.example.profileservice.profile.domain.CandidateProfile;
import com.example.profileservice.profile.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/profiles")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping("/me")
    public ResponseEntity<CandidateProfile> getMyProfile(
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        CandidateProfile profile = profileService.getOrCreateProfile(userId);
        return ResponseEntity.ok(profile);
    }

    @PutMapping("/me")
    public ResponseEntity<CandidateProfile> updateMyProfile(
            @RequestHeader(value = "X-User-Id", required = false) UUID userId,
            @RequestBody CandidateProfile update) {
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        CandidateProfile updated = profileService.updateProfile(userId, update);
        return ResponseEntity.ok(updated);
    }

    @PutMapping("/me/self-skills")
    public ResponseEntity<Void> upsertSelfSkills(
            @RequestHeader(value = "X-User-Id", required = false) UUID userId,
            @RequestBody List<SelfSkillDto> skills) {
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        profileService.upsertSelfSkills(userId, skills);
        return ResponseEntity.ok().build();
    }
}