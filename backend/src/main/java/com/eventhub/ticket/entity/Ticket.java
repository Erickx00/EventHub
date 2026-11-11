package com.eventhub.ticket.entity;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Ingresso individual. O QR Code codifica apenas o qr_token: nenhum dado
 * pessoal trafega nele, e o token so faz sentido contra o backend.
 */
@Entity
@Table(name = "tickets")
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "order_item_id", nullable = false)
    private UUID orderItemId;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "ticket_type_id", nullable = false)
    private UUID ticketTypeId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "qr_token", nullable = false, length = 64, unique = true)
    private String qrToken;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TicketStatus status;

    @CreationTimestamp
    @Column(name = "issued_at", nullable = false, updatable = false)
    private Instant issuedAt;

    protected Ticket() {
        // exigido pelo JPA
    }

    private Ticket(UUID orderId, UUID orderItemId, UUID eventId, UUID ticketTypeId, UUID userId, String qrToken) {
        this.orderId = orderId;
        this.orderItemId = orderItemId;
        this.eventId = eventId;
        this.ticketTypeId = ticketTypeId;
        this.userId = userId;
        this.qrToken = qrToken;
        this.status = TicketStatus.VALID;
    }

    public static Ticket issue(UUID orderId, UUID orderItemId, UUID eventId, UUID ticketTypeId, UUID userId,
            String qrToken) {
        return new Ticket(orderId, orderItemId, eventId, ticketTypeId, userId, qrToken);
    }

    public boolean isValid() {
        return status == TicketStatus.VALID;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public UUID getEventId() {
        return eventId;
    }

    public UUID getTicketTypeId() {
        return ticketTypeId;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getQrToken() {
        return qrToken;
    }

    public TicketStatus getStatus() {
        return status;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }
}
