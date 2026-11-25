package com.example.profileservice.repository;

import com.example.profileservice.entity.ContactAccessLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ContactAccessLogRepository extends JpaRepository<ContactAccessLog, UUID> {

    List<ContactAccessLog> findByCompanyId(UUID companyId);

    List<ContactAccessLog> findByHrId(UUID hrId);

    List<ContactAccessLog> findByUserId(UUID userId);

    @Query("SELECT cal FROM ContactAccessLog cal WHERE cal.companyId = :companyId AND cal.userId = :userId")
    List<ContactAccessLog> findByCompanyIdAndUserId(@Param("companyId") UUID companyId, @Param("userId") UUID userId);

    @Query("SELECT cal FROM ContactAccessLog cal WHERE cal.createdAt BETWEEN :from AND :to ORDER BY cal.createdAt DESC")
    List<ContactAccessLog> findByDateRange(@Param("from") OffsetDateTime from, @Param("to") OffsetDateTime to);
}