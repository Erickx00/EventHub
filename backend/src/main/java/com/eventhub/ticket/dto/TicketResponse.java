package com.eventhub.ticket.dto;

import java.time.Instant;
import java.util.UUID;

import com.eventhub.ticket.entity.Ticket;
import com.eventhub.ticket.entity.TicketStatus;

public record TicketResponse(
        UUID id,
        UUID eventId,
        UUID ticketTypeId,
        TicketStatus status,
        String qrToken,
        Instant issuedAt) {

    public static TicketResponse from(Ticket ticket) {
        return new TicketResponse(
                ticket.getId(),
                ticket.getEventId(),
                ticket.getTicketTypeId(),
                ticket.getStatus(),
                ticket.getQrToken(),
                ticket.getIssuedAt());
    }
}
