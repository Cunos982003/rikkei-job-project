package com.example.profileservice.profile.service;

import com.example.profileservice.profile.api.dto.LmsStudentDto;
import com.example.profileservice.profile.domain.CandidateProfile;
import com.example.profileservice.profile.infrastructure.CandidateProfileRepository;
import com.example.profileservice.profile.infrastructure.LmsClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    private static final UUID TEST_USER_ID = UUID.randomUUID();

    @Mock
    CandidateProfileRepository candidateProfileRepository;

    @Mock
    LmsClient lmsClient;

    ProfileService profileService;

    @BeforeEach
    void setup() {
        profileService = new ProfileService(candidateProfileRepository, lmsClient);
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
}