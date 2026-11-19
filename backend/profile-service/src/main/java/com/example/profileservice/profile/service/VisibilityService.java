package com.example.profileservice.profile.service;

import com.example.profileservice.common.domain.Visibility;
import com.example.profileservice.profile.api.dto.PendingReviewDto;
import com.example.profileservice.profile.api.dto.StaffEditSelfDeclaredRequest;
import com.example.profileservice.profile.api.dto.StaffEditSelfDeclaredResponse;
import com.example.profileservice.profile.api.dto.StaffPreviewProfileDto;
import com.example.profileservice.profile.domain.CandidateProfile;
import com.example.profileservice.profile.domain.ProfileEdit;
import com.example.profileservice.profile.domain.ProfileReview;
import com.example.profileservice.profile.infrastructure.CandidateProfileRepository;
import com.example.profileservice.profile.infrastructure.ProfileEditRepository;
import com.example.profileservice.profile.infrastructure.ProfileReviewRepository;
import com.example.profileservice.profile.service.SkillProfileService;
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
    private final ProfileEditRepository profileEditRepo;
    private final ProfileService profileService;
    private final SkillProfileService skillProfileService;

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
        requireProfile(userId); // verify profile exists
        return reviewRepo.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * Danh sách hồ sơ theo trạng thái cho màn cán bộ, kèm thời điểm gửi duyệt gần nhất.
     * Hỗ trợ lọc theo từ khóa tìm kiếm (q), mã lớp (classCode).
     */
    @Transactional(readOnly = true)
    public Page<PendingReviewDto> listByStatus(Visibility status, String q, String classCode, Pageable pageable) {
        Page<CandidateProfile> page = profileRepo.findByVisibilityAndFilters(status, q, classCode, pageable);
        if (page.isEmpty()) {
            return page.map(profile -> toPendingReviewDto(profile, null, 0));
        }

        List<UUID> userIds = page.getContent().stream().map(CandidateProfile::getUserId).toList();
        Map<UUID, OffsetDateTime> submittedAt = reviewRepo
                .findByUserIdInAndActionOrderByCreatedAtDesc(userIds, ProfileReview.Action.SUBMIT)
                .stream()
                .collect(Collectors.toMap(
                        ProfileReview::getUserId,
                        ProfileReview::getCreatedAt,
                        (newer, older) -> newer));

        // Count rejections per user
        Map<UUID, Long> rejectionCounts = reviewRepo
                .findByUserIdInAndActionOrderByCreatedAtDesc(userIds, ProfileReview.Action.REJECT)
                .stream()
                .collect(Collectors.groupingBy(
                        ProfileReview::getUserId,
                        Collectors.counting()));

        return page.map(profile -> toPendingReviewDto(
                profile,
                submittedAt.get(profile.getUserId()),
                rejectionCounts.getOrDefault(profile.getUserId(), 0L).intValue()));
    }

    /**
     * Lấy preview hồ sơ đầy đủ cho staff xem (che email/phone theo UT-03).
     */
    @Transactional(readOnly = true)
    public StaffPreviewProfileDto getPreviewProfile(UUID userId) {
        CandidateProfile profile = requireProfile(userId);

        // Count rejections
        long rejectionCount = reviewRepo.countByUserIdAndAction(userId, ProfileReview.Action.REJECT);

        // Get review history
        List<StaffPreviewProfileDto.ReviewHistoryDto> reviewHistory = reviewRepo
                .findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(r -> StaffPreviewProfileDto.ReviewHistoryDto.builder()
                        .id(r.getId())
                        .action(r.getAction().name())
                        .reason(r.getReason())
                        .actorId(r.getActorId())
                        .createdAt(r.getCreatedAt())
                        .build())
                .toList();

        // Get skills from ProfileService (reuse existing logic)
        // We'll delegate to ProfileService.getProfileForPreview
        return profileService.getProfileForPreview(userId, rejectionCount, reviewHistory);
    }

    /**
     * Sửa phần tự khai (headline, summary, self_skills) bởi staff.
     * Ghi audit log vào profile_edits.
     */
    @Transactional
    public StaffEditSelfDeclaredResponse editSelfDeclared(UUID userId, UUID staffId, StaffEditSelfDeclaredRequest request) {
        CandidateProfile profile = requireProfileForUpdate(userId);

        String oldValue;
        String newValue = request.getValue();
        StaffEditSelfDeclaredRequest.Field field = request.getField();

        switch (field) {
            case HEADLINE -> {
                oldValue = profile.getHeadline();
                profile.setHeadline(newValue);
            }
            case SUMMARY -> {
                oldValue = profile.getSummary();
                profile.setSummary(newValue);
            }
            case SELF_SKILLS -> {
                // For self_skills, value should be JSON array of {skillId, selfScore}
                // Delegate to SkillProfileService.upsertSelfSkillsFromJson
                oldValue = "[]"; // placeholder, actual old skills fetched from DB
                skillProfileService.upsertSelfSkillsFromJson(userId, newValue);
                profileRepo.save(profile);
                return StaffEditSelfDeclaredResponse.builder()
                        .userId(userId)
                        .field(field)
                        .oldValue(oldValue)
                        .newValue(newValue)
                        .build();
            }
            default -> throw new IllegalArgumentException("Trường không được phép sửa: " + field);
        }

        profileRepo.save(profile);

        // Log to profile_edits
        ProfileEdit edit = ProfileEdit.builder()
                .userId(userId)
                .field(field.name())
                .oldValue(oldValue)
                .newValue(newValue)
                .reason(request.getReason())
                .staffId(staffId)
                .build();
        profileEditRepo.save(edit);

        return StaffEditSelfDeclaredResponse.builder()
                .userId(userId)
                .field(field)
                .oldValue(oldValue)
                .newValue(newValue)
                .build();
    }

    private PendingReviewDto toPendingReviewDto(CandidateProfile profile, OffsetDateTime submittedAt, int rejectionCount) {
        return PendingReviewDto.builder()
                .userId(profile.getUserId())
                .fullName(profile.getFullName())
                .email(profile.getEmail())
                .headline(profile.getHeadline())
                .avatarUrl(profile.getAvatarUrl())
                .submittedAt(submittedAt)
                .classCode(profile.getClassCode())
                .rejectionCount(rejectionCount)
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