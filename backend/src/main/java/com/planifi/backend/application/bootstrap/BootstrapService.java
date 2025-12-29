package com.planifi.backend.application.bootstrap;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.planifi.backend.domain.Account;
import com.planifi.backend.domain.AccountType;
import com.planifi.backend.domain.SystemSetting;
import com.planifi.backend.domain.User;
import com.planifi.backend.infrastructure.persistence.AccountRepository;
import com.planifi.backend.infrastructure.persistence.SystemSettingRepository;
import com.planifi.backend.infrastructure.persistence.UserRepository;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BootstrapService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final SystemSettingRepository systemSettingRepository;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;

    public BootstrapService(UserRepository userRepository,
                            AccountRepository accountRepository,
                            SystemSettingRepository systemSettingRepository,
                            PasswordEncoder passwordEncoder,
                            ObjectMapper objectMapper) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.systemSettingRepository = systemSettingRepository;
        this.passwordEncoder = passwordEncoder;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void apply(BootstrapConfig config) {
        saveSetting("storage", config.storage());
        saveSetting("security", config.security());

        for (BootstrapUserConfig userConfig : config.users()) {
            upsertUser(userConfig);
        }
    }

    private void saveSetting(String key, Object value) {
        String payload;
        try {
            payload = objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new BootstrapConfigException("No se pudo serializar la configuración de " + key, ex);
        }
        SystemSetting setting = new SystemSetting(key, payload, OffsetDateTime.now());
        systemSettingRepository.save(setting);
    }

    private void upsertUser(BootstrapUserConfig config) {
        String normalizedEmail = normalizeEmail(config.email());
        Optional<User> existing = userRepository.findByEmail(normalizedEmail);
        OffsetDateTime now = OffsetDateTime.now();
        String passwordHash = resolvePasswordHash(config, existing.orElse(null));

        User user = existing
                .map(current -> new User(current.getId(), normalizedEmail, passwordHash,
                        resolveFullName(config, current), current.getCreatedAt()))
                .orElseGet(() -> new User(UUID.randomUUID(), normalizedEmail, passwordHash,
                        config.fullName(), now));

        User saved = userRepository.save(user);
        for (BootstrapAccountConfig accountConfig : config.accounts()) {
            upsertAccount(saved, accountConfig);
        }
    }

    private void upsertAccount(User user, BootstrapAccountConfig config) {
        Optional<Account> existing = accountRepository
                .findByUserIdAndNameIgnoreCase(user.getId(), config.name());
        OffsetDateTime now = OffsetDateTime.now();
        Account account = existing
                .map(current -> new Account(current.getId(), user.getId(), config.name(),
                        current.getType(), config.currency().toUpperCase(Locale.ROOT),
                        current.getCreatedAt(), current.getDisabledAt()))
                .orElseGet(() -> new Account(UUID.randomUUID(), user.getId(), config.name(),
                        AccountType.CASH, config.currency().toUpperCase(Locale.ROOT), now, null));
        accountRepository.save(account);
    }

    private String resolvePasswordHash(BootstrapUserConfig config, User existing) {
        if (config.passwordHash() != null && !config.passwordHash().isBlank()) {
            return config.passwordHash();
        }
        if (config.password() != null && !config.password().isBlank()) {
            return passwordEncoder.encode(config.password());
        }
        if (existing != null) {
            return existing.getPasswordHash();
        }
        throw new BootstrapConfigException("Falta password para usuario: " + config.email());
    }

    private String resolveFullName(BootstrapUserConfig config, User existing) {
        String fullName = config.fullName();
        if (fullName != null && !fullName.isBlank()) {
            return fullName;
        }
        return existing.getFullName();
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
