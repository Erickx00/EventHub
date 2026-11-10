package com.eventhub.order.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.eventhub.order.entity.Order;
import com.eventhub.order.entity.OrderItem;
import com.eventhub.order.entity.OrderStatus;

public record OrderResponse(
        UUID id,
        UUID eventId,
        OrderStatus status,
        BigDecimal totalAmount,
        Instant expiresAt,
        Instant createdAt,
        List<Item> items) {

    public record Item(UUID ticketTypeId, int quantity, BigDecimal unitPrice, BigDecimal subtotal) {

        static Item from(OrderItem item) {
            return new Item(item.getTicketTypeId(), item.getQuantity(), item.getUnitPrice(), item.getSubtotal());
        }
    }

    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getEventId(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getExpiresAt(),
                order.getCreatedAt(),
                order.getItems().stream().map(Item::from).toList());
    }
}
