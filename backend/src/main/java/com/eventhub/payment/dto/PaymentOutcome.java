package com.eventhub.payment.dto;

/**
 * Resultado pretendido do pagamento simulado. Um provedor real nao recebe isso
 * do cliente: o status vem da resposta dele.
 */
public enum PaymentOutcome {
    APPROVED,
    REFUSED
}
