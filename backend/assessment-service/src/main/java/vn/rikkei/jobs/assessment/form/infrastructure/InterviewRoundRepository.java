package vn.rikkei.jobs.assessment.form.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.rikkei.jobs.assessment.form.domain.InterviewRound;

public interface InterviewRoundRepository extends JpaRepository<InterviewRound, UUID> {

    List<InterviewRound> findByInterviewerIdOrderByDueAtAsc(UUID interviewerId);

    List<InterviewRound> findByInterviewerIdAndBatchIdOrderByDueAtAsc(UUID interviewerId, UUID batchId);

    Optional<InterviewRound> findByIdAndInterviewerId(UUID id, UUID interviewerId);
}
