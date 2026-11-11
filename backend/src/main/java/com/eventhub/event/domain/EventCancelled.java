package com.eventhub.event.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Evento de dominio publicado quando um organizador cancela o evento.
 *
 * Existe para desacoplar: o modulo de eventos nao conhece pedidos nem ingressos,
 * mas quem depende dele reage. Sem isso, EventService -> OrderService ->
 * TicketService -> EventService fecharia um ciclo de beans.
 */
public record EventCancelled(UUID eventId, Instant occurredAt) {

    public static EventCancelled now(UUID eventId) {
        return new EventCancelled(eventId, Instant.now());
    }
}
