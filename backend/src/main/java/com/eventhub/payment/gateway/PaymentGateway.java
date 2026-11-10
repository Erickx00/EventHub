package com.eventhub.payment.gateway;

import java.math.BigDecimal;

import com.eventhub.payment.dto.PaymentOutcome;
import com.eventhub.payment.entity.PaymentMethod;
import com.eventhub.payment.entity.PaymentStatus;

/**
 * Fronteira com o mundo externo. Quando um provedor real entrar, ele implementa
 * esta interface e o restante do codigo nao muda.
 */
public interface PaymentGateway {

    PaymentStatus charge(BigDecimal amount, PaymentMethod method, PaymentOutcome outcome);
}
