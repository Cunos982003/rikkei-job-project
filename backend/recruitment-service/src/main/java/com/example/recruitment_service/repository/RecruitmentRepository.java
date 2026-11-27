package com.example.recruitment_service.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.recruitment_service.entity.Recruitment;
import com.example.recruitment_service.enums.RecruitmentStatus;

import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;

public interface RecruitmentRepository
        extends JpaRepository<Recruitment, UUID>, JpaSpecificationExecutor<Recruitment> {
    @Query("select r from Recruitment r where r.user_id = :userId order by r.created_at desc")
    List<Recruitment> findForUser(@Param("userId") UUID userId);

    @Query("select r from Recruitment r where r.user_id = :userId and r.status = :status order by r.created_at desc")
    List<Recruitment> findForUserAndStatus(
            @Param("userId") UUID userId,
            @Param("status") RecruitmentStatus status);

    @Query("select r from Recruitment r where r.id = :id and r.user_id = :userId")
    Optional<Recruitment> findForUserById(@Param("id") UUID id, @Param("userId") UUID userId);

    @Query("select case when count(r) > 0 then true else false end from Recruitment r "
            + "where r.job_id = :jobId and r.user_id = :userId and r.status <> :withdrawn")
    boolean existsActiveByJobAndUser(
            @Param("jobId") UUID jobId,
            @Param("userId") UUID userId,
            @Param("withdrawn") RecruitmentStatus withdrawn);

    default Page<Recruitment> findForStaff(
            UUID companyId,
            UUID jobId,
            RecruitmentStatus status,
            Pageable pageable) {
        Specification<Recruitment> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (companyId != null) {
                predicates.add(cb.equal(root.get("company_id"), companyId));
            }
            if (jobId != null) {
                predicates.add(cb.equal(root.get("job_id"), jobId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (query != null && query.getResultType() != Long.class && query.getResultType() != long.class
                    && pageable != null && pageable.getSort().isSorted()) {
                List<Order> orders = new ArrayList<>();
                for (Sort.Order sortOrder : pageable.getSort()) {
                    var expr = root.get(sortOrder.getProperty());
                    orders.add(sortOrder.isAscending() ? cb.asc(expr) : cb.desc(expr));
                }
                query.orderBy(orders);
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        Pageable unsorted = pageable != null && pageable.isPaged()
                ? PageRequest.of(pageable.getPageNumber(), pageable.getPageSize())
                : Pageable.unpaged();
        return findAll(spec, unsorted);
    }
}
