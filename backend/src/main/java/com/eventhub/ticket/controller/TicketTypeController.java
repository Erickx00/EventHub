package com.eventhub.ticket.controller;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.eventhub.security.AuthPrincipal;
import com.eventhub.ticket.dto.CreateTicketTypeRequest;
import com.eventhub.ticket.dto.TicketTypeResponse;
import com.eventhub.ticket.dto.UpdateTicketTypeRequest;
import com.eventhub.ticket.service.TicketTypeService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
public class TicketTypeController {

    private final TicketTypeService ticketTypeService;

    public TicketTypeController(TicketTypeService ticketTypeService) {
        this.ticketTypeService = ticketTypeService;
    }

    @GetMapping("/events/{eventId}/ticket-types")
    public List<TicketTypeResponse> listByEvent(@PathVariable UUID eventId,
            @AuthenticationPrincipal AuthPrincipal viewer) {
        return ticketTypeService.listByEvent(eventId, viewer);
    }

    @PostMapping("/events/{eventId}/ticket-types")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public ResponseEntity<TicketTypeResponse> create(@PathVariable UUID eventId,
            @Valid @RequestBody CreateTicketTypeRequest request,
            @AuthenticationPrincipal AuthPrincipal principal) {
        TicketTypeResponse created = ticketTypeService.create(eventId, request, principal);
        return ResponseEntity
                .created(URI.create("/api/ticket-types/" + created.id()))
                .body(created);
    }

    @PutMapping("/ticket-types/{id}")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public TicketTypeResponse update(@PathVariable UUID id,
            @Valid @RequestBody UpdateTicketTypeRequest request,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return ticketTypeService.update(id, request, principal);
    }

    @DeleteMapping("/ticket-types/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public void delete(@PathVariable UUID id, @AuthenticationPrincipal AuthPrincipal principal) {
        ticketTypeService.delete(id, principal);
    }
}
