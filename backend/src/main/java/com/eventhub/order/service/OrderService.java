package com.eventhub.order.service;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eventhub.event.entity.Event;
import com.eventhub.event.service.EventService;
import com.eventhub.order.dto.CreateOrderRequest;
import com.eventhub.order.dto.OrderResponse;
import com.eventhub.order.entity.Order;
import com.eventhub.order.entity.OrderItem;
import com.eventhub.order.entity.OrderStatus;
import com.eventhub.order.repository.OrderRepository;
import com.eventhub.security.AuthPrincipal;
import com.eventhub.shared.exception.BusinessException;
import com.eventhub.shared.exception.ErrorCode;
import com.eventhub.shared.response.PageResponse;
import com.eventhub.ticket.entity.TicketType;
import com.eventhub.ticket.repository.TicketTypeRepository;
import com.eventhub.ticket.service.TicketTypeService;

@Service
public class OrderService {

    private final OrderRepository orders;
    private final EventService events;
    private final TicketTypeRepository ticketTypes;
    private final TicketTypeService ticketTypeService;
    private final Duration paymentTtl;

    public OrderService(OrderRepository orders,
            EventService events,
            TicketTypeRepository ticketTypes,
            TicketTypeService ticketTypeService,
            @Value("${eventhub.orders.payment-ttl}") Duration paymentTtl) {
        this.orders = orders;
        this.events = events;
        this.ticketTypes = ticketTypes;
        this.ticketTypeService = ticketTypeService;
        this.paymentTtl = paymentTtl;
    }

    /**
     * Cria o pedido reservando estoque na mesma transacao: se qualquer item
     * falhar, o rollback desfaz as baixas ja feitas.
     */
    @Transactional
    public OrderResponse create(CreateOrderRequest request, AuthPrincipal principal) {
        Event event = events.requireVisible(request.eventId(), principal);
        if (!event.isPubliclyVisible()) {
            throw BusinessException.conflict(ErrorCode.EVENT_NOT_PUBLISHED,
                    "Somente eventos publicados podem receber pedidos.");
        }

        Instant now = Instant.now();
        Order order = Order.create(principal.userId(), event.getId(), now.plus(paymentTtl));

        for (Map.Entry<UUID, Integer> line : mergeDuplicates(request).entrySet()) {
            UUID ticketTypeId = line.getKey();
            int quantity = line.getValue();

            TicketType ticketType = ticketTypes.findById(ticketTypeId)
                    .orElseThrow(() -> BusinessException.notFound(ErrorCode.TICKET_TYPE_NOT_FOUND,
                            "Tipo de ingresso nao encontrado."));

            if (!ticketType.getEventId().equals(event.getId())) {
                throw BusinessException.badRequest(ErrorCode.INVALID_TICKET_TYPE,
                        "Ingresso nao pertence a este evento.");
            }

            // Preco lido do banco ANTES da baixa (o update limpa o contexto JPA).
            var unitPrice = ticketType.getPrice();

            ticketTypeService.reserve(ticketTypeId, quantity);
            order.addItem(ticketTypeId, quantity, unitPrice);
        }

        return OrderResponse.from(orders.save(order));
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> findMine(AuthPrincipal principal, Pageable pageable) {
        return PageResponse.from(
                orders.findAllByUserIdOrderByCreatedAtDesc(principal.userId(), pageable),
                OrderResponse::from);
    }

    @Transactional(readOnly = true)
    public OrderResponse getById(UUID id, AuthPrincipal principal) {
        return OrderResponse.from(requireOwnedOrder(id, principal));
    }

    /**
     * Cancelamento devolve o estoque reservado.
     */
    @Transactional
    public OrderResponse cancel(UUID id, AuthPrincipal principal) {
        Order order = requireOwnedOrder(id, principal);
        order.cancel();
        releaseStock(order);
        return OrderResponse.from(order);
    }

    /**
     * Chamado pelo job. Marca como EXPIRED e devolve o estoque de cada item.
     *
     * @return quantidade de pedidos expirados nesta passagem
     */
    @Transactional
    public int expireOverdue() {
        List<Order> overdue = orders.findAllByStatusAndExpiresAtBefore(OrderStatus.PENDING, Instant.now());

        for (Order order : overdue) {
            order.expire();
            releaseStock(order);
        }

        return overdue.size();
    }

    private void releaseStock(Order order) {
        for (OrderItem item : order.getItems()) {
            ticketTypeService.release(item.getTicketTypeId(), item.getQuantity());
        }
    }

    private Order requireOwnedOrder(UUID id, AuthPrincipal principal) {
        Order order = orders.findById(id)
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.ORDER_NOT_FOUND, "Pedido nao encontrado."));

        if (!order.getUserId().equals(principal.userId())) {
            // 404 em vez de 403: nao confirma que o pedido existe.
            throw BusinessException.notFound(ErrorCode.ORDER_NOT_FOUND, "Pedido nao encontrado.");
        }

        return order;
    }

    /**
     * O mesmo tipo pode vir em duas linhas; somar evita reservar duas vezes e
     * furar o limite por pedido.
     */
    private static Map<UUID, Integer> mergeDuplicates(CreateOrderRequest request) {
        Map<UUID, Integer> merged = new LinkedHashMap<>();
        for (CreateOrderRequest.Item item : request.items()) {
            merged.merge(item.ticketTypeId(), item.quantity(), Integer::sum);
        }
        return merged;
    }
}
