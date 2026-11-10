package com.eventhub.ticket.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.eventhub.ticket.entity.TicketType;

public record TicketTypeResponse(
        UUID id,
        UUID eventId,
        String name,
        String description,
        BigDecimal price,
        int totalQuantity,
        int availableQuantity,
        int maxPerOrder,
        Instant salesStart,
        Instant salesEnd,
        boolean soldOut) {

    public static TicketTypeResponse from(TicketType ticketType) {
        return new TicketTypeResponse(
                ticketType.getId(),
                ticketType.getEventId(),
                ticketType.getName(),
                ticketType.getDescription(),
                ticketType.getPrice(),
                ticketType.getTotalQuantity(),
                ticketType.getAvailableQuantity(),
                ticketType.getMaxPerOrder(),
                ticketType.getSalesStart(),
                ticketType.getSalesEnd(),
                ticketType.getAvailableQuantity() == 0);
    }
}
