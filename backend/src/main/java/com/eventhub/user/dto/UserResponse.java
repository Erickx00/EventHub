package com.eventhub.user.dto;

import java.time.Instant;
import java.util.UUID;

import com.eventhub.user.entity.Role;
import com.eventhub.user.entity.User;

/**
 * Representacao publica do usuario. Nunca inclui hash de senha.
 */
public record UserResponse(UUID id, String name, String email, Role role, Instant createdAt) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole(), user.getCreatedAt());
    }
}
