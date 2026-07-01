package com.enterprise.shield.service;

import com.enterprise.shield.dto.AuthResponse;
import com.enterprise.shield.dto.LoginRequest;
import com.enterprise.shield.dto.RegisterRequest;
import com.enterprise.shield.model.Role;
import com.enterprise.shield.model.User;
import com.enterprise.shield.repository.RoleRepository;
import com.enterprise.shield.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class AuthService {

    private static final long TOKEN_VALIDITY_SECONDS = 3600;

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final AuthenticationManager authenticationManager;

    public AuthService(UserRepository userRepository,
                        RoleRepository roleRepository,
                        PasswordEncoder passwordEncoder,
                        JwtEncoder jwtEncoder,
                        AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
        this.authenticationManager = authenticationManager;
    }

    public User register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new IllegalArgumentException("Username already taken");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already registered");
        }

        User user = new User();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setTinNumber(request.tinNumber());

        Role internRole = roleRepository.findByName("ROLE_INTERN")
                .orElseThrow(() -> new IllegalStateException("Default role missing - run migrations"));
        Set<Role> roles = new HashSet<>();
        roles.add(internRole);
        user.setRoles(roles);

        return userRepository.save(user);
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );

        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

        Instant now = Instant.now();
        String scope = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.joining(" "));

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("shield-api")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(TOKEN_VALIDITY_SECONDS))
                .subject(user.getUsername())
                .claim("roles", Stream.of(scope.split(" ")).filter(s -> !s.isBlank()).collect(Collectors.toList()))
                .claim("tin_number", user.getTinNumber() == null ? "" : user.getTinNumber())
                .build();

        String token = jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(org.springframework.security.oauth2.jose.jws.SignatureAlgorithm.RS256).build(),
                claims
        )).getTokenValue();

        return AuthResponse.bearer(token, TOKEN_VALIDITY_SECONDS);
    }
}
