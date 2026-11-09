package com.eventhub.event.controller;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;

import com.eventhub.event.dto.CreateEventRequest;
import com.eventhub.event.dto.EventResponse;
import com.eventhub.event.dto.UpdateEventRequest;
import com.eventhub.event.entity.Category;
import com.eventhub.event.service.EventService;
import com.eventhub.security.AuthPrincipal;
import com.eventhub.shared.response.PageResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    /**
     * Listagem publica: so eventos PUBLICADOS. Filtros sao opcionais e combinaveis.
     */
    @GetMapping
    public PageResponse<EventResponse> list(
            @RequestParam(required = false) Category category,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false, name = "q") String query,
            @PageableDefault(size = 20, sort = "startDate", direction = Sort.Direction.ASC) Pageable pageable) {
        return eventService.search(category, city, from, to, query, pageable);
    }

    @GetMapping("/mine")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public PageResponse<EventResponse> mine(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return eventService.findMine(principal, pageable);
    }

    @GetMapping("/{id}")
    public EventResponse get(@PathVariable UUID id, @AuthenticationPrincipal AuthPrincipal viewer) {
        return eventService.getById(id, viewer);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public ResponseEntity<EventResponse> create(@Valid @RequestBody CreateEventRequest request,
            @AuthenticationPrincipal AuthPrincipal principal) {
        EventResponse created = eventService.create(request, principal);
        return ResponseEntity
                .created(URI.create("/api/events/" + created.id()))
                .body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public EventResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateEventRequest request,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return eventService.update(id, request, principal);
    }

    @PatchMapping("/{id}/publish")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public EventResponse publish(@PathVariable UUID id, @AuthenticationPrincipal AuthPrincipal principal) {
        return eventService.publish(id, principal);
    }

    @PatchMapping("/{id}/unpublish")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public EventResponse unpublish(@PathVariable UUID id, @AuthenticationPrincipal AuthPrincipal principal) {
        return eventService.unpublish(id, principal);
    }

    /**
     * Cancelamento, nao exclusao fisica: o historico de pedidos precisa continuar
     * apontando para o evento.
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public void cancel(@PathVariable UUID id, @AuthenticationPrincipal AuthPrincipal principal) {
        eventService.cancel(id, principal);
    }
}
