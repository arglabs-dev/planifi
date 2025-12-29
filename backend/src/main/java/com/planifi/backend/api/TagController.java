package com.planifi.backend.api;

import com.planifi.backend.api.dto.CreateTagRequest;
import com.planifi.backend.api.dto.TagResponse;
import com.planifi.backend.application.InvalidCredentialsException;
import com.planifi.backend.application.TagService;
import com.planifi.backend.config.AuthenticatedApiKey;
import com.planifi.backend.config.AuthenticatedUser;
import com.planifi.backend.domain.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tags")
@Validated
public class TagController {

    private final TagService tagService;

    public TagController(TagService tagService) {
        this.tagService = tagService;
    }

    @GetMapping
    public List<TagResponse> listTags(Authentication authentication) {
        UUID userId = requireUserId(authentication);
        return tagService.listTags(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TagResponse createTag(Authentication authentication,
                                 @RequestHeader("Idempotency-Key") @NotBlank String idempotencyKey,
                                 @Valid @RequestBody CreateTagRequest request) {
        UUID userId = requireUserId(authentication);
        Tag tag = tagService.createTag(userId, request.name(), idempotencyKey);
        return toResponse(tag);
    }

    private TagResponse toResponse(Tag tag) {
        return new TagResponse(tag.getId(), tag.getName(), tag.getCreatedAt());
    }

    private UUID requireUserId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new InvalidCredentialsException();
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof AuthenticatedUser authenticatedUser) {
            return authenticatedUser.userId();
        }
        if (principal instanceof AuthenticatedApiKey authenticatedApiKey) {
            if (authenticatedApiKey.userId() == null) {
                throw new InvalidCredentialsException();
            }
            return authenticatedApiKey.userId();
        }
        throw new InvalidCredentialsException();
    }
}
