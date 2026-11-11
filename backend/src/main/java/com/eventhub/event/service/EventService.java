package com.eventhub.event.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eventhub.event.domain.EventCancelled;
import com.eventhub.event.dto.CreateEventRequest;
import com.eventhub.event.dto.EventResponse;
import com.eventhub.event.dto.UpdateEventRequest;
import com.eventhub.event.entity.Category;
import com.eventhub.event.entity.Event;
import com.eventhub.event.entity.EventStatus;
import com.eventhub.event.repository.EventRepository;
import com.eventhub.event.repository.EventSpecifications;
import com.eventhub.security.AuthPrincipal;
import com.eventhub.shared.exception.BusinessException;
import com.eventhub.shared.exception.ErrorCode;
import com.eventhub.shared.response.PageResponse;
import com.eventhub.user.entity.Role;

@Service
public class EventService {

    private final EventRepository events;
    private final ApplicationEventPublisher publisher;

    public EventService(EventRepository events, ApplicationEventPublisher publisher) {
        this.events = events;
        this.publisher = publisher;
    }

    @Transactional(readOnly = true)
    public PageResponse<EventResponse> search(Category category, String city, Instant from, Instant to, String q,
            Pageable pageable) {
        List<Specification<Event>> filters = new ArrayList<>();
        filters.add(EventSpecifications.published());

        if (category != null) {
            filters.add(EventSpecifications.withCategory(category));
        }
        if (hasText(city)) {
            filters.add(EventSpecifications.inCity(city));
        }
        if (from != null) {
            filters.add(EventSpecifications.startingFrom(from));
        }
        if (to != null) {
            filters.add(EventSpecifications.startingUntil(to));
        }
        if (hasText(q)) {
            filters.add(EventSpecifications.nameContains(q));
        }

        Page<Event> page = events.findAll(Specification.allOf(filters), pageable);
        return PageResponse.from(page, EventResponse::from);
    }

    @Transactional(readOnly = true)
    public PageResponse<EventResponse> findMine(AuthPrincipal principal, Pageable pageable) {
        Page<Event> page = events.findAll(EventSpecifications.ownedBy(principal.userId()), pageable);
        return PageResponse.from(page, EventResponse::from);
    }

    /**
     * Rascunho so e visivel para o dono e para ADMIN. Para os demais responde 404,
     * nao 403: nao confirma que o evento existe.
     */
    @Transactional(readOnly = true)
    public EventResponse getById(UUID id, AuthPrincipal viewer) {
        return EventResponse.from(requireVisible(id, viewer));
    }

    /**
     * Usado por modulos que precisam do evento (ingressos, por exemplo) sem
     * duplicar a regra de visibilidade.
     */
    @Transactional(readOnly = true)
    public Event requireVisible(UUID id, AuthPrincipal viewer) {
        Event event = requireEvent(id);
        if (!event.isPubliclyVisible() && !canManage(event, viewer)) {
            throw notFound();
        }
        return event;
    }

    /**
     * Exige um evento apto a receber dinheiro: publicado. Se o organizador
     * cancelou ou despublicou depois que alguem abriu o pedido, cobrar seria
     * vender ingresso para um evento que nao acontece.
     */
    @Transactional(readOnly = true)
    public Event requireOpenForSales(UUID id) {
        Event event = requireEvent(id);
        if (!event.isPubliclyVisible()) {
            throw BusinessException.conflict(ErrorCode.EVENT_NOT_PUBLISHED,
                    "Evento nao esta publicado e nao pode receber pedidos.");
        }
        return event;
    }

    /**
     * Carrega o evento e exige que o solicitante possa gerencia-lo. Lanca 404 se
     * nao existe e 403 se e de outro organizador.
     */
    @Transactional(readOnly = true)
    public Event requireManageable(UUID id, AuthPrincipal principal) {
        Event event = requireEvent(id);
        requireOwnership(event, principal);
        return event;
    }

    @Transactional
    public EventResponse create(CreateEventRequest request, AuthPrincipal principal) {
        requireCanOrganize(principal);

        if (request.startDate().isBefore(Instant.now())) {
            throw BusinessException.badRequest(ErrorCode.INVALID_PERIOD,
                    "A data de inicio deve ser futura.");
        }

        Event event = Event.draft(
                principal.userId(),
                request.name(),
                request.description(),
                request.category(),
                request.city(),
                request.venue(),
                request.startDate(),
                request.endDate());

        return EventResponse.from(events.save(event));
    }

    @Transactional
    public EventResponse update(UUID id, UpdateEventRequest request, AuthPrincipal principal) {
        Event event = requireEvent(id);
        requireOwnership(event, principal);

        event.updateDetails(request.name(), request.description(), request.category(), request.city(),
                request.venue(), request.startDate(), request.endDate());

        return EventResponse.from(event);
    }

    @Transactional
    public EventResponse publish(UUID id, AuthPrincipal principal) {
        Event event = requireEvent(id);
        requireOwnership(event, principal);
        event.publish();
        return EventResponse.from(event);
    }

    @Transactional
    public EventResponse unpublish(UUID id, AuthPrincipal principal) {
        Event event = requireEvent(id);
        requireOwnership(event, principal);
        event.unpublish();
        return EventResponse.from(event);
    }

    @Transactional
    public void cancel(UUID id, AuthPrincipal principal) {
        Event event = requireEvent(id);
        requireOwnership(event, principal);

        if (event.getStatus() == EventStatus.CANCELLED) {
            return;
        }

        event.cancel();

        // Publicado na mesma transacao: se o cancelamento da cascata falhar,
        // o evento nao fica cancelado com pedidos vivos.
        publisher.publishEvent(EventCancelled.now(event.getId()));
    }

    /**
     * Fecha eventos cujo fim ja passou. Chamado pelo job: sem isso um evento
     * antigo continuaria PUBLISHED e aparecendo na busca como se fosse futuro.
     *
     * @return quantidade de eventos finalizados nesta passagem
     */
    @Transactional
    public int finishOverdue() {
        Instant now = Instant.now();
        List<Event> ended = events.findAllByStatusAndEndDateBefore(EventStatus.PUBLISHED, now);

        for (Event event : ended) {
            event.finish(now);
        }

        return ended.size();
    }

    private Event requireEvent(UUID id) {
        return events.findById(id).orElseThrow(EventService::notFound);
    }

    private static BusinessException notFound() {
        return BusinessException.notFound(ErrorCode.EVENT_NOT_FOUND, "Evento nao encontrado.");
    }

    private static void requireCanOrganize(AuthPrincipal principal) {
        if (principal.role() != Role.ORGANIZER && principal.role() != Role.ADMIN) {
            throw BusinessException.forbidden(ErrorCode.FORBIDDEN,
                    "Apenas organizadores podem criar eventos.");
        }
    }

    private static void requireOwnership(Event event, AuthPrincipal principal) {
        if (!canManage(event, principal)) {
            throw BusinessException.forbidden(ErrorCode.FORBIDDEN,
                    "Voce nao pode alterar eventos de outro organizador.");
        }
    }

    private static boolean canManage(Event event, AuthPrincipal principal) {
        return principal != null
                && (event.isOwnedBy(principal.userId()) || principal.role() == Role.ADMIN);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
