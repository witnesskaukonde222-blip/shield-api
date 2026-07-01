package com.enterprise.shield.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, length = 100)
    private String userId;

    @Column(nullable = false, length = 20)
    private String action;

    @Column(name = "target_table", nullable = false, length = 50)
    private String targetTable;

    @Column(name = "record_id", nullable = false, length = 100)
    private String recordId = "N/A";

    @Column(name = "ip_address", nullable = false, length = 45)
    private String ipAddress;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    @Column(name = "exchange_rate", nullable = false, precision = 12, scale = 4)
    private BigDecimal exchangeRate;

    @Column(name = "timestamp")
    private OffsetDateTime timestamp = OffsetDateTime.now();

    @Column(name = "previous_row_hash", length = 64)
    private String previousRowHash;

    @Column(name = "current_row_hash", nullable = false, length = 64)
    private String currentRowHash;
}
