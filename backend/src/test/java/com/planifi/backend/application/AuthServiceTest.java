package com.planifi.backend.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.planifi.backend.api.dto.AuthResponse;
import com.planifi.backend.api.dto.LoginRequest;
import com.planifi.backend.api.dto.RegisterUserRequest;
import com.planifi.backend.domain.User;
import com.planifi.backend.infrastructure.persistence.UserRepository;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, jwtService);
    }

    @Test
    void registerCreatesUserWithHashedPassword() {
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("Sup3rS3cret!")).thenReturn("hashed");
        when(userRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtService.issueToken(any())).thenReturn(
                new JwtToken("token", OffsetDateTime.now().plusMinutes(60)));

        AuthResponse response = authService.register(
                new RegisterUserRequest("User@Example.com", "Sup3rS3cret!", "User One"));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();

        assertThat(saved.getEmail()).isEqualTo("user@example.com");
        assertThat(saved.getPasswordHash()).isEqualTo("hashed");
        assertThat(saved.getFullName()).isEqualTo("User One");
        assertThat(response.token()).isEqualTo("token");
    }

    @Test
    void registerRejectsDuplicateEmails() {
        when(userRepository.findByEmail("user@example.com"))
                .thenReturn(Optional.of(new User(
                        UUID.randomUUID(),
                        "user@example.com",
                        "hash",
                        "User",
                        OffsetDateTime.now()
                )));

        assertThatThrownBy(() -> authService.register(
                new RegisterUserRequest("user@example.com", "Sup3rS3cret!", null)))
                .isInstanceOf(EmailAlreadyRegisteredException.class);
    }

    @Test
    void loginFailsWhenPasswordDoesNotMatch() {
        User user = new User(
                UUID.randomUUID(),
                "user@example.com",
                "hash",
                "User",
                OffsetDateTime.now()
        );
        when(userRepository.findByEmail(eq("user@example.com"))).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hash")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(
                new LoginRequest("user@example.com", "wrong")))
                .isInstanceOf(InvalidCredentialsException.class);
    }
}
