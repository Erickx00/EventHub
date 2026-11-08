package com.eventhub.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Corpo de /refresh e /logout: em ambos o unico dado e o refresh token opaco.
 */
public record TokenRequest(@NotBlank(message = "obrigatorio") String refreshToken) {
}
