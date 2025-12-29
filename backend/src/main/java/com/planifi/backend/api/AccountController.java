package com.planifi.backend.api;

import com.planifi.backend.api.dto.AccountResponse;
import com.planifi.backend.api.dto.CreateAccountRequest;
import com.planifi.backend.application.AccountService;
import com.planifi.backend.application.InvalidCredentialsException;
import com.planifi.backend.config.AuthenticatedUser;
import com.planifi.backend.domain.Account;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/accounts")
@Validated
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping
    public List<AccountResponse> listAccounts(@AuthenticationPrincipal AuthenticatedUser user) {
        AuthenticatedUser authenticatedUser = requireUser(user);
        return accountService.listActiveAccounts(authenticatedUser.userId()).stream()
                .map(this::toResponse)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse createAccount(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestHeader("Idempotency-Key") @NotBlank String idempotencyKey,
            @Valid @RequestBody CreateAccountRequest request) {
        AuthenticatedUser authenticatedUser = requireUser(user);
        Account account = accountService.createAccount(authenticatedUser.userId(),
                request.name(), request.type(), idempotencyKey);
        return toResponse(account);
    }

    @PostMapping("/{accountId}/disable")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void disableAccount(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestHeader("Idempotency-Key") @NotBlank String idempotencyKey,
            @PathVariable UUID accountId) {
        AuthenticatedUser authenticatedUser = requireUser(user);
        accountService.disableAccount(authenticatedUser.userId(), accountId, idempotencyKey);
    }

    private AccountResponse toResponse(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getName(),
                account.getType(),
                account.getCurrency(),
                account.getCreatedAt()
        );
    }

    private AuthenticatedUser requireUser(AuthenticatedUser user) {
        if (user == null) {
            throw new InvalidCredentialsException();
        }
        return user;
    }
}
