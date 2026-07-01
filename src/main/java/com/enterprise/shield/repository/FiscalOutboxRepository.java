package com.enterprise.shield.repository;

import com.enterprise.shield.model.FiscalOutbox;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface FiscalOutboxRepository extends JpaRepository<FiscalOutbox, UUID> {
    List<FiscalOutbox> findTop10ByStatusAndNextAttemptAtBefore(String status, LocalDateTime before);
}
