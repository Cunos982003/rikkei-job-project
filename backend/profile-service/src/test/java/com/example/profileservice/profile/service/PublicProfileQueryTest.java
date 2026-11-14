package com.example.profileservice.profile.service;

import com.example.profileservice.profile.domain.CandidateProfile;
import com.example.profileservice.profile.domain.CandidateProfile.Visibility;
import com.example.profileservice.profile.infrastructure.CandidateProfileRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Điều kiện dùng chung "chỉ hồ sơ PUBLIC" mà DN-03, DN-06, UT-03 sẽ dùng.
 */
@SpringBootTest
@Testcontainers
@Transactional
class PublicProfileQueryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17.4")
            .withDatabaseName("profile_db")
            .withUsername("postgres")
            .withPassword("postgres");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    CandidateProfileRepository profileRepo;

    @Autowired
    PublicProfileQuery publicProfileQuery;

    private UUID profileWith(Visibility visibility) {
        UUID userId = UUID.randomUUID();
        profileRepo.save(CandidateProfile.builder()
                .userId(userId)
                .fullName("Ứng viên " + visibility)
                .visibility(visibility)
                .build());
        return userId;
    }

    @Test
    void only_public_profiles_are_returned() {
        UUID publicId = profileWith(Visibility.PUBLIC);
        UUID privateId = profileWith(Visibility.PRIVATE);
        UUID pendingId = profileWith(Visibility.PENDING);
        UUID rejectedId = profileWith(Visibility.REJECTED);

        assertThat(publicProfileQuery.findAll()).extracting(CandidateProfile::getUserId)
                .contains(publicId)
                .doesNotContain(privateId, pendingId, rejectedId);
    }

    @Test
    void paged_query_also_restricts_to_public() {
        UUID publicId = profileWith(Visibility.PUBLIC);
        UUID pendingId = profileWith(Visibility.PENDING);

        assertThat(publicProfileQuery.findAll(PageRequest.of(0, 50)).getContent())
                .extracting(CandidateProfile::getUserId)
                .contains(publicId)
                .doesNotContain(pendingId);
    }

    @Test
    void find_by_id_hides_non_public_profiles() {
        UUID publicId = profileWith(Visibility.PUBLIC);
        UUID pendingId = profileWith(Visibility.PENDING);

        assertThat(publicProfileQuery.findById(publicId)).isPresent();
        assertThat(publicProfileQuery.findById(pendingId)).isEmpty();
        assertThat(publicProfileQuery.isVisibleToRecruiters(publicId)).isTrue();
        assertThat(publicProfileQuery.isVisibleToRecruiters(pendingId)).isFalse();
    }

    @Test
    void find_by_id_hides_unknown_user() {
        assertThat(publicProfileQuery.findById(UUID.randomUUID())).isEmpty();
    }

    @Test
    void withdrawing_hides_profile_from_recruiters_immediately() {
        CandidateProfile profile = profileRepo.save(CandidateProfile.builder()
                .userId(UUID.randomUUID())
                .fullName("Ứng viên")
                .visibility(Visibility.PUBLIC)
                .build());
        assertThat(publicProfileQuery.isVisibleToRecruiters(profile.getUserId())).isTrue();

        profile.setVisibility(Visibility.PRIVATE);
        profileRepo.saveAndFlush(profile);

        assertThat(publicProfileQuery.isVisibleToRecruiters(profile.getUserId())).isFalse();
        assertThat(publicProfileQuery.findAll()).extracting(CandidateProfile::getUserId)
                .doesNotContain(profile.getUserId());
    }
}
