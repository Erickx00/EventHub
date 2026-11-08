package com.eventhub.auth.dto;

import com.eventhub.user.dto.UserResponse;

/**
 * @param expiresIn validade do access token em segundos; o frontend renova
 *                  antes disso com o refresh token
 */
public record AuthResponse(String tokenType, String accessToken, String refreshToken, long expiresIn,
        UserResponse user) {
}
