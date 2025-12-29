package com.planifi.backend.api;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.planifi.backend.api.dto.CreateAccountRequest;
import com.planifi.backend.application.AccountService;
import com.planifi.backend.application.InvalidCredentialsException;
import com.planifi.backend.domain.AccountType;
import org.junit.jupiter.api.Test;

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
}
