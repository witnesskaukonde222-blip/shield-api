package com.enterprise.shield.scheduler;

import com.enterprise.shield.model.FiscalOutbox;
import com.enterprise.shield.repository.FiscalOutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class OutboxSyncScheduler {

    private static final Logger logger = LoggerFactory.getLogger(OutboxSyncScheduler.class);
    private static final int MAX_RETRIES = 5;

    private final FiscalOutboxRepository outboxRepository;
    private final RestTemplate restTemplate;
    private final String fiscalGatewayUrl;

    public OutboxSyncScheduler(FiscalOutboxRepository outboxRepository,
                                RestTemplate restTemplate,
                                org.springframework.core.env.Environment env) {
        this.outboxRepository = outboxRepository;
        this.restTemplate = restTemplate;
        this.fiscalGatewayUrl = env.getProperty(
                "shield.fiscal-gateway.url", "https://api.gateway.mock.gov.zw/v1/fiscalise");
    }

    /**
     * Runs every 10 seconds. Designed for environments with intermittent
     * power/network availability (load-shedding): pending records survive
     * restarts because they live in PostgreSQL, not in memory.
     */
    @Scheduled(fixedDelay = 10000)
    @Transactional
    public void processPendingOutboxTransactions() {
        List<FiscalOutbox> pendingTransactions = outboxRepository
                .findTop10ByStatusAndNextAttemptAtBefore("PENDING", LocalDateTime.now());

        for (FiscalOutbox transaction : pendingTransactions) {
            try {
                var response = restTemplate.postForEntity(fiscalGatewayUrl, transaction.getPayload(), String.class);
                if (response.getStatusCode().is2xxSuccessful()) {
                    transaction.setStatus("PROCESSED");
                } else {
                    handleFailure(transaction);
                }
            } catch (Exception ex) {
                logger.warn("Outbox sync attempt failed for {}: {}", transaction.getId(), ex.getMessage());
                handleFailure(transaction);
            }
            outboxRepository.save(transaction);
        }
    }

    private void handleFailure(FiscalOutbox transaction) {
        int currentRetries = transaction.getRetryCount() + 1;
        transaction.setRetryCount(currentRetries);

        if (currentRetries >= MAX_RETRIES) {
            transaction.setStatus("FAILED"); // Parked for manual/DLQ review.
        } else {
            long delaySeconds = (long) Math.pow(2, currentRetries) * 30;
            transaction.setNextAttemptAt(LocalDateTime.now().plusSeconds(delaySeconds));
        }
    }
}
