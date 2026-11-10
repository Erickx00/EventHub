package com.eventhub.ticket.dto;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateTicketTypeRequest(
        @NotBlank(message = "obrigatorio") @Size(max = 80, message = "no maximo 80 caracteres") String name,

        @Size(max = 400, message = "no maximo 400 caracteres") String description,

        @NotNull(message = "obrigatorio") @DecimalMin(value = "0.00", message = "nao pode ser negativo") @Digits(integer = 10, fraction = 2, message = "formato invalido") BigDecimal price,

        @NotNull(message = "obrigatorio") @Min(value = 1, message = "deve ser maior que zero") Integer totalQuantity,

        @NotNull(message = "obrigatorio") @Min(value = 1, message = "deve ser maior que zero") Integer maxPerOrder,

        @NotNull(message = "obrigatorio") Instant salesStart,

        @NotNull(message = "obrigatorio") Instant salesEnd) {
}
