package com.eventhub.payment.gateway;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import com.eventhub.payment.dto.PaymentOutcome;
import com.eventhub.payment.entity.PaymentMethod;
import com.eventhub.payment.entity.PaymentStatus;

/**
 * Simulador: devolve exatamente o resultado pedido, sem chamada externa.
 * Nenhuma regra de negocio depende dele alem da traducao outcome -> status.
 */
@Component
public class SimulatedPaymentGateway implements PaymentGateway {

    @Override
    public PaymentStatus charge(BigDecimal amount, PaymentMethod method, PaymentOutcome outcome) {
        return outcome == PaymentOutcome.APPROVED ? PaymentStatus.APPROVED : PaymentStatus.REFUSED;
    }
}
