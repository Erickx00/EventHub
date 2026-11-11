package com.eventhub.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.eventhub.order.entity.Order;
import com.eventhub.order.entity.OrderStatus;
import com.eventhub.order.repository.OrderRepository;
import com.eventhub.payment.dto.PaymentOutcome;
import com.eventhub.payment.dto.PaymentRequest;
import com.eventhub.payment.entity.Payment;
import com.eventhub.payment.entity.PaymentMethod;
import com.eventhub.payment.entity.PaymentStatus;
import com.eventhub.payment.gateway.SimulatedPaymentGateway;
import com.eventhub.payment.repository.PaymentRepository;
import com.eventhub.security.AuthPrincipal;
import com.eventhub.shared.exception.BusinessException;
import com.eventhub.shared.exception.ErrorCode;
import com.eventhub.ticket.dto.TicketIssuance;
import com.eventhub.ticket.service.TicketService;
import com.eventhub.user.entity.Role;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private OrderRepository orders;

    @Mock
    private PaymentRepository payments;

    @Mock
    private TicketService ticketService;

    private PaymentService paymentService;

    private final UUID userId = UUID.randomUUID();
    private final AuthPrincipal attendee = new AuthPrincipal(userId, "ana@example.com", Role.ATTENDEE);
    private final UUID orderId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(orders, payments, new SimulatedPaymentGateway(), ticketService);
    }

    private Order pendingOrder() {
        Order order = Order.create(userId, UUID.randomUUID(), Instant.now().plus(10, ChronoUnit.MINUTES));
        order.addItem(UUID.randomUUID(), 2, new BigDecimal("50.00"));
        return order;
    }

    private static void setExpiresAt(Order order, Instant expiresAt) {
        try {
            Field field = Order.class.getDeclaredField("expiresAt");
            field.setAccessible(true);
            field.set(order, expiresAt);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }

    @Test
    void approvedPaymentMarksOrderAsPaidAndIssuesTickets() {
        Order order = pendingOrder();
        when(orders.findByIdForUpdate(orderId)).thenReturn(Optional.of(order));

        var response = paymentService.pay(orderId,
                new PaymentRequest(PaymentMethod.PIX, PaymentOutcome.APPROVED), attendee);

        assertThat(response.status()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(response.orderStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(response.amount()).isEqualByComparingTo("100.00");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);

        ArgumentCaptor<TicketIssuance> issuance = ArgumentCaptor.forClass(TicketIssuance.class);
        verify(ticketService).issueFor(issuance.capture());
        assertThat(issuance.getValue().userId()).isEqualTo(userId);
        assertThat(issuance.getValue().items()).hasSize(1);
        assertThat(issuance.getValue().items().get(0).quantity()).isEqualTo(2);
    }

    @Test
    void refusedPaymentKeepsOrderPendingForAnotherAttempt() {
        Order order = pendingOrder();
        when(orders.findByIdForUpdate(orderId)).thenReturn(Optional.of(order));

        var response = paymentService.pay(orderId,
                new PaymentRequest(PaymentMethod.CREDIT_CARD, PaymentOutcome.REFUSED), attendee);

        assertThat(response.status()).isEqualTo(PaymentStatus.REFUSED);
        assertThat(response.paidAt()).isNull();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);

        ArgumentCaptor<Payment> saved = ArgumentCaptor.forClass(Payment.class);
        verify(payments).save(saved.capture());
        assertThat(saved.getValue().isApproved()).isFalse();
        verify(ticketService, never()).issueFor(any());
    }

    @Test
    void refusesPaymentAfterExpiry() {
        Order order = pendingOrder();
        setExpiresAt(order, Instant.now().minus(1, ChronoUnit.MINUTES));
        when(orders.findByIdForUpdate(orderId)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> paymentService.pay(orderId,
                new PaymentRequest(PaymentMethod.PIX, PaymentOutcome.APPROVED), attendee))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.ORDER_EXPIRED);

        // O status so muda no job: mutar aqui seria desfeito pelo rollback da excecao.
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        verify(payments, never()).save(any());
    }

    @Test
    void refusesSecondPaymentOnAlreadyPaidOrder() {
        Order order = pendingOrder();
        order.markPaid();
        when(orders.findByIdForUpdate(orderId)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> paymentService.pay(orderId,
                new PaymentRequest(PaymentMethod.PIX, PaymentOutcome.APPROVED), attendee))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.ORDER_NOT_PENDING);

        verify(payments, never()).save(any());
    }

    @Test
    void hidesOrderFromAnotherUser() {
        Order order = pendingOrder();
        when(orders.findByIdForUpdate(orderId)).thenReturn(Optional.of(order));
        AuthPrincipal other = new AuthPrincipal(UUID.randomUUID(), "outro@example.com", Role.ATTENDEE);

        assertThatThrownBy(() -> paymentService.pay(orderId,
                new PaymentRequest(PaymentMethod.PIX, PaymentOutcome.APPROVED), other))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.ORDER_NOT_FOUND);
    }

    @Test
    void readsOrderWithPessimisticLock() {
        Order order = pendingOrder();
        when(orders.findByIdForUpdate(orderId)).thenReturn(Optional.of(order));

        paymentService.pay(orderId, new PaymentRequest(PaymentMethod.PIX, PaymentOutcome.APPROVED), attendee);

        // Se voltar a usar findById simples, dois cliques simultaneos podem pagar duas vezes.
        verify(orders).findByIdForUpdate(orderId);
        verify(orders, never()).findById(any());
    }
}
