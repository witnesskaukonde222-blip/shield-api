package com.enterprise.shield.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TaxReturn(
        String id,
        String tinNumber,
        LocalDate periodEnding,
        BigDecimal amountDue,
        String currencyCode,
        String status
) {
}
