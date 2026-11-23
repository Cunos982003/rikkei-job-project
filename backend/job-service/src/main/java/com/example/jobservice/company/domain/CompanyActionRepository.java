package com.example.jobservice.company.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CompanyActionRepository extends JpaRepository<CompanyAction, UUID> {

    Page<CompanyAction> findByCompanyId(UUID companyId, Pageable pageable);
}

