package com.planifi.backend.application;

import com.planifi.backend.api.dto.AuthResponse;
import com.planifi.backend.api.dto.LoginRequest;
import com.planifi.backend.api.dto.RegisterUserRequest;
import com.planifi.backend.domain.User;
import com.planifi.backend.infrastructure.persistence.UserRepository;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthResponse register(RegisterUserRequest request) {
        String normalizedEmail = normalizeEmail(request.email());
        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new EmailAlreadyRegisteredException(normalizedEmail);
        }

        String passwordHash = passwordEncoder.encode(request.password());
        User user = new User(
                UUID.randomUUID(),
                normalizedEmail,
                passwordHash,
                request.fullName(),
                OffsetDateTime.now()
        );
        User saved = userRepository.save(user);
        return toAuthResponse(saved);
    }

    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = normalizeEmail(request.email());
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        return toAuthResponse(user);
    }

    private AuthResponse toAuthResponse(User user) {
        JwtToken token = jwtService.issueToken(user);
        return new AuthResponse(
                user.getId(),
                user.getEmail(),
                token.token(),
                "Bearer",
                token.expiresAt()
        );
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }
}
