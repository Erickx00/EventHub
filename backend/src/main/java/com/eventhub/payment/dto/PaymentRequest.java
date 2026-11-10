package com.eventhub.payment.dto;

import com.eventhub.payment.entity.PaymentMethod;

import jakarta.validation.constraints.NotNull;

/**
 * `outcome` existe apenas porque o pagamento e simulado: e o controle do
 * simulador, nao algo que um gateway real aceitaria. Ao integrar um provedor de
 * verdade, o campo sai do contrato e o status passa a vir do provedor.
 */
public record PaymentRequest(
        @NotNull(message = "obrigatorio") PaymentMethod method,

        @NotNull(message = "obrigatorio") PaymentOutcome outcome) {
}
