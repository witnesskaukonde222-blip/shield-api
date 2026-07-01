package com.enterprise.shield.controller;

import com.enterprise.shield.dto.TaxReturn;
import com.enterprise.shield.service.TaxpayerService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/taxpayers")
public class TaxpayerController {

    private final TaxpayerService service;

    public TaxpayerController(TaxpayerService service) {
        this.service = service;
    }

    // Object-level authorization: an ADMIN can view any TIN, but a regular
    // user can only view returns matching the tin_number embedded in their own JWT.
    // This prevents Broken Object Level Authorization (BOLA / IDOR).
    @GetMapping("/{tin}/returns")
    @PreAuthorize("hasRole('ADMIN') or #tin == authentication.tokenAttributes['tin_number']")
    public ResponseEntity<List<TaxReturn>> getTaxReturns(@PathVariable String tin) {
        return ResponseEntity.ok(service.findByTin(tin));
    }
}
