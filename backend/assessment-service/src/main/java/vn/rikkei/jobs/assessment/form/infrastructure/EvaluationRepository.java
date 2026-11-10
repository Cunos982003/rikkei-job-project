package vn.rikkei.jobs.assessment.form.infrastructure;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.rikkei.jobs.assessment.form.domain.Evaluation;
import vn.rikkei.jobs.assessment.form.domain.EvaluationContext;
import vn.rikkei.jobs.assessment.form.domain.EvaluationStatus;

public interface EvaluationRepository extends JpaRepository<Evaluation, UUID> {

    Optional<Evaluation> findByContextAndSubjectIdAndMentorId(
            EvaluationContext context, UUID subjectId, UUID mentorId);

    List<Evaluation> findByMentorIdAndContextAndSubjectIdIn(
            UUID mentorId, EvaluationContext context, Collection<UUID> subjectIds);

    long countByMentorIdAndStatus(UUID mentorId, EvaluationStatus status);

    @Query(
            """
            select e from Evaluation e
            left join fetch e.items
            where e.id = :id
            """)
    Optional<Evaluation> findByIdWithItems(@Param("id") UUID id);
}
