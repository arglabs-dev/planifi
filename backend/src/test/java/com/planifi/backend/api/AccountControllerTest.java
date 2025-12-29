package com.planifi.backend.api;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.planifi.backend.api.dto.CreateAccountRequest;
import com.planifi.backend.application.AccountService;
import com.planifi.backend.application.InvalidCredentialsException;
import com.planifi.backend.config.AuthenticatedApiKey;
import com.planifi.backend.domain.Account;
import com.planifi.backend.domain.AccountType;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

class AccountControllerTest {

    @Test
    void listRequiresAuthenticatedUser() {
        AccountService accountService = mock(AccountService.class);
        AccountController controller = new AccountController(accountService);

        assertThatThrownBy(() -> controller.listAccounts(null))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void createRequiresAuthenticatedUser() {
        AccountService accountService = mock(AccountService.class);
        AccountController controller = new AccountController(accountService);

        assertThatThrownBy(() -> controller.createAccount(
                null,
                "idem-12345678",
                new CreateAccountRequest("Cuenta nómina", AccountType.BANK)))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void listAcceptsAuthenticatedApiKey() {
        AccountService accountService = mock(AccountService.class);
        AccountController controller = new AccountController(accountService);
        UUID userId = UUID.randomUUID();
        AuthenticatedApiKey apiKey = new AuthenticatedApiKey(UUID.randomUUID(), userId);
        Authentication authentication = new UsernamePasswordAuthenticationToken(apiKey, null, List.of());

        when(accountService.listActiveAccounts(userId)).thenReturn(List.of());

        controller.listAccounts(authentication);

        verify(accountService).listActiveAccounts(userId);
    }

    @Test
    void createAcceptsAuthenticatedApiKey() {
        AccountService accountService = mock(AccountService.class);
        AccountController controller = new AccountController(accountService);
        UUID userId = UUID.randomUUID();
        AuthenticatedApiKey apiKey = new AuthenticatedApiKey(UUID.randomUUID(), userId);
        Authentication authentication = new UsernamePasswordAuthenticationToken(apiKey, null, List.of());
        Account account = new Account(
                UUID.randomUUID(),
                userId,
                "Cuenta nómina",
                AccountType.BANK,
                "MXN",
                OffsetDateTime.parse("2024-09-10T12:00:00Z"),
                null
        );

        when(accountService.createAccount(userId, "Cuenta nómina", AccountType.BANK, "idem-12345678"))
                .thenReturn(account);

        controller.createAccount(authentication,
                "idem-12345678",
                new CreateAccountRequest("Cuenta nómina", AccountType.BANK));

        verify(accountService).createAccount(userId, "Cuenta nómina", AccountType.BANK, "idem-12345678");
    }

    @Test
    void apiKeyWithoutUserIdIsRejected() {
        AccountService accountService = mock(AccountService.class);
        AccountController controller = new AccountController(accountService);
        AuthenticatedApiKey apiKey = new AuthenticatedApiKey(UUID.randomUUID(), null);
        Authentication authentication = new UsernamePasswordAuthenticationToken(apiKey, null, List.of());

        assertThatThrownBy(() -> controller.listAccounts(authentication))
                .isInstanceOf(InvalidCredentialsException.class);
    }
}
