package com.example.jobservice.company.domain;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CompanyRepository extends JpaRepository<Company, UUID> {
    Optional<Company> findByTaxCode(String taxCode);

    boolean existsByTaxCode(String taxCode);
}
