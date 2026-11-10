package vn.rikkei.jobs.assessment.form.infrastructure;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.rikkei.jobs.assessment.form.domain.EvaluationContext;
import vn.rikkei.jobs.assessment.form.domain.EvaluationCriteria;

public interface EvaluationCriteriaRepository extends JpaRepository<EvaluationCriteria, UUID> {

    List<EvaluationCriteria> findByContextOrderBySortOrderAsc(EvaluationContext context);
}
