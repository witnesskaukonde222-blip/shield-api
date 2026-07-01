package com.enterprise.shield.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class ThreatDetectionFilter extends OncePerRequestFilter {

    private static final String HONEYPOT_FORWARD_PATH = "/api/v1/decoy/taxpayers/all";

    private final ThreatDetectionEngine threatDetectionEngine;

    public ThreatDetectionFilter(ThreatDetectionEngine threatDetectionEngine) {
        this.threatDetectionEngine = threatDetectionEngine;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Never threat-check the honeypot itself, or this becomes a redirect loop.
        return request.getRequestURI().startsWith("/api/v1/decoy")
                || request.getRequestURI().startsWith("/actuator");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String clientKey = resolveClientKey(request);

        if (threatDetectionEngine.isThreatDetected(clientKey)) {
            request.getRequestDispatcher(HONEYPOT_FORWARD_PATH).forward(request, response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String resolveClientKey(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
