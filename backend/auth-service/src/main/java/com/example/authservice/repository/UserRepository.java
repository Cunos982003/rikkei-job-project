package com.example.authservice.repository;

import com.example.authservice.entity.User;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByLmsUserId(String lmsUserId);
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("SELECT u FROM User u WHERE u.companyId = :companyId AND u.role = com.example.authservice.entity.UserRole.HR ORDER BY u.createdAt DESC")
    List<User> findHrAccountsByCompanyId(@Param("companyId") UUID companyId);

    @Query("SELECT u.companyId, COUNT(u) FROM User u WHERE u.companyId IN :companyIds AND u.role = com.example.authservice.entity.UserRole.HR GROUP BY u.companyId")
    List<Object[]> countHrAccountsByCompanyIds(@Param("companyIds") Collection<UUID> companyIds);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.email = :email")
    Optional<User> findByEmailForUpdate(@Param("email") String email);
}
