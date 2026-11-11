package com.eventhub.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.eventhub.event.entity.Category;
import com.eventhub.event.entity.Event;
import com.eventhub.event.service.EventService;
import com.eventhub.order.dto.CreateOrderRequest;
import com.eventhub.order.entity.Order;
import com.eventhub.order.entity.OrderStatus;
import com.eventhub.order.repository.OrderRepository;
import com.eventhub.security.AuthPrincipal;
import com.eventhub.shared.exception.BusinessException;
import com.eventhub.shared.exception.ErrorCode;
import com.eventhub.ticket.entity.TicketType;
import com.eventhub.ticket.repository.TicketTypeRepository;
import com.eventhub.ticket.service.TicketTypeService;
import com.eventhub.user.entity.Role;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    private static final Instant START = Instant.now().plus(30, ChronoUnit.DAYS);
    private static final Instant SALES_START = Instant.now().minus(1, ChronoUnit.DAYS);
    private static final Instant SALES_END = START;

    @Mock
    private OrderRepository orders;

    @Mock
    private EventService events;

    @Mock
    private TicketTypeRepository ticketTypes;

    @Mock
    private TicketTypeService ticketTypeService;

    private OrderService orderService;

    private final UUID userId = UUID.randomUUID();
    private final AuthPrincipal attendee = new AuthPrincipal(userId, "ana@example.com", Role.ATTENDEE);
    private final UUID eventId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        orderService = new OrderService(orders, events, ticketTypes, ticketTypeService, Duration.ofMinutes(15));
    }

    private Event publishedEvent() {
        Event event = Event.draft(UUID.randomUUID(), "Java Conference", null, Category.TECHNOLOGY,
                "Campina Grande", null, START, START.plus(8, ChronoUnit.HOURS));
        event.publish();
        // O evento so recebe id ao ser persistido; o fixture precisa dele para a
        // checagem "ingresso pertence a este evento".
        setField(event, "id", eventId);
        return event;
    }

    private static void setField(Object target, String name, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(name);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private TicketType ticketType(BigDecimal price) {
        return TicketType.create(eventId, "Pista", null, price, 100, 5, SALES_START, SALES_END);
    }

    private CreateOrderRequest orderWith(UUID ticketTypeId, int quantity) {
        return new CreateOrderRequest(eventId, List.of(new CreateOrderRequest.Item(ticketTypeId, quantity)));
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

    private Order pendingOrderWithItem(UUID ticketTypeId, int quantity) {
        Order order = Order.create(userId, eventId, Instant.now().plus(10, ChronoUnit.MINUTES));
        order.addItem(ticketTypeId, quantity, new BigDecimal("50.00"));
        return order;
    }

    @Test
    void createsOrderWithTotalFromDatabaseAndReservesStock() {
        UUID ticketTypeId = UUID.randomUUID();
        when(events.requireOpenForSales(eventId)).thenReturn(publishedEvent());
        when(ticketTypes.findById(ticketTypeId)).thenReturn(Optional.of(ticketType(new BigDecimal("50.00"))));
        when(orders.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = orderService.create(orderWith(ticketTypeId, 2), attendee);

        ArgumentCaptor<Order> saved = ArgumentCaptor.forClass(Order.class);
        verify(orders).save(saved.capture());
        assertThat(saved.getValue().getTotalAmount()).isEqualByComparingTo("100.00");
        assertThat(saved.getValue().getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(response.items()).hasSize(1);
        verify(ticketTypeService).reserve(ticketTypeId, 2);
    }

    @Test
    void refusesOrderForUnpublishedEvent() {
        when(events.requireOpenForSales(eventId))
                .thenThrow(BusinessException.conflict(ErrorCode.EVENT_NOT_PUBLISHED, "nao publicado"));

        assertThatThrownBy(() -> orderService.create(orderWith(UUID.randomUUID(), 1), attendee))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.EVENT_NOT_PUBLISHED);

        verify(orders, never()).save(any());
    }

    @Test
    void refusesTicketTypeBelongingToAnotherEvent() {
        UUID ticketTypeId = UUID.randomUUID();
        TicketType foreign = TicketType.create(UUID.randomUUID(), "Pista", null, BigDecimal.TEN, 10, 2,
                SALES_START, SALES_END);

        when(events.requireOpenForSales(eventId)).thenReturn(publishedEvent());
        when(ticketTypes.findById(ticketTypeId)).thenReturn(Optional.of(foreign));

        assertThatThrownBy(() -> orderService.create(orderWith(ticketTypeId, 1), attendee))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.INVALID_TICKET_TYPE);

        verify(ticketTypeService, never()).reserve(any(), anyInt());
    }

    @Test
    void mergesDuplicateLinesBeforeReserving() {
        UUID ticketTypeId = UUID.randomUUID();
        when(events.requireOpenForSales(eventId)).thenReturn(publishedEvent());
        when(ticketTypes.findById(ticketTypeId)).thenReturn(Optional.of(ticketType(new BigDecimal("50.00"))));
        when(orders.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var request = new CreateOrderRequest(eventId, List.of(
                new CreateOrderRequest.Item(ticketTypeId, 1),
                new CreateOrderRequest.Item(ticketTypeId, 2)));

        var response = orderService.create(request, attendee);

        verify(ticketTypeService).reserve(ticketTypeId, 3);
        assertThat(response.items()).hasSize(1);
        assertThat(response.totalAmount()).isEqualByComparingTo("150.00");
    }

    @Test
    void doesNotPersistOrderWhenStockIsGone() {
        UUID ticketTypeId = UUID.randomUUID();
        when(events.requireOpenForSales(eventId)).thenReturn(publishedEvent());
        when(ticketTypes.findById(ticketTypeId)).thenReturn(Optional.of(ticketType(new BigDecimal("50.00"))));
        org.mockito.Mockito.doThrow(BusinessException.conflict(ErrorCode.TICKET_SOLD_OUT, "sem estoque"))
                .when(ticketTypeService).reserve(ticketTypeId, 2);

        assertThatThrownBy(() -> orderService.create(orderWith(ticketTypeId, 2), attendee))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.TICKET_SOLD_OUT);

        verify(orders, never()).save(any());
    }

    @Test
    void cancellingReleasesReservedStock() {
        UUID ticketTypeId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        when(orders.findById(orderId)).thenReturn(Optional.of(pendingOrderWithItem(ticketTypeId, 2)));

        var response = orderService.cancel(orderId, attendee);

        assertThat(response.status()).isEqualTo(OrderStatus.CANCELLED);
        verify(ticketTypeService).release(ticketTypeId, 2);
    }

    @Test
    void hidesOrderFromAnotherUser() {
        UUID orderId = UUID.randomUUID();
        when(orders.findById(orderId)).thenReturn(Optional.of(pendingOrderWithItem(UUID.randomUUID(), 1)));
        AuthPrincipal other = new AuthPrincipal(UUID.randomUUID(), "outro@example.com", Role.ATTENDEE);

        assertThatThrownBy(() -> orderService.getById(orderId, other))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.ORDER_NOT_FOUND);
    }

    @Test
    void expiresOverdueOrdersAndReturnsStock() {
        UUID ticketTypeId = UUID.randomUUID();
        Order overdue = pendingOrderWithItem(ticketTypeId, 2);
        setExpiresAt(overdue, Instant.now().minus(1, ChronoUnit.MINUTES));

        when(orders.findAllByStatusAndExpiresAtBefore(any(), any())).thenReturn(List.of(overdue));

        int expired = orderService.expireOverdue();

        assertThat(expired).isEqualTo(1);
        assertThat(overdue.getStatus()).isEqualTo(OrderStatus.EXPIRED);
        verify(ticketTypeService).release(ticketTypeId, 2);
    }

    @Test
    void expirationIsIdempotentWhenNoOrderIsOverdue() {
        when(orders.findAllByStatusAndExpiresAtBefore(any(), any())).thenReturn(List.of());

        assertThat(orderService.expireOverdue()).isZero();
        verify(ticketTypeService, never()).release(any(), anyInt());
    }
}
