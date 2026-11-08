package com.eventhub.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "obrigatorio") @Email(message = "formato invalido") String email,
        @NotBlank(message = "obrigatorio") String password) {
}
