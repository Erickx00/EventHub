package com.eventhub.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank(message = "obrigatorio") @Size(max = 120, message = "no maximo 120 caracteres") String name) {
}
