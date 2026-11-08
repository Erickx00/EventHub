package com.eventhub.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank(message = "obrigatorio") String currentPassword,

        @NotBlank(message = "obrigatorio") @Size(min = 8, max = 72, message = "entre 8 e 72 caracteres") String newPassword) {
}
