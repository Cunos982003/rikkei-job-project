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
        CandidateProfile profile = requireProfile(userId);

        if (!consent) {
            throw new IllegalArgumentException("Consent is required to submit profile for review");
        }
        if (profile.getVisibility() != Visibility.PRIVATE && profile.getVisibility() != Visibility.REJECTED) {
            throw new InvalidTransitionException(
                    "Can only submit PRIVATE or REJECTED profile for review. Current: " + profile.getVisibility());
        }

        profile.setVisibility(Visibility.PENDING);
        profile.setConsentAt(OffsetDateTime.now());
        profile.setConsentWithdrawnAt(null);
        profileRepo.save(profile);

        reviewRepo.save(ProfileReview.builder()
                .userId(userId)
                .action(ProfileReview.Action.SUBMIT)
                .actorId(userId)
                .build());
    }

    @Transactional
    public void approve(UUID userId, UUID actorId) {
        CandidateProfile profile = requireProfile(userId);

        if (profile.getVisibility() != Visibility.PENDING) {
            throw new InvalidTransitionException(
                    "Can only approve PENDING profile. Current: " + profile.getVisibility());
        }

        profile.setVisibility(Visibility.PUBLIC);
        profileRepo.save(profile);

        reviewRepo.save(ProfileReview.builder()
                .userId(userId)
                .action(ProfileReview.Action.APPROVE)
                .actorId(actorId)
                .build());
    }

    @Transactional
    public void reject(UUID userId, UUID actorId, String reason) {
        CandidateProfile profile = requireProfile(userId);

        if (profile.getVisibility() != Visibility.PENDING) {
            throw new InvalidTransitionException(
                    "Can only reject PENDING profile. Current: " + profile.getVisibility());
        }
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Rejection reason is required");
        }

        profile.setVisibility(Visibility.REJECTED);
        profileRepo.save(profile);

        reviewRepo.save(ProfileReview.builder()
                .userId(userId)
                .action(ProfileReview.Action.REJECT)
                .reason(reason)
                .actorId(actorId)
                .build());
    }

    @Transactional
    public void withdraw(UUID userId) {
        CandidateProfile profile = requireProfile(userId);

        if (profile.getVisibility() != Visibility.PENDING && profile.getVisibility() != Visibility.PUBLIC) {
            throw new InvalidTransitionException(
                    "Can only withdraw PENDING or PUBLIC profile. Current: " + profile.getVisibility());
        }

        profile.setVisibility(Visibility.PRIVATE);
        profile.setConsentWithdrawnAt(OffsetDateTime.now());
        profileRepo.save(profile);

        reviewRepo.save(ProfileReview.builder()
                .userId(userId)
                .action(ProfileReview.Action.WITHDRAW)
                .actorId(userId)
                .build());
    }

    @Transactional(readOnly = true)
    public List<ProfileReview> getReviewHistory(UUID userId) {
        return reviewRepo.findByUserIdOrderByCreatedAtDesc(userId);
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
}
