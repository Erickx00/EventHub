package com.eventhub.event.repository;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;

import com.eventhub.event.entity.Category;
import com.eventhub.event.entity.Event;
import com.eventhub.event.entity.EventStatus;

/**
 * Filtros combinaveis da listagem de eventos. Os nomes dos campos sao strings
 * porque o projeto nao gera metamodelo JPA; um rename exige atualizar aqui.
 */
public final class EventSpecifications {

    private EventSpecifications() {
    }

    public static Specification<Event> published() {
        return (root, query, cb) -> cb.equal(root.get("status"), EventStatus.PUBLISHED);
    }

    public static Specification<Event> withStatus(EventStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Event> withCategory(Category category) {
        return (root, query, cb) -> cb.equal(root.get("category"), category);
    }

    public static Specification<Event> inCity(String city) {
        String normalized = city.trim().toLowerCase(Locale.ROOT);
        return (root, query, cb) -> cb.equal(cb.lower(root.get("city")), normalized);
    }

    public static Specification<Event> startingFrom(Instant from) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("startDate"), from);
    }

    public static Specification<Event> startingUntil(Instant to) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("startDate"), to);
    }

    public static Specification<Event> nameContains(String term) {
        String pattern = "%" + term.trim().toLowerCase(Locale.ROOT) + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.get("name")), pattern);
    }

    public static Specification<Event> ownedBy(UUID organizerId) {
        return (root, query, cb) -> cb.equal(root.get("organizerId"), organizerId);
    }
}
