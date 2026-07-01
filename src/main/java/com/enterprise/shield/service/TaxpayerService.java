package com.enterprise.shield.service;

import com.enterprise.shield.dto.TaxReturn;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class TaxpayerService {

    public List<TaxReturn> findByTin(String tin) {
        // Production implementation queries the real taxpayer/returns tables.
        return List.of(
                new TaxReturn("TR-2026-001", tin, LocalDate.of(2026, 3, 31),
                        new BigDecimal("1250.00"), "ZWG", "FILED"),
                new TaxReturn("TR-2026-002", tin, LocalDate.of(2026, 6, 30),
                        new BigDecimal("980.50"), "ZWG", "PENDING")
        );
    }
}
