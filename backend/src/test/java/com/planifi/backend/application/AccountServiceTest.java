package com.planifi.backend.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.planifi.backend.domain.Account;
import com.planifi.backend.domain.AccountType;
import com.planifi.backend.domain.IdempotencyKey;
import com.planifi.backend.infrastructure.persistence.AccountRepository;
import com.planifi.backend.infrastructure.persistence.IdempotencyKeyRepository;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private IdempotencyKeyRepository idempotencyKeyRepository;

    private AccountService accountService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();
        accountService = new AccountService(accountRepository, idempotencyKeyRepository, objectMapper);
    }

    @Test
    void createAccountPersistsWithDefaults() {
        UUID userId = UUID.randomUUID();
        when(idempotencyKeyRepository.findByIdempotencyKey("idem-1")).thenReturn(Optional.empty());
        when(accountRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Account account = accountService.createAccount(userId, "Cuenta nómina",
                AccountType.BANK, "idem-1");

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(captor.capture());
        Account saved = captor.getValue();

        assertThat(saved.getUserId()).isEqualTo(userId);
        assertThat(saved.getName()).isEqualTo("Cuenta nómina");
        assertThat(saved.getType()).isEqualTo(AccountType.BANK);
        assertThat(saved.getCurrency()).isEqualTo("MXN");
        assertThat(account.getId()).isNotNull();
    }

    @Test
    void createAccountReplaysStoredResponse() throws Exception {
        UUID userId = UUID.randomUUID();
        Account stored = new Account(
                UUID.randomUUID(),
                userId,
                "Cuenta cash",
                AccountType.CASH,
                "MXN",
                OffsetDateTime.parse("2024-09-10T12:00:00Z"),
                null
        );
        String responseBody = objectMapper.writeValueAsString(stored);
        IdempotencyKey record = new IdempotencyKey(
                UUID.randomUUID(),
                "idem-2",
                accountService.hashRequest("create-account", userId.toString(), "Cuenta cash",
                        AccountType.CASH.name()),
                responseBody,
                "COMPLETED",
                OffsetDateTime.now());

        when(idempotencyKeyRepository.findByIdempotencyKey("idem-2"))
                .thenReturn(Optional.of(record));

        Account response = accountService.createAccount(userId, "Cuenta cash",
                AccountType.CASH, "idem-2");

        assertThat(response).usingRecursiveComparison().isEqualTo(stored);
        verify(accountRepository, never()).save(any());
    }

    @Test
    void disableAccountFailsWhenMissing() {
        UUID userId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        when(idempotencyKeyRepository.findByIdempotencyKey("idem-3")).thenReturn(Optional.empty());
        when(accountRepository.findByIdAndUserId(accountId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.disableAccount(userId, accountId, "idem-3"))
                .isInstanceOf(AccountNotFoundException.class);
    }

    @Test
    void rejectsIdempotencyKeyReuseWithDifferentPayload() {
        UUID userId = UUID.randomUUID();
        IdempotencyKey record = new IdempotencyKey(
                UUID.randomUUID(),
                "idem-4",
                accountService.hashRequest("create-account", userId.toString(), "Cuenta cash",
                        AccountType.CASH.name()),
                null,
                "COMPLETED",
                OffsetDateTime.now());
        when(idempotencyKeyRepository.findByIdempotencyKey("idem-4"))
                .thenReturn(Optional.of(record));

        assertThatThrownBy(() -> accountService.createAccount(userId, "Cuenta nómina",
                AccountType.BANK, "idem-4"))
                .isInstanceOf(IdempotencyKeyReuseException.class);
    }
}
