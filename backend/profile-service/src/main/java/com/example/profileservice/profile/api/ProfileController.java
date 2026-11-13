package com.example.profileservice.profile.api;

import com.example.profileservice.profile.api.dto.SelfSkillDto;
import com.example.profileservice.profile.api.dto.UpdateProfileRequest;
import com.example.profileservice.profile.domain.CandidateProfile;
import com.example.profileservice.profile.service.ProfileService;
import com.example.profileservice.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
            @AuthenticationPrincipal UserPrincipal principal) {
        UUID userId = principal.getUserId();
        CandidateProfile profile = profileService.getOrCreateProfile(userId);
        return ResponseEntity.ok(profile);
    }

    @PutMapping("/me")
    public ResponseEntity<CandidateProfile> updateMyProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody UpdateProfileRequest update) {
        UUID userId = principal.getUserId();
        CandidateProfile updated = profileService.updateProfile(userId, update);
        return ResponseEntity.ok(updated);
    }

    @PutMapping("/me/self-skills")
    public ResponseEntity<Void> upsertSelfSkills(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody List<SelfSkillDto> skills) {
        UUID userId = principal.getUserId();
        profileService.upsertSelfSkills(userId, skills);
        return ResponseEntity.ok().build();
    }
}