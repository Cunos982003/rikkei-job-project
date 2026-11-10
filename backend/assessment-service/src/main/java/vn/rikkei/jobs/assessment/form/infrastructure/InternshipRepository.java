package vn.rikkei.jobs.assessment.form.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.rikkei.jobs.assessment.form.domain.Internship;

public interface InternshipRepository extends JpaRepository<Internship, UUID> {

    List<Internship> findByMentorIdOrderByDueAtAsc(UUID mentorId);

    List<Internship> findByMentorIdAndBatchIdOrderByDueAtAsc(UUID mentorId, UUID batchId);

    Optional<Internship> findByIdAndMentorId(UUID id, UUID mentorId);
}
