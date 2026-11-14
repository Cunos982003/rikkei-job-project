package com.example.profileservice.profile.service;

import com.example.profileservice.profile.api.dto.PendingReviewDto;
import com.example.profileservice.profile.domain.CandidateProfile;
import com.example.profileservice.profile.domain.CandidateProfile.Visibility;
import com.example.profileservice.profile.domain.ProfileReview;
import com.example.profileservice.profile.infrastructure.CandidateProfileRepository;
import com.example.profileservice.profile.infrastructure.ProfileReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VisibilityService {

    private final CandidateProfileRepository profileRepo;
    private final ProfileReviewRepository reviewRepo;

    @Transactional
    public void submitForReview(UUID userId, boolean consent) {
        CandidateProfile profile = requireProfileForUpdate(userId);
        profile.submitForReview(consent, userId);
        profileRepo.save(profile);
    }

    @Transactional
    public void approve(UUID userId, UUID actorId) {
        CandidateProfile profile = requireProfileForUpdate(userId);
        profile.approve(actorId);
        profileRepo.save(profile);
    }

    @Transactional
    public void reject(UUID userId, UUID actorId, String reason) {
        CandidateProfile profile = requireProfileForUpdate(userId);
        profile.reject(actorId, reason);
        profileRepo.save(profile);
    }

    @Transactional
    public void withdraw(UUID userId) {
        CandidateProfile profile = requireProfileForUpdate(userId);
        profile.withdraw(userId);
        profileRepo.save(profile);
    }

    @Transactional(readOnly = true)
    public List<ProfileReview> getReviewHistory(UUID userId) {
        CandidateProfile profile = requireProfile(userId);
        return profile.getReviewHistory();
    }

    /**
     * Danh sách hồ sơ theo trạng thái cho màn cán bộ, kèm thời điểm gửi duyệt gần nhất.
     */
    @Transactional(readOnly = true)
    public Page<PendingReviewDto> listByStatus(Visibility status, Pageable pageable) {
        Page<CandidateProfile> page = profileRepo.findByVisibility(status, pageable);
        if (page.isEmpty()) {
            return page.map(profile -> toPendingReviewDto(profile, null));
        }

        List<UUID> userIds = page.getContent().stream().map(CandidateProfile::getUserId).toList();
        Map<UUID, OffsetDateTime> submittedAt = reviewRepo
                .findByUserIdInAndActionOrderByCreatedAtDesc(userIds, ProfileReview.Action.SUBMIT)
                .stream()
                .collect(Collectors.toMap(
                        ProfileReview::getUserId,
                        ProfileReview::getCreatedAt,
                        (newer, older) -> newer));

        return page.map(profile -> toPendingReviewDto(profile, submittedAt.get(profile.getUserId())));
    }

    private PendingReviewDto toPendingReviewDto(CandidateProfile profile, OffsetDateTime submittedAt) {
        return PendingReviewDto.builder()
                .userId(profile.getUserId())
                .fullName(profile.getFullName())
                .email(profile.getEmail())
                .headline(profile.getHeadline())
                .avatarUrl(profile.getAvatarUrl())
                .submittedAt(submittedAt)
                .build();
    }

    private CandidateProfile requireProfile(UUID userId) {
        return profileRepo.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Profile not found for userId: " + userId));
    }

    private CandidateProfile requireProfileForUpdate(UUID userId) {
        return profileRepo.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new IllegalArgumentException("Profile not found for userId: " + userId));
    }
}