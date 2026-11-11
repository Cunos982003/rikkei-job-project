package com.example.profileservice.profile.service;

import com.example.profileservice.common.LevelConstants;
import com.example.profileservice.profile.api.dto.LmsStudentDto;
import com.example.profileservice.profile.api.dto.SelfSkillDto;
import com.example.profileservice.profile.domain.CandidateProfile;
import com.example.profileservice.profile.infrastructure.CandidateProfileRepository;
import com.example.profileservice.profile.infrastructure.LmsClient;
import com.example.profileservice.skill.domain.Skill;
import com.example.profileservice.skill.domain.SkillEvidence;
import com.example.profileservice.skill.domain.SkillScore;
import com.example.profileservice.skill.infrastructure.SkillEvidenceRepository;
import com.example.profileservice.skill.infrastructure.SkillRepository;
import com.example.profileservice.skill.infrastructure.SkillScoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    private static final UUID TEST_USER_ID = UUID.randomUUID();
    private static final UUID SKILL_ID = UUID.randomUUID();

    @Mock
    CandidateProfileRepository candidateProfileRepository;

    @Mock
    LmsClient lmsClient;

    @Mock
    SkillEvidenceRepository skillEvidenceRepository;

    @Mock
    SkillScoreRepository skillScoreRepository;

    @Mock
    SkillRepository skillRepository;

    ProfileService profileService;

    @BeforeEach
    void setup() {
        profileService = new ProfileService(candidateProfileRepository, lmsClient, skillEvidenceRepository, skillScoreRepository, skillRepository);
    }

    @Test
    void getOrCreateProfile_fetches_from_lms_on_first_call() {
        // Given
        LmsStudentDto mockStudent = new LmsStudentDto(
                "Test User",
                "test@example.com",
                "Test Program",
                "Class 1A",
                "Java Course"
        );
        when(lmsClient.getStudent(TEST_USER_ID)).thenReturn(Mono.just(mockStudent));
        when(candidateProfileRepository.findByUserId(TEST_USER_ID)).thenReturn(Optional.empty());
        when(candidateProfileRepository.save(any(CandidateProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        CandidateProfile profile = profileService.getOrCreateProfile(TEST_USER_ID);

        // Then
        assertThat(profile).isNotNull();
        assertThat(profile.getFullName()).isEqualTo("Test User");
        assertThat(profile.getEmail()).isEqualTo("test@example.com");
        assertThat(profile.getProgram()).isEqualTo("Test Program");
        assertThat(profile.getClassCode()).isEqualTo("Class 1A");
        assertThat(profile.getCourseName()).isEqualTo("Java Course");
        assertThat(profile.getVisibility()).isEqualTo(CandidateProfile.Visibility.PRIVATE);
        verify(lmsClient).getStudent(TEST_USER_ID);
        verify(candidateProfileRepository).save(any(CandidateProfile.class));
    }

    @Test
    void getOrCreateProfile_returns_existing_on_second_call() {
        // Given
        CandidateProfile existingProfile = CandidateProfile.builder()
                .userId(TEST_USER_ID)
                .fullName("Existing User")
                .email("existing@example.com")
                .program("Existing Program")
                .classCode("Class 2B")
                .courseName("Spring Course")
                .visibility(CandidateProfile.Visibility.PUBLIC)
                .build();

        when(candidateProfileRepository.findByUserId(TEST_USER_ID)).thenReturn(Optional.of(existingProfile));

        // When
        CandidateProfile profile = profileService.getOrCreateProfile(TEST_USER_ID);

        // Then
        assertThat(profile).isNotNull();
        assertThat(profile.getFullName()).isEqualTo("Existing User");
        assertThat(profile.getEmail()).isEqualTo("existing@example.com");
        assertThat(profile.getProgram()).isEqualTo("Existing Program");
        assertThat(profile.getClassCode()).isEqualTo("Class 2B");
        assertThat(profile.getCourseName()).isEqualTo("Spring Course");
        assertThat(profile.getVisibility()).isEqualTo(CandidateProfile.Visibility.PUBLIC);
        verify(lmsClient, never()).getStudent(any());
        verify(candidateProfileRepository, never()).save(any());
    }

    @Test
    void upsertSelfSkills_creates_self_evidence_unverified() {
        // Given
        Skill javaSkill = Skill.builder()
                .id(SKILL_ID)
                .code("JAVA_CORE")
                .name("Java Core")
                .group("Programming")
                .build();

        when(skillRepository.findById(SKILL_ID)).thenReturn(Optional.of(javaSkill));
        when(skillEvidenceRepository.findByUserIdAndSkillIdAndSourceAndSourceRef(
                TEST_USER_ID, SKILL_ID, "SELF", "SELF_JAVA_CORE")).thenReturn(Optional.empty());
        when(skillScoreRepository.findByUserIdAndSkillId(TEST_USER_ID, SKILL_ID)).thenReturn(Optional.empty());

        // When
        profileService.upsertSelfSkills(TEST_USER_ID, List.of(new SelfSkillDto(SKILL_ID, 85)));

        // Then — evidence is recorded as self-declared and NOT verified
        ArgumentCaptor<SkillEvidence> evidenceCaptor = ArgumentCaptor.forClass(SkillEvidence.class);
        verify(skillEvidenceRepository).save(evidenceCaptor.capture());
        SkillEvidence savedEvidence = evidenceCaptor.getValue();
        assertThat(savedEvidence.getUserId()).isEqualTo(TEST_USER_ID);
        assertThat(savedEvidence.getSkill().getId()).isEqualTo(SKILL_ID);
        assertThat(savedEvidence.getSource()).isEqualTo("SELF");
        assertThat(savedEvidence.getSourceRef()).isEqualTo("SELF_JAVA_CORE");
        assertThat(savedEvidence.getScore()).isEqualTo((short) 85);
        assertThat(savedEvidence.isVerified()).isFalse();
        assertThat(savedEvidence.getRecordedAt()).isNotNull();

        // Then — skill_score carries selfScore only, never verifiedScore
        ArgumentCaptor<SkillScore> scoreCaptor = ArgumentCaptor.forClass(SkillScore.class);
        verify(skillScoreRepository).save(scoreCaptor.capture());
        SkillScore savedScore = scoreCaptor.getValue();
        assertThat(savedScore.getUserId()).isEqualTo(TEST_USER_ID);
        assertThat(savedScore.getSkillId()).isEqualTo(SKILL_ID);
        assertThat(savedScore.getSelfScore()).isEqualTo((short) 85);
        assertThat(savedScore.getVerifiedScore()).isNull();
        assertThat(savedScore.getLevel()).isEqualTo(LevelConstants.LEVEL_NANG_CAO);
        assertThat(savedScore.getUpdatedAt()).isNotNull();
    }

    @Test
    void upsertSelfSkills_updates_existing_evidence_in_place() {
        // Given — an existing self-declared skill at 50
        Skill javaSkill = Skill.builder()
                .id(SKILL_ID)
                .code("JAVA_CORE")
                .name("Java Core")
                .group("Programming")
                .build();

        SkillEvidence existingEvidence = SkillEvidence.builder()
                .userId(TEST_USER_ID)
                .skill(javaSkill)
                .source("SELF")
                .sourceRef("SELF_JAVA_CORE")
                .title("Self-reported Java Core")
                .score((short) 50)
                .recordedAt(OffsetDateTime.now().minusDays(10))
                .verified(false)
                .build();

        SkillScore existingScore = SkillScore.builder()
                .userId(TEST_USER_ID)
                .skillId(SKILL_ID)
                .selfScore((short) 50)
                .level(LevelConstants.LEVEL_KHA)
                .updatedAt(OffsetDateTime.now().minusDays(10))
                .build();

        when(skillRepository.findById(SKILL_ID)).thenReturn(Optional.of(javaSkill));
        when(skillEvidenceRepository.findByUserIdAndSkillIdAndSourceAndSourceRef(
                TEST_USER_ID, SKILL_ID, "SELF", "SELF_JAVA_CORE")).thenReturn(Optional.of(existingEvidence));
        when(skillScoreRepository.findByUserIdAndSkillId(TEST_USER_ID, SKILL_ID)).thenReturn(Optional.of(existingScore));

        // When — re-submit the same skill with a new score
        profileService.upsertSelfSkills(TEST_USER_ID, List.of(new SelfSkillDto(SKILL_ID, 70)));

        // Then — the same row is updated, not duplicated
        assertThat(existingEvidence.getScore()).isEqualTo((short) 70);
        assertThat(existingEvidence.isVerified()).isFalse();
        assertThat(existingScore.getSelfScore()).isEqualTo((short) 70);
        assertThat(existingScore.getLevel()).isEqualTo(LevelConstants.LEVEL_VUNG);
        verify(skillEvidenceRepository).save(existingEvidence);
        verify(skillScoreRepository).save(existingScore);
    }

    @Test
    void upsertSelfSkills_level_82_is_Vung_on_the_shared_scale() {
        // 82 sits between the two candidate scales. The shared HS-01 scale (85/70/50)
        // puts it at "Vững"; a stricter 80-floor scale would wrongly report "Năng cao".
        assertThat(levelForSelfScore(82)).isEqualTo(LevelConstants.LEVEL_VUNG);
    }

    @Test
    void upsertSelfSkills_level_60_is_Kha_on_the_shared_scale() {
        // 60 is below the shared "Vững" floor of 70, so it must fall to "Khá".
        assertThat(levelForSelfScore(60)).isEqualTo(LevelConstants.LEVEL_KHA);
    }

    private String levelForSelfScore(int selfScore) {
        Skill javaSkill = Skill.builder()
                .id(SKILL_ID)
                .code("JAVA_CORE")
                .name("Java Core")
                .group("Programming")
                .build();

        when(skillRepository.findById(SKILL_ID)).thenReturn(Optional.of(javaSkill));
        when(skillEvidenceRepository.findByUserIdAndSkillIdAndSourceAndSourceRef(
                TEST_USER_ID, SKILL_ID, "SELF", "SELF_JAVA_CORE")).thenReturn(Optional.empty());
        when(skillScoreRepository.findByUserIdAndSkillId(TEST_USER_ID, SKILL_ID)).thenReturn(Optional.empty());

        profileService.upsertSelfSkills(TEST_USER_ID, List.of(new SelfSkillDto(SKILL_ID, selfScore)));

        ArgumentCaptor<SkillScore> scoreCaptor = ArgumentCaptor.forClass(SkillScore.class);
        verify(skillScoreRepository).save(scoreCaptor.capture());
        return scoreCaptor.getValue().getLevel();
    }

    @Test
    void upsertSelfSkills_rejects_negative_score() {
        assertThatThrownBy(() -> profileService.upsertSelfSkills(TEST_USER_ID,
                List.of(new SelfSkillDto(SKILL_ID, -1))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("selfScore must be between 0 and 100");
    }

    @Test
    void upsertSelfSkills_rejects_score_above_100() {
        assertThatThrownBy(() -> profileService.upsertSelfSkills(TEST_USER_ID,
                List.of(new SelfSkillDto(SKILL_ID, 101))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("selfScore must be between 0 and 100");
    }

    @Test
    void upsertSelfSkills_rejects_null_score() {
        assertThatThrownBy(() -> profileService.upsertSelfSkills(TEST_USER_ID,
                List.of(new SelfSkillDto(SKILL_ID, null))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("selfScore must be between 0 and 100");
    }

    @Test
    void upsertSelfSkills_rejects_null_skillId() {
        assertThatThrownBy(() -> profileService.upsertSelfSkills(TEST_USER_ID,
                List.of(new SelfSkillDto(null, 85))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("skillId must not be null");
    }

    @Test
    void upsertSelfSkills_handles_null_list() {
        // Should not throw, just return silently
        profileService.upsertSelfSkills(TEST_USER_ID, null);
        verifyNoInteractions(skillRepository, skillEvidenceRepository, skillScoreRepository);
    }

    @Test
    void upsertSelfSkills_handles_empty_list() {
        // Should not throw, just return silently
        profileService.upsertSelfSkills(TEST_USER_ID, List.of());
        verifyNoInteractions(skillRepository, skillEvidenceRepository, skillScoreRepository);
    }

    @Test
    void upsertSelfSkills_existing_verifiedScore_takes_precedence_over_selfScore() {
        // Given — existing skill with verifiedScore = 85 (level "Năng cao")
        Skill javaSkill = Skill.builder()
                .id(SKILL_ID)
                .code("JAVA_CORE")
                .name("Java Core")
                .group("Programming")
                .build();

        SkillScore existingScore = SkillScore.builder()
                .userId(TEST_USER_ID)
                .skillId(SKILL_ID)
                .verifiedScore((short) 85)  // Verified score = 85 → "Năng cao"
                .selfScore((short) 50)       // Previous self score = 50
                .level(LevelConstants.LEVEL_NANG_CAO)
                .updatedAt(OffsetDateTime.now().minusDays(10))
                .build();

        SkillEvidence existingLmsEvidence = SkillEvidence.builder()
                .userId(TEST_USER_ID)
                .skill(javaSkill)
                .source("LMS")  // Verified evidence
                .sourceRef("JAVA01")
                .title("Java Core Course")
                .score((short) 85)
                .recordedAt(OffsetDateTime.now().minusDays(10))
                .verified(true)
                .build();

        when(skillRepository.findById(SKILL_ID)).thenReturn(Optional.of(javaSkill));
        when(skillEvidenceRepository.findByUserIdAndSkillIdAndSourceAndSourceRef(
                TEST_USER_ID, SKILL_ID, "SELF", "SELF_JAVA_CORE")).thenReturn(Optional.empty());
        when(skillScoreRepository.findByUserIdAndSkillId(TEST_USER_ID, SKILL_ID)).thenReturn(Optional.of(existingScore));

        // When — user submits a lower selfScore (70) for the same skill
        profileService.upsertSelfSkills(TEST_USER_ID, List.of(new SelfSkillDto(SKILL_ID, 70)));

        // Then — verifiedScore (85) takes precedence, level remains "Năng cao" (≥85)
        ArgumentCaptor<SkillScore> scoreCaptor = ArgumentCaptor.forClass(SkillScore.class);
        verify(skillScoreRepository).save(scoreCaptor.capture());
        SkillScore savedScore = scoreCaptor.getValue();
        assertThat(savedScore.getVerifiedScore()).isEqualTo((short) 85);
        assertThat(savedScore.getSelfScore()).isEqualTo((short) 70);  // selfScore updated
        assertThat(savedScore.getLevel()).isEqualTo(LevelConstants.LEVEL_NANG_CAO);     // Level based on verifiedScore (85)

        // Verify a NEW self-evidence is created (not the existing LMS evidence)
        ArgumentCaptor<SkillEvidence> evidenceCaptor = ArgumentCaptor.forClass(SkillEvidence.class);
        verify(skillEvidenceRepository).save(evidenceCaptor.capture());
        SkillEvidence savedSelfEvidence = evidenceCaptor.getValue();
        assertThat(savedSelfEvidence.getSource()).isEqualTo("SELF");
        assertThat(savedSelfEvidence.getSourceRef()).isEqualTo("SELF_JAVA_CORE");
        assertThat(savedSelfEvidence.getScore()).isEqualTo((short) 70);
        assertThat(savedSelfEvidence.isVerified()).isFalse();
    }
}