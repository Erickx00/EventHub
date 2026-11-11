package com.eventhub.checkin.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.eventhub.checkin.dto.CheckInResponse;
import com.eventhub.checkin.dto.ValidateCheckInRequest;
import com.eventhub.security.AuthPrincipal;
import com.eventhub.ticket.service.TicketService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/checkins")
public class CheckInController {

    private final TicketService ticketService;

    public CheckInController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @PostMapping("/validate")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public CheckInResponse validate(@Valid @RequestBody ValidateCheckInRequest request,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return ticketService.validate(request.qrToken(), principal);
    }
}
