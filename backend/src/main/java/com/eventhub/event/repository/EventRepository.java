package com.eventhub.event.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.eventhub.event.entity.Event;
import com.eventhub.event.entity.EventStatus;

/**
 * JpaSpecificationExecutor permite compor filtros sem multiplicar metodos de
 * consulta (categoria + cidade + periodo + busca).
 */
public interface EventRepository extends JpaRepository<Event, UUID>, JpaSpecificationExecutor<Event> {

    long countByOrganizerId(UUID organizerId);

    long countByOrganizerIdAndStatus(UUID organizerId, EventStatus status);
}
