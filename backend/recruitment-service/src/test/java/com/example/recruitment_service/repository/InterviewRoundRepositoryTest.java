package com.example.recruitment_service.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.example.recruitment_service.entity.InterviewRound;
import com.example.recruitment_service.entity.Recruitment;
import com.example.recruitment_service.enums.InterviewRoundResult;
import com.example.recruitment_service.enums.RecruitmentStatus;

import jakarta.persistence.EntityManager;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class InterviewRoundRepositoryTest {

    @Autowired
    private InterviewRoundRepository rounds;

    @Autowired
    private RecruitmentRepository recruitments;

    @Autowired
    private EntityManager entityManager;

    @Test
    void persistsRoundsWithSequentialNumbersAndNullSchedule() {
        Recruitment recruitment = createRecruitment();
        rounds.saveAndFlush(round(recruitment, 1, 0));
        rounds.saveAndFlush(round(recruitment, 2, 100));
        rounds.saveAndFlush(round(recruitment, 3, null));
        entityManager.clear();

        List<InterviewRound> persisted = rounds.findByRecruitment_IdOrderByRoundNoAsc(recruitment.getId());

        assertThat(persisted).extracting(InterviewRound::getRoundNo).containsExactly(1, 2, 3);
        assertThat(persisted).extracting(InterviewRound::getScheduledAt).containsOnlyNulls();
        assertThat(persisted).extracting(InterviewRound::getResult)
                .containsOnly(InterviewRoundResult.PENDING);
        assertThat(persisted).extracting(InterviewRound::getScore).containsExactly(0, 100, null);
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 101})
    void rejectsScoresOutsideZeroToHundred(int score) {
        Recruitment recruitment = createRecruitment();

        assertThatThrownBy(() -> rounds.saveAndFlush(round(recruitment, 1, score)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private Recruitment createRecruitment() {
        Recruitment recruitment = new Recruitment();
        recruitment.setUser_id(UUID.randomUUID());
        recruitment.setJob_id(UUID.randomUUID());
        recruitment.setStatus(RecruitmentStatus.INTERVIEW);
        return recruitments.saveAndFlush(recruitment);
    }

    private InterviewRound round(Recruitment recruitment, int roundNo, Integer score) {
        InterviewRound round = new InterviewRound();
        round.setRecruitment(recruitment);
        round.setRoundNo(roundNo);
        round.setName("Round " + roundNo);
        round.setScore(score);
        return round;
    }
}
