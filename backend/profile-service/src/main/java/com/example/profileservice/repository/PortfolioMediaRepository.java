package com.example.profileservice.repository;

import com.example.profileservice.entity.PortfolioMedia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PortfolioMediaRepository extends JpaRepository<PortfolioMedia, UUID> {

    List<PortfolioMedia> findByProjectIdOrderBySortOrderAsc(UUID projectId);

    void deleteByProjectId(UUID projectId);
}