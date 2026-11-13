package com.example.profileservice.profile.service;

import com.example.profileservice.profile.domain.CandidateProfile;
import com.example.profileservice.profile.domain.CandidateProfile.Visibility;
import com.example.profileservice.profile.domain.ProfileReview;
import com.example.profileservice.profile.infrastructure.CandidateProfileRepository;
import com.example.profileservice.profile.infrastructure.ProfileReviewRepository;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VisibilityServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID STAFF_ID = UUID.randomUUID();

    @Mock
    CandidateProfileRepository profileRepo;

    @Mock
    ProfileReviewRepository reviewRepo;

    VisibilityService service;

    @BeforeEach
    void setup() {
        service = new VisibilityService(profileRepo, reviewRepo);
    }

    private CandidateProfile profileWith(Visibility visibility) {
        return CandidateProfile.builder()
                .userId(USER_ID)
                .visibility(visibility)
                .build();
    }

    private void givenProfile(CandidateProfile profile) {
        when(profileRepo.findByUserId(USER_ID)).thenReturn(Optional.of(profile));
        when(profileRepo.save(any(CandidateProfile.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private ProfileReview capturedReview() {
        ArgumentCaptor<ProfileReview> captor = ArgumentCaptor.forClass(ProfileReview.class);
        verify(reviewRepo).save(captor.capture());
        return captor.getValue();
    }

    @Test
    void submit_moves_private_to_pending_and_records_consent() {
        CandidateProfile profile = profileWith(Visibility.PRIVATE);
        givenProfile(profile);

        service.submitForReview(USER_ID, true);

        assertThat(profile.getVisibility()).isEqualTo(Visibility.PENDING);
        assertThat(profile.getConsentAt()).isNotNull();
        assertThat(profile.getConsentWithdrawnAt()).isNull();

        ProfileReview review = capturedReview();
        assertThat(review.getAction()).isEqualTo(ProfileReview.Action.SUBMIT);
        assertThat(review.getActorId()).isEqualTo(USER_ID);
        assertThat(review.getUserId()).isEqualTo(USER_ID);
    }

    @Test
    void submit_without_consent_is_rejected_and_changes_nothing() {
        CandidateProfile profile = profileWith(Visibility.PRIVATE);
        when(profileRepo.findByUserId(USER_ID)).thenReturn(Optional.of(profile));

        assertThatThrownBy(() -> service.submitForReview(USER_ID, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Consent");

        assertThat(profile.getVisibility()).isEqualTo(Visibility.PRIVATE);
        verify(profileRepo, never()).save(any());
        verify(reviewRepo, never()).save(any());
    }

    @Test
    void resubmit_from_rejected_moves_back_to_pending_and_clears_withdrawal() {
        CandidateProfile profile = CandidateProfile.builder()
                .userId(USER_ID)
                .visibility(Visibility.REJECTED)
                .consentWithdrawnAt(OffsetDateTime.now())
                .build();
        givenProfile(profile);

        service.submitForReview(USER_ID, true);

        assertThat(profile.getVisibility()).isEqualTo(Visibility.PENDING);
        assertThat(profile.getConsentWithdrawnAt()).isNull();
    }

    @Test
    void approve_moves_pending_to_public() {
        CandidateProfile profile = profileWith(Visibility.PENDING);
        givenProfile(profile);

        service.approve(USER_ID, STAFF_ID);

        assertThat(profile.getVisibility()).isEqualTo(Visibility.PUBLIC);
        ProfileReview review = capturedReview();
        assertThat(review.getAction()).isEqualTo(ProfileReview.Action.APPROVE);
        assertThat(review.getActorId()).isEqualTo(STAFF_ID);
    }

    @Test
    void reject_moves_pending_to_rejected_and_stores_reason() {
        CandidateProfile profile = profileWith(Visibility.PENDING);
        givenProfile(profile);

        service.reject(USER_ID, STAFF_ID, "Thiếu minh chứng kỹ năng");

        assertThat(profile.getVisibility()).isEqualTo(Visibility.REJECTED);
        ProfileReview review = capturedReview();
        assertThat(review.getAction()).isEqualTo(ProfileReview.Action.REJECT);
        assertThat(review.getReason()).isEqualTo("Thiếu minh chứng kỹ năng");
        assertThat(review.getActorId()).isEqualTo(STAFF_ID);
    }

    @Test
    void reject_without_reason_is_rejected_and_changes_nothing() {
        CandidateProfile profile = profileWith(Visibility.PENDING);
        when(profileRepo.findByUserId(USER_ID)).thenReturn(Optional.of(profile));

        assertThatThrownBy(() -> service.reject(USER_ID, STAFF_ID, "  "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("reason");

        assertThat(profile.getVisibility()).isEqualTo(Visibility.PENDING);
        verify(profileRepo, never()).save(any());
        verify(reviewRepo, never()).save(any());
    }

    @Test
    void withdraw_from_pending_returns_to_private() {
        CandidateProfile profile = profileWith(Visibility.PENDING);
        givenProfile(profile);

        service.withdraw(USER_ID);

        assertThat(profile.getVisibility()).isEqualTo(Visibility.PRIVATE);
        assertThat(profile.getConsentWithdrawnAt()).isNotNull();
        assertThat(capturedReview().getAction()).isEqualTo(ProfileReview.Action.WITHDRAW);
    }

    @Test
    void withdraw_from_public_returns_to_private() {
        CandidateProfile profile = profileWith(Visibility.PUBLIC);
        givenProfile(profile);

        service.withdraw(USER_ID);

        assertThat(profile.getVisibility()).isEqualTo(Visibility.PRIVATE);
        assertThat(capturedReview().getAction()).isEqualTo(ProfileReview.Action.WITHDRAW);
    }

    @Test
    void approve_private_profile_is_an_invalid_transition() {
        CandidateProfile profile = profileWith(Visibility.PRIVATE);
        when(profileRepo.findByUserId(USER_ID)).thenReturn(Optional.of(profile));

        assertThatThrownBy(() -> service.approve(USER_ID, STAFF_ID))
                .isInstanceOf(InvalidTransitionException.class)
                .hasMessageContaining("PENDING");

        assertThat(profile.getVisibility()).isEqualTo(Visibility.PRIVATE);
        verify(reviewRepo, never()).save(any());
    }

    @Test
    void withdraw_rejected_profile_is_an_invalid_transition() {
        CandidateProfile profile = profileWith(Visibility.REJECTED);
        when(profileRepo.findByUserId(USER_ID)).thenReturn(Optional.of(profile));

        assertThatThrownBy(() -> service.withdraw(USER_ID))
                .isInstanceOf(InvalidTransitionException.class);
    }

    @Test
    void submit_public_profile_is_an_invalid_transition() {
        CandidateProfile profile = profileWith(Visibility.PUBLIC);
        when(profileRepo.findByUserId(USER_ID)).thenReturn(Optional.of(profile));

        assertThatThrownBy(() -> service.submitForReview(USER_ID, true))
                .isInstanceOf(InvalidTransitionException.class);
    }

    @Test
    void reject_public_profile_is_an_invalid_transition() {
        CandidateProfile profile = profileWith(Visibility.PUBLIC);
        when(profileRepo.findByUserId(USER_ID)).thenReturn(Optional.of(profile));

        assertThatThrownBy(() -> service.reject(USER_ID, STAFF_ID, "lý do"))
                .isInstanceOf(InvalidTransitionException.class);
    }

    @Test
    void review_history_is_returned_newest_first() {
        ProfileReview older = ProfileReview.builder()
                .userId(USER_ID).action(ProfileReview.Action.SUBMIT)
                .actorId(USER_ID).createdAt(OffsetDateTime.now().minusHours(2)).build();
        ProfileReview newer = ProfileReview.builder()
                .userId(USER_ID).action(ProfileReview.Action.APPROVE)
                .actorId(STAFF_ID).createdAt(OffsetDateTime.now()).build();
        when(reviewRepo.findByUserIdOrderByCreatedAtDesc(USER_ID)).thenReturn(List.of(newer, older));

        List<ProfileReview> history = service.getReviewHistory(USER_ID);

        assertThat(history).hasSize(2);
        assertThat(history.get(0).getAction()).isEqualTo(ProfileReview.Action.APPROVE);
        assertThat(history.get(1).getAction()).isEqualTo(ProfileReview.Action.SUBMIT);
    }

    @Test
    void operations_on_missing_profile_are_rejected() {
        when(profileRepo.findByUserId(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.submitForReview(USER_ID, true))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.withdraw(USER_ID))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.approve(USER_ID, STAFF_ID))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void full_lifecycle_records_every_transition() {
        CandidateProfile profile = profileWith(Visibility.PRIVATE);
        givenProfile(profile);

        service.submitForReview(USER_ID, true);
        service.reject(USER_ID, STAFF_ID, "Bổ sung chứng chỉ");
        service.submitForReview(USER_ID, true);
        service.approve(USER_ID, STAFF_ID);
        service.withdraw(USER_ID);

        ArgumentCaptor<ProfileReview> captor = ArgumentCaptor.forClass(ProfileReview.class);
        verify(reviewRepo, times(5)).save(captor.capture());

        assertThat(captor.getAllValues()).extracting(ProfileReview::getAction).containsExactly(
                ProfileReview.Action.SUBMIT,
                ProfileReview.Action.REJECT,
                ProfileReview.Action.SUBMIT,
                ProfileReview.Action.APPROVE,
                ProfileReview.Action.WITHDRAW);
        assertThat(profile.getVisibility()).isEqualTo(Visibility.PRIVATE);
    }
}
