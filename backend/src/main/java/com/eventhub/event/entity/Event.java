package com.eventhub.event.entity;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.eventhub.shared.exception.BusinessException;
import com.eventhub.shared.exception.ErrorCode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Evento. As transicoes de estado vivem aqui (e nao no service) porque valem para
 * qualquer caso de uso: publicar, cancelar, editar. O service cuida de
 * autorizacao e de coordenar repositorios.
 */
@Entity
@Table(name = "events")
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "organizer_id", nullable = false)
    private UUID organizerId;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private Category category;

    @Column(nullable = false, length = 120)
    private String city;

    @Column(length = 160)
    private String venue;

    @Column(name = "start_date", nullable = false)
    private Instant startDate;

    @Column(name = "end_date", nullable = false)
    private Instant endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EventStatus status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Event() {
        // exigido pelo JPA
    }

    private Event(UUID organizerId, String name, String description, Category category, String city, String venue,
            Instant startDate, Instant endDate) {
        this.organizerId = organizerId;
        this.name = name;
        this.description = description;
        this.category = category;
        this.city = city;
        this.venue = venue;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = EventStatus.DRAFT;
    }

    public static Event draft(UUID organizerId, String name, String description, Category category, String city,
            String venue, Instant startDate, Instant endDate) {
        requireValidPeriod(startDate, endDate);
        return new Event(organizerId, name.trim(), description, category, city.trim(),
                venue == null ? null : venue.trim(), startDate, endDate);
    }

    public void updateDetails(String name, String description, Category category, String city, String venue,
            Instant startDate, Instant endDate) {
        if (!isEditable()) {
            throw BusinessException.conflict(ErrorCode.EVENT_NOT_EDITABLE,
                    "Apenas eventos em rascunho ou publicados podem ser editados.");
        }
        requireValidPeriod(startDate, endDate);

        this.name = name.trim();
        this.description = description;
        this.category = category;
        this.city = city.trim();
        this.venue = venue == null ? null : venue.trim();
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public void publish() {
        if (status != EventStatus.DRAFT) {
            throw BusinessException.conflict(ErrorCode.EVENT_NOT_PUBLISHABLE,
                    "Somente eventos em rascunho podem ser publicados.");
        }
        this.status = EventStatus.PUBLISHED;
    }

    public void unpublish() {
        if (status != EventStatus.PUBLISHED) {
            throw BusinessException.conflict(ErrorCode.EVENT_NOT_EDITABLE,
                    "Somente eventos publicados podem voltar para rascunho.");
        }
        this.status = EventStatus.DRAFT;
    }

    /**
     * Cancelar e idempotente: chamar duas vezes nao e erro, o evento ja esta
     * invisivel para o publico.
     */
    public void cancel() {
        if (status == EventStatus.CANCELLED) {
            return;
        }
        if (status == EventStatus.FINISHED) {
            throw BusinessException.conflict(ErrorCode.EVENT_NOT_EDITABLE,
                    "Evento finalizado nao pode ser cancelado.");
        }
        this.status = EventStatus.CANCELLED;
    }

    /**
     * Encerra um evento publicado cujo fim ja passou. Chamado pelo job; a
     * checagem de data existe para o metodo nao virar um "force finish".
     */
    public void finish(Instant now) {
        if (status != EventStatus.PUBLISHED) {
            throw BusinessException.conflict(ErrorCode.EVENT_NOT_EDITABLE,
                    "Somente eventos publicados podem ser finalizados.");
        }
        if (endDate.isAfter(now)) {
            throw BusinessException.conflict(ErrorCode.INVALID_PERIOD,
                    "Evento ainda nao terminou.");
        }
        this.status = EventStatus.FINISHED;
    }

    private static void requireValidPeriod(Instant startDate, Instant endDate) {
        if (startDate == null || endDate == null || !endDate.isAfter(startDate)) {
            throw BusinessException.badRequest(ErrorCode.INVALID_PERIOD,
                    "A data de termino deve ser posterior a data de inicio.");
        }
    }

    public boolean isOwnedBy(UUID userId) {
        return organizerId.equals(userId);
    }

    public boolean isEditable() {
        return status == EventStatus.DRAFT || status == EventStatus.PUBLISHED;
    }

    public boolean isPubliclyVisible() {
        return status == EventStatus.PUBLISHED;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrganizerId() {
        return organizerId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Category getCategory() {
        return category;
    }

    public String getCity() {
        return city;
    }

    public String getVenue() {
        return venue;
    }

    public Instant getStartDate() {
        return startDate;
    }

    public Instant getEndDate() {
        return endDate;
    }

    public EventStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
