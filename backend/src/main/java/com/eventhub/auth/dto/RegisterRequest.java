package com.eventhub.auth.dto;

import com.eventhub.user.entity.Role;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Cadastro publico. O papel e opcional (padrao ATTENDEE) e ADMIN e recusado:
 * elevacao de privilegio nunca vem do corpo da requisicao.
 */
public record RegisterRequest(
        @NotBlank(message = "obrigatorio") @Size(max = 120, message = "no maximo 120 caracteres") String name,

        @NotBlank(message = "obrigatorio") @Email(message = "formato invalido") @Size(max = 180, message = "no maximo 180 caracteres") String email,

        @NotBlank(message = "obrigatorio") @Size(min = 8, max = 72, message = "entre 8 e 72 caracteres") String password,

        Role role) {
}
