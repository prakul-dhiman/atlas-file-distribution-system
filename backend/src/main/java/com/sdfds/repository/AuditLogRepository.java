package com.sdfds.repository;

import com.sdfds.entity.AuditLog;
import com.sdfds.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    Page<AuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<AuditLog> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    Page<AuditLog> findByEventTypeOrderByCreatedAtDesc(String eventType, Pageable pageable);

    Page<AuditLog> findBySeverityOrderByCreatedAtDesc(String severity, Pageable pageable);

    @Query("SELECT a FROM AuditLog a WHERE a.createdAt >= :from AND a.createdAt <= :to ORDER BY a.createdAt DESC")
    List<AuditLog> findByDateRange(@Param("from") Instant from, @Param("to") Instant to);

    @Query("SELECT a FROM AuditLog a WHERE a.user = :user AND a.createdAt >= :from AND a.createdAt <= :to ORDER BY a.createdAt DESC")
    List<AuditLog> findByUserAndDateRange(@Param("user") User user, @Param("from") Instant from, @Param("to") Instant to);

    long countBySeverity(String severity);

    long countByCreatedAtAfter(Instant since);
}