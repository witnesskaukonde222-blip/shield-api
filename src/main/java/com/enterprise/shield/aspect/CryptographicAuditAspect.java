package com.enterprise.shield.aspect;

import com.enterprise.shield.model.AuditLog;
import com.enterprise.shield.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Aspect
@Component
public class CryptographicAuditAspect {

    private final AuditLogRepository auditRepository;
    private final String auditSecret;

    public CryptographicAuditAspect(AuditLogRepository auditRepository,
                                     org.springframework.core.env.Environment env) {
        this.auditRepository = auditRepository;
        // Secret should be supplied via SHIELD_AUDIT_SECRET in production - never hardcode it.
        this.auditSecret = env.getProperty("shield.audit.secret", "CHANGE_ME_IN_PRODUCTION");
    }

    @Around("execution(* com.enterprise.shield.repository.*.save*(..)) || " +
            "execution(* com.enterprise.shield.repository.*.delete*(..))")
    public Object profileRepositoryMutation(ProceedingJoinPoint joinPoint) throws Throwable {
        Object result = joinPoint.proceed();

        // Avoid infinite recursion: never audit writes to the audit_logs table itself.
        if (joinPoint.getTarget() instanceof AuditLogRepository) {
            return result;
        }

        try {
            String currentUsername = resolveCurrentUsername();
            String clientIp = resolveClientIp();
            String targetTable = joinPoint.getTarget().getClass().getSimpleName();
            String actionType = joinPoint.getSignature().getName().toUpperCase().contains("DELETE") ? "DELETE" : "WRITE";

            Optional<AuditLog> latestLog = auditRepository.findFirstByOrderByIdDesc();
            String previousHash = latestLog.map(AuditLog::getCurrentRowHash)
                    .orElse("0".repeat(64));

            AuditLog newLog = new AuditLog();
            newLog.setUserId(currentUsername);
            newLog.setAction(actionType);
            newLog.setTargetTable(targetTable);
            newLog.setIpAddress(clientIp);
            newLog.setPreviousRowHash(previousHash);
            newLog.setCurrencyCode("ZWG");
            newLog.setExchangeRate(fetchCurrentInterbankRate());

            String dataToHash = currentUsername + actionType + targetTable + clientIp + previousHash;
            newLog.setCurrentRowHash(calculateHmacSha256(dataToHash));

            auditRepository.save(newLog);
        } catch (Exception auditFailure) {
            // Audit logging must never break the primary business transaction;
            // failures here are surfaced to monitoring instead of propagated.
            org.slf4j.LoggerFactory.getLogger(CryptographicAuditAspect.class)
                    .error("Audit log capture failed", auditFailure);
        }

        return result;
    }

    private String resolveCurrentUsername() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null ? authentication.getName() : "SYSTEM";
    }

    private String resolveClientIp() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return "INTERNAL";
        }
        HttpServletRequest request = attributes.getRequest();
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String calculateHmacSha256(String data) throws Exception {
        Mac sha256Hmac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey = new SecretKeySpec(auditSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        sha256Hmac.init(secretKey);
        byte[] hashBytes = sha256Hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : hashBytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private BigDecimal fetchCurrentInterbankRate() {
        // Production implementation calls the central bank / interbank rate API.
        return new BigDecimal("13.5612");
    }
}
