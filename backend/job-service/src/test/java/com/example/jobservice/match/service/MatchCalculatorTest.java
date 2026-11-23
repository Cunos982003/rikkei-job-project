package com.example.jobservice.match.service;

import com.example.jobservice.job.domain.JobSkill;
import com.example.jobservice.match.client.CandidateSkillScore;
import com.example.jobservice.match.dto.MatchSkillItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class MatchCalculatorTest {

    private final UUID dummyJobId = UUID.randomUUID();

    @Test
    @DisplayName("A) JD có bắt buộc và ưu tiên; verified đạt 100%, 50%, 100% -> score 82.5, belowMinimum=false")
    void caseA_mixedRequiredAndOptional_exact50Percent_notBelowMinimum() {
        // JD: JAVA_CORE bắt buộc min70, SPRING_BOOT bắt buộc min60, GIT ưu tiên min50
        List<JobSkill> jobSkills = List.of(
                new JobSkill(dummyJobId, "skl-01", "JAVA_CORE", "Java Core", true, 70),
                new JobSkill(dummyJobId, "skl-02", "SPRING_BOOT", "Spring Boot", true, 60),
                new JobSkill(dummyJobId, "skl-04", "GIT", "Git", false, 50)
        );

        // Ứng viên verified: JAVA_CORE 70, SPRING_BOOT 30, GIT 50
        List<CandidateSkillScore> candidateSkills = List.of(
                new CandidateSkillScore("skl-01", "JAVA_CORE", "Java Core", 70, null),
                new CandidateSkillScore("skl-02", "SPRING_BOOT", "Spring Boot", 30, null),
                new CandidateSkillScore("skl-04", "GIT", "Git", 50, null)
        );

        MatchCalculator.MatchResult result = MatchCalculator.computeMatch(jobSkills, candidateSkills);

        // TBreq = (70/70 + 30/60) / 2 = (1.0 + 0.5) / 2 = 0.75
        // TBopt = 50/50 = 1.0
        // score = 70 * 0.75 + 30 * 1.0 = 52.5 + 30 = 82.5
        assertEquals(new BigDecimal("82.5"), result.score());
        assertFalse(result.belowMinimum(), "30 đúng bằng 50% của 60 nên không được coi là dưới ngưỡng tối thiểu");

        // met: JAVA_CORE, GIT
        assertEquals(2, result.met().size());
        assertEquals("JAVA_CORE", result.met().get(0).skillCode());
        assertEquals("GIT", result.met().get(1).skillCode());

        // missing: SPRING_BOOT (gap 30)
        assertEquals(1, result.missing().size());
        MatchSkillItem missingItem = result.missing().get(0);
        assertEquals("SPRING_BOOT", missingItem.skillCode());
        assertEquals(30, missingItem.candidateScore());
        assertEquals(30, missingItem.gap());
    }

    @Test
    @DisplayName("A2) Như A nhưng SPRING_BOOT=29 -> score 81.9, belowMinimum=true")
    void caseA2_candidateScoreBelow50Percent_triggersBelowMinimum() {
        List<JobSkill> jobSkills = List.of(
                new JobSkill(dummyJobId, "skl-01", "JAVA_CORE", "Java Core", true, 70),
                new JobSkill(dummyJobId, "skl-02", "SPRING_BOOT", "Spring Boot", true, 60),
                new JobSkill(dummyJobId, "skl-04", "GIT", "Git", false, 50)
        );

        // SPRING_BOOT=29 (< 30)
        List<CandidateSkillScore> candidateSkills = List.of(
                new CandidateSkillScore("skl-01", "JAVA_CORE", "Java Core", 70, null),
                new CandidateSkillScore("skl-02", "SPRING_BOOT", "Spring Boot", 29, null),
                new CandidateSkillScore("skl-04", "GIT", "Git", 50, null)
        );

        MatchCalculator.MatchResult result = MatchCalculator.computeMatch(jobSkills, candidateSkills);

        // TBreq = (1.0 + 29/60) / 2 = 0.7416666...
        // score = 70 * 0.7416666... + 30 = 51.91666... + 30 = 81.9166... -> 81.9
        assertEquals(new BigDecimal("81.9"), result.score());
        assertTrue(result.belowMinimum(), "29 < 30 (50% của 60) nên belowMinimum phải là true");
    }

    @Test
    @DisplayName("B) JD chỉ có kỹ năng bắt buộc: ratio 0.5, 1, 0 -> score 50.0; belowMinimum=true")
    void caseB_onlyRequiredSkills_missingSkill_belowMinimumTrue() {
        // JD: JAVA_CORE min80, SQL min60, REACT min50 (đều bắt buộc)
        List<JobSkill> jobSkills = List.of(
                new JobSkill(dummyJobId, "skl-01", "JAVA_CORE", "Java Core", true, 80),
                new JobSkill(dummyJobId, "skl-03", "SQL", "SQL", true, 60),
                new JobSkill(dummyJobId, "skl-08", "REACT", "React", true, 50)
        );

        // Ứng viên: JAVA_CORE 40, SQL 60, REACT không có
        List<CandidateSkillScore> candidateSkills = List.of(
                new CandidateSkillScore("skl-01", "JAVA_CORE", "Java Core", 40, null),
                new CandidateSkillScore("skl-03", "SQL", "SQL", 60, null)
        );

        MatchCalculator.MatchResult result = MatchCalculator.computeMatch(jobSkills, candidateSkills);

        // TBreq = (40/80 + 60/60 + 0/50) / 3 = (0.5 + 1.0 + 0.0) / 3 = 0.5
        // Không có kỹ năng ưu tiên -> score = 100 * 0.5 = 50.0
        assertEquals(new BigDecimal("50.0"), result.score());
        assertTrue(result.belowMinimum(), "REACT điểm 0 < 25 (50% của 50) nên belowMinimum=true");

        // met: SQL
        assertEquals(1, result.met().size());
        assertEquals("SQL", result.met().get(0).skillCode());

        // missing: JAVA_CORE (gap 40), REACT (gap 50) sắp xếp theo tên
        assertEquals(2, result.missing().size());
        assertEquals("JAVA_CORE", result.missing().get(0).skillCode());
        assertEquals(40, result.missing().get(0).gap());
        assertEquals("REACT", result.missing().get(1).skillCode());
        assertEquals(50, result.missing().get(1).gap());
    }

    @Test
    @DisplayName("C) Ứng viên không có kỹ năng nào -> score 0.0, missing liệt kê đủ mọi kỹ năng, belowMinimum=true")
    void caseC_candidateNoSkills_scoreZero_allInMissing() {
        List<JobSkill> jobSkills = List.of(
                new JobSkill(dummyJobId, "skl-01", "JAVA_CORE", "Java Core", true, 80),
                new JobSkill(dummyJobId, "skl-04", "GIT", "Git", false, 50)
        );

        MatchCalculator.MatchResult result = MatchCalculator.computeMatch(jobSkills, Collections.emptyList());

        assertEquals(new BigDecimal("0.0"), result.score());
        assertTrue(result.belowMinimum());
        assertTrue(result.met().isEmpty());
        assertEquals(2, result.missing().size());
        // Bắt buộc trước, ưu tiên sau
        assertEquals("JAVA_CORE", result.missing().get(0).skillCode());
        assertEquals(80, result.missing().get(0).gap());
        assertEquals("GIT", result.missing().get(1).skillCode());
        assertEquals(50, result.missing().get(1).gap());
    }

    @Test
    @DisplayName("D) Ứng viên chỉ có selfScore 90, verifiedScore null -> candidateScore = 0, không cộng điểm")
    void caseD_onlySelfScore_verifiedScoreNull_treatedAsZero() {
        List<JobSkill> jobSkills = List.of(
                new JobSkill(dummyJobId, "skl-01", "JAVA_CORE", "Java Core", true, 70)
        );

        List<CandidateSkillScore> candidateSkills = List.of(
                new CandidateSkillScore("skl-01", "JAVA_CORE", "Java Core", null, 90)
        );

        MatchCalculator.MatchResult result = MatchCalculator.computeMatch(jobSkills, candidateSkills);

        assertEquals(new BigDecimal("0.0"), result.score());
        assertTrue(result.belowMinimum());
        assertEquals(1, result.missing().size());
        assertEquals(0, result.missing().get(0).candidateScore());
        assertEquals(70, result.missing().get(0).gap());
    }

    @Test
    @DisplayName("E) verifiedScore 100 so với minScore 70 -> ratio bị chặn ở 1.0 -> score 100.0")
    void caseE_scoreExceedsMinScore_cappedAtOne() {
        List<JobSkill> jobSkills = List.of(
                new JobSkill(dummyJobId, "skl-01", "JAVA_CORE", "Java Core", true, 70)
        );

        List<CandidateSkillScore> candidateSkills = List.of(
                new CandidateSkillScore("skl-01", "JAVA_CORE", "Java Core", 100, null)
        );

        MatchCalculator.MatchResult result = MatchCalculator.computeMatch(jobSkills, candidateSkills);

        assertEquals(new BigDecimal("100.0"), result.score());
        assertFalse(result.belowMinimum());
        assertEquals(1, result.met().size());
        assertEquals(100, result.met().get(0).candidateScore());
    }

    @Test
    @DisplayName("F) Các trường hợp biên: JD chỉ có kỹ năng ưu tiên, JD rỗng, minScore=0, khớp không phân biệt hoa thường")
    void caseF_edgeCases() {
        // 1. JD chỉ có kỹ năng ưu tiên: score = 100 * TBopt
        List<JobSkill> optOnlyJob = List.of(
                new JobSkill(dummyJobId, "skl-04", "GIT", "Git", false, 50),
                new JobSkill(dummyJobId, "skl-10", "DOCKER", "Docker", false, 50)
        );
        List<CandidateSkillScore> cand1 = List.of(
                new CandidateSkillScore("skl-04", "git", "Git", 50, null),
                new CandidateSkillScore("skl-10", "Docker", "Docker", 25, null)
        );
        MatchCalculator.MatchResult res1 = MatchCalculator.computeMatch(optOnlyJob, cand1);
        // TBopt = (1.0 + 0.5) / 2 = 0.75 -> score = 75.0
        assertEquals(new BigDecimal("75.0"), res1.score());
        assertFalse(res1.belowMinimum(), "Không có kỹ năng bắt buộc thì belowMinimum luôn false");

        // 2. JD không có kỹ năng nào: score = 0.0, met/missing rỗng, belowMinimum = false
        MatchCalculator.MatchResult res2 = MatchCalculator.computeMatch(Collections.emptyList(), cand1);
        assertEquals(new BigDecimal("0.0"), res2.score());
        assertFalse(res2.belowMinimum());
        assertTrue(res2.met().isEmpty());
        assertTrue(res2.missing().isEmpty());

        // 3. minScore = 0: ratio = 1.0
        List<JobSkill> minScoreZeroJob = List.of(
                new JobSkill(dummyJobId, "skl-01", "JAVA_CORE", "Java Core", true, 0)
        );
        MatchCalculator.MatchResult res3 = MatchCalculator.computeMatch(minScoreZeroJob, Collections.emptyList());
        assertEquals(new BigDecimal("100.0"), res3.score());
        assertFalse(res3.belowMinimum());
        assertEquals(1, res3.met().size());

        // 4. Khớp theo skillId nếu JD thiếu skillCode
        List<JobSkill> noCodeJob = List.of(
                new JobSkill(dummyJobId, "SKILL-UUID-999", null, "Custom Skill", true, 50)
        );
        List<CandidateSkillScore> candBySkillId = List.of(
                new CandidateSkillScore("SKILL-UUID-999", null, "Custom Skill", 50, null)
        );
        MatchCalculator.MatchResult res4 = MatchCalculator.computeMatch(noCodeJob, candBySkillId);
        assertEquals(new BigDecimal("100.0"), res4.score());
        assertEquals(1, res4.met().size());
    }
}

