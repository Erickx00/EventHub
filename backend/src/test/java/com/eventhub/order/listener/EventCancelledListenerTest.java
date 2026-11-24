package com.eventhub.order.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.eventhub.event.domain.EventCancelled;
import com.eventhub.order.entity.Order;
import com.eventhub.order.entity.OrderStatus;
import com.eventhub.order.repository.OrderRepository;
import com.eventhub.payment.service.PaymentService;
import com.eventhub.ticket.service.TicketService;
import com.eventhub.ticket.service.TicketTypeService;

@ExtendWith(MockitoExtension.class)
class EventCancelledListenerTest {

    @Mock
    private OrderRepository orders;

    @Mock
    private TicketTypeService ticketTypeService;

    @Mock
    private TicketService ticketService;

    @Mock
    private PaymentService paymentService;

    private EventCancelledListener listener;

    private final UUID eventId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final UUID ticketTypeId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        listener = new EventCancelledListener(orders, ticketTypeService, ticketService, paymentService);
    }

    private Order order(OrderStatus status) {
        Order order = Order.create(userId, eventId, Instant.now().plus(10, ChronoUnit.MINUTES));
        order.addItem(ticketTypeId, 2, new BigDecimal("50.00"));

        if (status == OrderStatus.PAID) {
            order.markPaid();
        }
        return order;
    }

    private void stubOrders(List<Order> pending, List<Order> paid) {
        when(orders.findAllByEventIdAndStatusForUpdate(eventId, OrderStatus.PENDING)).thenReturn(pending);
        when(orders.findAllByEventIdAndStatusForUpdate(eventId, OrderStatus.PAID)).thenReturn(paid);
    }

    @Test
    void cancelsPendingOrdersAndReturnsReservedStock() {
        Order pending = order(OrderStatus.PENDING);
        stubOrders(List.of(pending), List.of());

        listener.handle(EventCancelled.now(eventId));

        assertThat(pending.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(ticketTypeService).release(ticketTypeId, 2);
        verify(paymentService, never()).refund(any());
        verify(ticketService).cancelForEvent(eventId);
    }

    @Test
    void cancelsPaidOrdersAndRefundsThem() {
        Order paid = order(OrderStatus.PAID);
        stubOrders(List.of(), List.of(paid));

        listener.handle(EventCancelled.now(eventId));

        assertThat(paid.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(paymentService).refund(paid);
        // Pedido pago nao "devolve estoque": o lugar vendido nao volta a venda
        // para um evento cancelado.
        verify(ticketTypeService, never()).release(any(), any(Integer.class));
    }

    @Test
    void invalidatesEveryTicketEvenWhenThereAreNoOrders() {
        stubOrders(List.of(), List.of());

        listener.handle(EventCancelled.now(eventId));

        verify(ticketService).cancelForEvent(eventId);
    }
}
