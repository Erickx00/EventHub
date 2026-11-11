package com.eventhub.ticket.dto;

import java.util.List;
import java.util.UUID;

/**
 * Dados necessarios para emitir ingressos, sem acoplar o modulo de ingressos a
 * entidade Order.
 */
public record TicketIssuance(UUID orderId, UUID userId, UUID eventId, List<Line> items) {

    public record Line(UUID orderItemId, UUID ticketTypeId, int quantity) {
    }
}
