package vn.rikkei.jobs.assessment.form.infrastructure;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.rikkei.jobs.assessment.form.domain.InternshipBatch;

public interface InternshipBatchRepository extends JpaRepository<InternshipBatch, UUID> {}
