package com.example.recruitment_service.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.example.recruitment_service.entity.Recruitment;
import com.example.recruitment_service.entity.RecruitmentStatusHistory;
import com.example.recruitment_service.enums.RecruitmentStatus;

import jakarta.persistence.EntityManager;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RecruitmentRepositoryTest {

    @Autowired
    private RecruitmentRepository recruitments;

    @Autowired
    private RecruitmentStatusHistoryRepository history;

    @Autowired
    private EntityManager entityManager;

    @Test
    void persistsRecruitmentAndStatusHistory() {
        UUID userId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        Recruitment recruitment = new Recruitment();
        recruitment.setUser_id(userId);
        recruitment.setJob_id(jobId);
        recruitment.setStatus(RecruitmentStatus.SUBMITTED);
        UUID id = recruitments.saveAndFlush(recruitment).getId();

        RecruitmentStatusHistory entry = new RecruitmentStatusHistory();
        entry.setRecruitment_id(id);
        entry.setActor_id(userId);
        entry.setFrom_status(RecruitmentStatus.SUBMITTED);
        entry.setTo_status(RecruitmentStatus.SUBMITTED);
        history.saveAndFlush(entry);
        entityManager.clear();

        assertThat(recruitments.findForUserById(id, userId))
                .get().extracting(Recruitment::getJob_id).isEqualTo(jobId);
        assertThat(recruitments.existsActiveByJobAndUser(jobId, userId, RecruitmentStatus.WITHDRAWN)).isTrue();
        assertThat(history.findHistory(id))
                .singleElement().extracting(RecruitmentStatusHistory::getActor_id).isEqualTo(userId);
    }

    @Test
    void companyFilterExcludesLegacyRowsWithoutCompanySnapshotButPlatformListIncludesThem() {
        UUID companyId = UUID.randomUUID();
        Recruitment owned = recruitment(UUID.randomUUID(), UUID.randomUUID(), companyId);
        Recruitment legacy = recruitment(UUID.randomUUID(), UUID.randomUUID(), null);
        recruitments.saveAndFlush(owned);
        recruitments.saveAndFlush(legacy);
        PageRequest page = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "created_at"));

        var companyResults = recruitments.findForStaff(companyId, null, null, page);
        var platformResults = recruitments.findForStaff(null, null, null, page);

        assertThat(companyResults.getContent()).extracting(Recruitment::getId).containsExactly(owned.getId());
        assertThat(platformResults.getContent()).extracting(Recruitment::getId)
                .contains(owned.getId(), legacy.getId());
    }

    private Recruitment recruitment(UUID userId, UUID jobId, UUID companyId) {
        Recruitment recruitment = new Recruitment();
        recruitment.setUser_id(userId);
        recruitment.setJob_id(jobId);
        recruitment.setCompany_id(companyId);
        recruitment.setStatus(RecruitmentStatus.SUBMITTED);
        return recruitment;
    }
}
