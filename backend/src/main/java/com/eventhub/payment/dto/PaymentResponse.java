package com.eventhub.payment.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.eventhub.order.entity.OrderStatus;
import com.eventhub.payment.entity.Payment;
import com.eventhub.payment.entity.PaymentMethod;
import com.eventhub.payment.entity.PaymentStatus;

public record PaymentResponse(
        UUID id,
        UUID orderId,
        PaymentStatus status,
        BigDecimal amount,
        PaymentMethod method,
        Instant paidAt,
        OrderStatus orderStatus) {

    public static PaymentResponse from(Payment payment, OrderStatus orderStatus) {
        return new PaymentResponse(
                payment.getId(),
                payment.getOrderId(),
                payment.getStatus(),
                payment.getAmount(),
                payment.getMethod(),
                payment.getPaidAt(),
                orderStatus);
    }
}
