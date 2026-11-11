package com.eventhub.checkin.entity;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Registro de entrada. Um ingresso tem no maximo um check-in (unique em
 * ticket_id).
 */
@Entity
@Table(name = "check_ins")
public class CheckIn {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "ticket_id", nullable = false, unique = true)
    private UUID ticketId;

    @Column(name = "validated_by", nullable = false)
    private UUID validatedBy;

    @CreationTimestamp
    @Column(name = "checked_at", nullable = false, updatable = false)
    private Instant checkedAt;

    protected CheckIn() {
        // exigido pelo JPA
    }

    private CheckIn(UUID ticketId, UUID validatedBy) {
        this.ticketId = ticketId;
        this.validatedBy = validatedBy;
    }

    public static CheckIn record(UUID ticketId, UUID validatedBy) {
        return new CheckIn(ticketId, validatedBy);
    }

    public UUID getId() {
        return id;
    }

    public UUID getTicketId() {
        return ticketId;
    }

    public UUID getValidatedBy() {
        return validatedBy;
    }

    public Instant getCheckedAt() {
        return checkedAt;
    }
}
