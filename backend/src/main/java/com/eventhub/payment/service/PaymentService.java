package com.eventhub.payment.service;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eventhub.order.entity.Order;
import com.eventhub.order.repository.OrderRepository;
import com.eventhub.payment.dto.PaymentRequest;
import com.eventhub.payment.dto.PaymentResponse;
import com.eventhub.payment.entity.Payment;
import com.eventhub.payment.entity.PaymentStatus;
import com.eventhub.payment.gateway.PaymentGateway;
import com.eventhub.payment.repository.PaymentRepository;
import com.eventhub.security.AuthPrincipal;
import com.eventhub.shared.exception.BusinessException;
import com.eventhub.shared.exception.ErrorCode;

@Service
public class PaymentService {

    private final OrderRepository orders;
    private final PaymentRepository payments;
    private final PaymentGateway paymentGateway;

    public PaymentService(OrderRepository orders, PaymentRepository payments, PaymentGateway paymentGateway) {
        this.orders = orders;
        this.payments = payments;
        this.paymentGateway = paymentGateway;
    }

    /**
     * O pedido e lido com lock pessimista: duas tentativas simultaneas de pagar o
     * mesmo pedido sao serializadas e a segunda encontra o status ja alterado.
     */
    @Transactional
    public PaymentResponse pay(UUID orderId, PaymentRequest request, AuthPrincipal principal) {
        Order order = orders.findByIdForUpdate(orderId)
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.ORDER_NOT_FOUND, "Pedido nao encontrado."));

        if (!order.getUserId().equals(principal.userId())) {
            throw BusinessException.notFound(ErrorCode.ORDER_NOT_FOUND, "Pedido nao encontrado.");
        }

        Instant now = Instant.now();

        if (order.isOverdueAt(now)) {
            // Nao alteramos o status aqui: mudar e lancar excecao desfaria a mudanca
            // no rollback. O job de expiracao faz a transicao e devolve o estoque.
            throw BusinessException.conflict(ErrorCode.ORDER_EXPIRED, "Pedido expirado.");
        }

        if (!order.isPending()) {
            throw BusinessException.conflict(ErrorCode.ORDER_NOT_PENDING,
                    "Pedido nao esta mais pendente (status atual: " + order.getStatus() + ").");
        }

        PaymentStatus status = paymentGateway.charge(order.getTotalAmount(), request.method(), request.outcome());

        Payment payment = status == PaymentStatus.APPROVED
                ? Payment.approved(orderId, order.getTotalAmount(), request.method(), now)
                : Payment.refused(orderId, order.getTotalAmount(), request.method());

        payments.save(payment);

        if (payment.isApproved()) {
            order.markPaid();
        }

        return PaymentResponse.from(payment, order.getStatus());
    }
}
