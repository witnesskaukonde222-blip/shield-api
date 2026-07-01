package com.enterprise.shield.controller;

import com.enterprise.shield.dto.AuthResponse;
import com.enterprise.shield.dto.LoginRequest;
import com.enterprise.shield.dto.RegisterRequest;
import com.enterprise.shield.model.User;
import com.enterprise.shield.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody RegisterRequest request) {
        User created = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body("Account created for " + created.getUsername());
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}
