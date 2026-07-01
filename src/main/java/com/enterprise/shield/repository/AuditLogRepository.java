package com.enterprise.shield.repository;

import com.enterprise.shield.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    Optional<AuditLog> findFirstByOrderByIdDesc();
}
