package com.enterprise.shield.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/decoy")
public class HoneypotDecoyController {

    private static final Logger logger = LoggerFactory.getLogger(HoneypotDecoyController.class);

    @RequestMapping("/taxpayers/all")
    public ResponseEntity<String> getFakeEnterpriseData(HttpServletRequest request) {
        String cefLog = String.format(
                "CEF:0|EnterpriseSecurity|ThreatShield|1.0|SRC_BLOCKED|Honeypot Decoy Triggered|9|" +
                        "src=%s msg=Client exceeded rate threshold. Rerouted to synthetic tax data.",
                request.getRemoteAddr()
        );
        logger.warn(cefLog);

        String syntheticPayload = """
                [
                    {"id": "d83a-491b", "taxpayer_name": "Mock Enterprise Ltd", "tin": "10029311", "balance_due": 0.00},
                    {"id": "a11e-920c", "taxpayer_name": "Synthesized Logistics", "tin": "10948112", "balance_due": 45000.12}
                ]
                """;
        return ResponseEntity.ok(syntheticPayload);
    }
}
