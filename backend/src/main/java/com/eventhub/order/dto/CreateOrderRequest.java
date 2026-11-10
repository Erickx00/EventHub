package com.eventhub.order.dto;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * O cliente envia evento e itens (tipo + quantidade). Preco e total sao
 * calculados no backend a partir do banco.
 */
public record CreateOrderRequest(
        @NotNull(message = "obrigatorio") UUID eventId,

        @NotEmpty(message = "informe ao menos um ingresso") @Size(max = 10, message = "no maximo 10 tipos por pedido") @Valid List<Item> items) {

    public record Item(
            @NotNull(message = "obrigatorio") UUID ticketTypeId,

            @NotNull(message = "obrigatorio") @jakarta.validation.constraints.Min(value = 1, message = "deve ser maior que zero") Integer quantity) {
    }
}
