package com.example.jobservice.match.service;

import com.example.jobservice.job.domain.JobSkill;
import com.example.jobservice.match.client.CandidateSkillScore;
import com.example.jobservice.match.dto.MatchSkillItem;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Lớp tính toán thuần túy (không phụ thuộc Spring/DB) thực hiện công thức khớp kỹ năng DN-06.
 */
public class MatchCalculator {

    public record MatchResult(
            BigDecimal score,
            boolean belowMinimum,
            List<MatchSkillItem> met,
            List<MatchSkillItem> missing
    ) {
    }

    private static final Comparator<MatchSkillItem> SKILL_ITEM_COMPARATOR = Comparator
            .comparing((MatchSkillItem item) -> !item.required()) // Bắt buộc (true -> false) trước ưu tiên (false -> true)
            .thenComparing(item -> item.skill() != null ? item.skill() : "", String.CASE_INSENSITIVE_ORDER)
            .thenComparing(item -> item.skillCode() != null ? item.skillCode() : "", String.CASE_INSENSITIVE_ORDER);

    public static MatchResult computeMatch(List<JobSkill> jobSkills, List<CandidateSkillScore> candidateSkills) {
        if (jobSkills == null || jobSkills.isEmpty()) {
            // JD không có kỹ năng nào -> điểm = 0, belowMinimum = false, met và missing rỗng
            return new MatchResult(
                    BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP),
                    false,
                    Collections.emptyList(),
                    Collections.emptyList()
            );
        }

        List<CandidateSkillScore> candSkills = candidateSkills != null ? candidateSkills : Collections.emptyList();

        List<BigDecimal> reqRatios = new ArrayList<>();
        List<BigDecimal> optRatios = new ArrayList<>();

        List<MatchSkillItem> metList = new ArrayList<>();
        List<MatchSkillItem> missingList = new ArrayList<>();

        boolean belowMinimum = false;

        for (JobSkill js : jobSkills) {
            // 1. Khớp kỹ năng theo skillCode (không phân biệt hoa thường), nếu JD thiếu skillCode thì khớp theo skillId
            CandidateSkillScore matchedCandSkill = findMatchingCandidateSkill(js, candSkills);

            // 2. candidateScore = verifiedScore của ứng viên; null hoặc không có kỹ năng = 0. TUYỆT ĐỐI KHÔNG dùng selfScore.
            int candidateScore = 0;
            if (matchedCandSkill != null && matchedCandSkill.verifiedScore() != null) {
                candidateScore = matchedCandSkill.verifiedScore();
            }

            int minScore = js.getMinScore();

            // 3. skillRatio = min(candidateScore / minScore, 1.0); nếu minScore == 0 thì skillRatio = 1.0.
            BigDecimal ratio;
            if (minScore <= 0) {
                ratio = BigDecimal.ONE;
            } else {
                BigDecimal candBd = BigDecimal.valueOf(candidateScore);
                BigDecimal minBd = BigDecimal.valueOf(minScore);
                ratio = candBd.divide(minBd, 10, RoundingMode.HALF_UP);
                if (ratio.compareTo(BigDecimal.ONE) > 0) {
                    ratio = BigDecimal.ONE;
                }
            }

            if (js.isRequired()) {
                reqRatios.add(ratio);

                // belowMinimum = true nếu tồn tại kỹ năng required=true có minScore>0 và candidateScore < 0.5*minScore
                // Nghiêm ngặt: đúng bằng 50% thì KHÔNG phải dưới (so sánh candidateScore * 2 < minScore)
                if (minScore > 0 && (candidateScore * 2 < minScore)) {
                    belowMinimum = true;
                }
            } else {
                optRatios.add(ratio);
            }

            // 4. met = candidateScore >= minScore; missing = còn lại, gap = minScore - candidateScore
            if (candidateScore >= minScore) {
                metList.add(new MatchSkillItem(
                        js.getSkillName(),
                        js.getSkillCode(),
                        js.isRequired(),
                        minScore,
                        candidateScore,
                        0
                ));
            } else {
                int gap = minScore - candidateScore;
                missingList.add(new MatchSkillItem(
                        js.getSkillName(),
                        js.getSkillCode(),
                        js.isRequired(),
                        minScore,
                        candidateScore,
                        gap
                ));
            }
        }

        // 5. Tính điểm tổng theo trọng số
        // TBreq = trung bình skillRatio các kỹ năng required=true; TBopt = trung bình các kỹ năng required=false
        // score = 70*TBreq + 30*TBopt. Không có kỹ năng ưu tiên: 100*TBreq. Không có kỹ năng bắt buộc nhưng có ưu tiên: 100*TBopt.
        BigDecimal totalScore;
        if (!reqRatios.isEmpty() && !optRatios.isEmpty()) {
            BigDecimal sumReq = reqRatios.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal tbReq = sumReq.divide(BigDecimal.valueOf(reqRatios.size()), 10, RoundingMode.HALF_UP);

            BigDecimal sumOpt = optRatios.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal tbOpt = sumOpt.divide(BigDecimal.valueOf(optRatios.size()), 10, RoundingMode.HALF_UP);

            totalScore = tbReq.multiply(BigDecimal.valueOf(70)).add(tbOpt.multiply(BigDecimal.valueOf(30)));
        } else if (!reqRatios.isEmpty()) {
            BigDecimal sumReq = reqRatios.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal tbReq = sumReq.divide(BigDecimal.valueOf(reqRatios.size()), 10, RoundingMode.HALF_UP);

            totalScore = tbReq.multiply(BigDecimal.valueOf(100));
        } else if (!optRatios.isEmpty()) {
            BigDecimal sumOpt = optRatios.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal tbOpt = sumOpt.divide(BigDecimal.valueOf(optRatios.size()), 10, RoundingMode.HALF_UP);

            totalScore = tbOpt.multiply(BigDecimal.valueOf(100));
        } else {
            totalScore = BigDecimal.ZERO;
        }

        // Làm tròn 1 chữ số thập phân (HALF_UP)
        BigDecimal finalScore = totalScore.setScale(1, RoundingMode.HALF_UP);

        // 6. Sắp xếp met và missing: bắt buộc trước, ưu tiên sau, rồi theo tên
        metList.sort(SKILL_ITEM_COMPARATOR);
        missingList.sort(SKILL_ITEM_COMPARATOR);

        return new MatchResult(finalScore, belowMinimum, metList, missingList);
    }

    private static CandidateSkillScore findMatchingCandidateSkill(JobSkill js, List<CandidateSkillScore> candidateSkills) {
        String jobCode = js.getSkillCode();
        if (jobCode != null && !jobCode.isBlank()) {
            String targetCode = jobCode.trim();
            for (CandidateSkillScore cs : candidateSkills) {
                if (cs.skillCode() != null && cs.skillCode().trim().equalsIgnoreCase(targetCode)) {
                    return cs;
                }
            }
        }

        String jobId = js.getSkillId();
        if (jobId != null && !jobId.isBlank()) {
            String targetId = jobId.trim();
            for (CandidateSkillScore cs : candidateSkills) {
                if (cs.skillId() != null && cs.skillId().trim().equalsIgnoreCase(targetId)) {
                    return cs;
                }
            }
        }

        return null;
    }
}

