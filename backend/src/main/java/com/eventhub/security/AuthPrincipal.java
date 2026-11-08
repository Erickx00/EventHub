package com.eventhub.security;

import java.util.UUID;

import com.eventhub.user.entity.Role;

/**
 * Identidade autenticada extraida do access token. E o principal injetado com
 * @AuthenticationPrincipal nos controllers.
 */
public record AuthPrincipal(UUID userId, String email, Role role) {
}
