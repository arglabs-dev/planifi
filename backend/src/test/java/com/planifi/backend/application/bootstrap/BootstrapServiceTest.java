package com.planifi.backend.application.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.planifi.backend.domain.Account;
import com.planifi.backend.domain.SystemSetting;
import com.planifi.backend.domain.User;
import com.planifi.backend.infrastructure.persistence.AccountRepository;
import com.planifi.backend.infrastructure.persistence.SystemSettingRepository;
import com.planifi.backend.infrastructure.persistence.UserRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class BootstrapServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private SystemSettingRepository systemSettingRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Captor
    private ArgumentCaptor<SystemSetting> settingCaptor;

    @Test
    void applyCreatesUsersAccountsAndSettings() {
        BootstrapConfig config = new BootstrapConfig(
                "v1",
                new BootstrapStorageConfig("local", new LocalStorageConfig("/var/lib/planifi")),
                new BootstrapSecurityConfig(new RateLimitConfig(true, 120, 20)),
                List.of(new BootstrapUserConfig(
                        "Admin@Example.com",
                        "Admin",
                        "change-me",
                        null,
                        List.of(new BootstrapAccountConfig("Principal", "mxn"))
                ))
        );

        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("change-me")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(accountRepository.findByUserIdAndNameIgnoreCase(any(), any())).thenReturn(Optional.empty());
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BootstrapService service = new BootstrapService(
                userRepository,
                accountRepository,
                systemSettingRepository,
                passwordEncoder,
                new ObjectMapper().findAndRegisterModules());

        service.apply(config);

        verify(systemSettingRepository, times(2)).save(settingCaptor.capture());
        List<SystemSetting> settings = settingCaptor.getAllValues();
        assertThat(settings).hasSize(2);
        assertThat(settings).extracting(SystemSetting::getKey)
                .containsExactlyInAnyOrder("storage", "security");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User saved = userCaptor.getValue();
        assertThat(saved.getEmail()).isEqualTo("admin@example.com");
        assertThat(saved.getPasswordHash()).isEqualTo("hashed");

        ArgumentCaptor<Account> accountCaptor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(accountCaptor.capture());
        Account account = accountCaptor.getValue();
        assertThat(account.getName()).isEqualTo("Principal");
        assertThat(account.getCurrency()).isEqualTo("MXN");
    }

    @Test
    void applyUsesPasswordHashWhenProvided() {
        UUID userId = UUID.randomUUID();
        OffsetDateTime createdAt = OffsetDateTime.now().minusDays(2);
        User existing = new User(userId, "user@example.com", "old-hash", "User", createdAt);
        Account existingAccount = new Account(UUID.randomUUID(), userId, "Principal", "MXN", createdAt);

        BootstrapConfig config = new BootstrapConfig(
                "v1",
                new BootstrapStorageConfig("local", new LocalStorageConfig("/var/lib/planifi")),
                new BootstrapSecurityConfig(new RateLimitConfig(true, 60, 10)),
                List.of(new BootstrapUserConfig(
                        "user@example.com",
                        "User Updated",
                        null,
                        "bcrypt-hash",
                        List.of(new BootstrapAccountConfig("Principal", "usd"))
                ))
        );

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(accountRepository.findByUserIdAndNameIgnoreCase(userId, "Principal"))
                .thenReturn(Optional.of(existingAccount));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BootstrapService service = new BootstrapService(
                userRepository,
                accountRepository,
                systemSettingRepository,
                passwordEncoder,
                new ObjectMapper().findAndRegisterModules());

        service.apply(config);

        verify(passwordEncoder, never()).encode(any());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User saved = userCaptor.getValue();
        assertThat(saved.getId()).isEqualTo(userId);
        assertThat(saved.getPasswordHash()).isEqualTo("bcrypt-hash");
        assertThat(saved.getFullName()).isEqualTo("User Updated");

        ArgumentCaptor<Account> accountCaptor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(accountCaptor.capture());
        Account savedAccount = accountCaptor.getValue();
        assertThat(savedAccount.getId()).isEqualTo(existingAccount.getId());
        assertThat(savedAccount.getCurrency()).isEqualTo("USD");
    }
}
