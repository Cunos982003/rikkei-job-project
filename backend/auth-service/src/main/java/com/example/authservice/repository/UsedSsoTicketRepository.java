package com.example.authservice.repository;

import com.example.authservice.entity.UsedSsoTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UsedSsoTicketRepository extends JpaRepository<UsedSsoTicket, Long> {
    boolean existsByTicket(String ticket);
}
