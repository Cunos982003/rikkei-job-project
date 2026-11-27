package com.example.profileservice.controller;

import com.example.profileservice.common.domain.Visibility;
import com.example.profileservice.dto.ContactRequestDto;
import com.example.profileservice.entity.CandidateProfile;
import com.example.profileservice.entity.ContactRequest;
import com.example.profileservice.entity.Skill;
import com.example.profileservice.entity.SkillScore;
import com.example.profileservice.repository.CandidateProfileRepository;
import com.example.profileservice.repository.SkillRepository;
import com.example.profileservice.repository.SkillScoreRepository;
import com.example.profileservice.service.ContactAccessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * NT-08: Test chứng minh HR không nhận được email/phone trước khi được mở liên hệ
 * Áp dụng cho:
 * 1. GET /api/v1/candidates (tìm kiếm / danh sách ứng viên)
 * 2. GET /api/v1/candidates/{userId} (chi tiết ứng viên)
 * 3. GET /api/v1/candidates/{userId}/contact (lấy liên hệ khi chưa mở -> 403 CONTACT_LOCKED)
 * 4. GET /api/v1/candidates/{userId}/contact (sau khi được duyệt -> 200 có email, phone)
 */
class CandidateContactPrivacyTest {

    private CandidateProfileRepository profileRepository;
    private SkillScoreRepository skillScoreRepository;
    private SkillRepository skillRepository;
    private ContactAccessService contactAccessService;

    private MockMvc candidateDirMvc;
    private MockMvc contactMvc;

    private final UUID candidateId = UUID.randomUUID();
    private final UUID hrId = UUID.randomUUID();
    private final UUID companyId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private final UUID skillId = UUID.randomUUID();

    private CandidateProfile profile;

    @BeforeEach
    void setUp() {
        profileRepository = mock(CandidateProfileRepository.class);
        skillScoreRepository = mock(SkillScoreRepository.class);
        skillRepository = mock(SkillRepository.class);
        contactAccessService = mock(ContactAccessService.class);

        CandidateDirectoryController directoryController = new CandidateDirectoryController(
                profileRepository, skillScoreRepository, skillRepository, contactAccessService);
        candidateDirMvc = MockMvcBuilders.standaloneSetup(directoryController).build();

        ContactController contactController = new ContactController(contactAccessService);
        contactMvc = MockMvcBuilders.standaloneSetup(contactController).build();

        profile = CandidateProfile.builder()
                .userId(candidateId)
                .fullName("Nguyễn Văn A")
                .email("nguyenvana@example.com")
                .phone("0912345678")
                .headline("Java Backend Developer")
                .program("Kỹ sư phần mềm")
                .visibility(Visibility.PUBLIC)
                .createdAt(OffsetDateTime.now())
                .build();

        Skill skill = Skill.builder()
                .id(skillId)
                .code("JAVA")
                .name("Java")
                .group("Backend")
                .build();

        SkillScore score = SkillScore.builder()
                .userId(candidateId)
                .skillId(skillId)
                .selfScore((short) 80)
                .verifiedScore((short) 85)
                .build();

        when(profileRepository.findAll()).thenReturn(List.of(profile));
        when(profileRepository.findByUserId(candidateId)).thenReturn(Optional.of(profile));
        when(skillRepository.findAll()).thenReturn(List.of(skill));
        when(skillScoreRepository.findByUserId(candidateId)).thenReturn(List.of(score));
    }

    @Test
    @DisplayName("HR tìm kiếm ứng viên khi chưa mở liên hệ -> JSON không chứa email và phone (null)")
    void listCandidates_whenContactNotUnlocked_shouldHideEmailAndPhone() throws Exception {
        // canSeeContact = false
        when(contactAccessService.canSeeContact(eq(companyId), eq(candidateId))).thenReturn(false);

        candidateDirMvc.perform(get("/api/v1/candidates")
                        .header("X-User-Id", hrId.toString())
                        .header("X-User-Role", "HR")
                        .header("X-Company-Id", companyId.toString())
                        .param("keyword", "Java"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].fullName").value("Nguyễn Văn A"))
                .andExpect(jsonPath("$.data.items[0].email").doesNotExist())
                .andExpect(jsonPath("$.data.items[0].phone").doesNotExist())
                .andExpect(jsonPath("$.data.items[0].contactLocked").value(true));
    }

    @Test
    @DisplayName("HR xem chi tiết ứng viên khi chưa mở liên hệ -> JSON không chứa email và phone (null)")
    void getCandidateById_whenContactNotUnlocked_shouldHideEmailAndPhone() throws Exception {
        // canSeeContact = false
        when(contactAccessService.canSeeContact(eq(companyId), eq(candidateId))).thenReturn(false);

        candidateDirMvc.perform(get("/api/v1/candidates/" + candidateId)
                        .header("X-User-Id", hrId.toString())
                        .header("X-User-Role", "HR")
                        .header("X-Company-Id", companyId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fullName").value("Nguyễn Văn A"))
                .andExpect(jsonPath("$.data.email").doesNotExist())
                .andExpect(jsonPath("$.data.phone").doesNotExist())
                .andExpect(jsonPath("$.data.contactLocked").value(true));
    }

    @Test
    @DisplayName("HR gọi API lấy thông tin liên hệ khi chưa mở liên hệ -> 403 Forbidden CONTACT_LOCKED")
    void getContact_whenContactLocked_shouldReturn403Forbidden() throws Exception {
        when(contactAccessService.getContact(eq(companyId), eq(hrId), eq(candidateId)))
                .thenThrow(new ContactAccessService.ContactAccessDeniedException("CONTACT_LOCKED"));

        contactMvc.perform(get("/api/v1/candidates/" + candidateId + "/contact")
                        .header("X-User-Id", hrId.toString()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("CONTACT_LOCKED"));
    }

    @Test
    @DisplayName("HR gọi API lấy liên hệ SAU KHI ĐƯỢC DUYỆT -> 200 OK trả về đầy đủ email và phone")
    void getContact_whenApproved_shouldReturnEmailAndPhone() throws Exception {
        ContactAccessService.ContactResponse contactResponse = ContactAccessService.ContactResponse.builder()
                .email("nguyenvana@example.com")
                .phone("0912345678")
                .build();

        when(contactAccessService.getContact(eq(companyId), eq(hrId), eq(candidateId)))
                .thenReturn(contactResponse);

        contactMvc.perform(get("/api/v1/candidates/" + candidateId + "/contact")
                        .header("X-User-Id", hrId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("nguyenvana@example.com"))
                .andExpect(jsonPath("$.data.phone").value("0912345678"));
    }

    @Test
    @DisplayName("HR xem chi tiết ứng viên SAU KHI ĐƯỢC DUYỆT mở liên hệ -> Trả về email và phone công khai")
    void getCandidateById_whenContactUnlocked_shouldReturnEmailAndPhone() throws Exception {
        when(contactAccessService.canSeeContact(eq(companyId), eq(candidateId))).thenReturn(true);

        candidateDirMvc.perform(get("/api/v1/candidates/" + candidateId)
                        .header("X-User-Id", hrId.toString())
                        .header("X-User-Role", "HR")
                        .header("X-Company-Id", companyId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fullName").value("Nguyễn Văn A"))
                .andExpect(jsonPath("$.data.email").value("nguyenvana@example.com"))
                .andExpect(jsonPath("$.data.phone").value("0912345678"))
                .andExpect(jsonPath("$.data.contactLocked").value(false));
    }
}
