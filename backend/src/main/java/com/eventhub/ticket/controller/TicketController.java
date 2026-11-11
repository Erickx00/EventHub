package com.eventhub.ticket.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.eventhub.security.AuthPrincipal;
import com.eventhub.ticket.dto.TicketResponse;
import com.eventhub.ticket.service.TicketService;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @GetMapping("/me")
    public List<TicketResponse> mine(@AuthenticationPrincipal AuthPrincipal principal) {
        return ticketService.findMine(principal);
    }

    @GetMapping("/{id}")
    public TicketResponse get(@PathVariable UUID id, @AuthenticationPrincipal AuthPrincipal principal) {
        return ticketService.getById(id, principal);
    }

    /**
     * PNG do QR Code para exibir na tela ou salvar no celular. O conteudo e
     * apenas o token opaco do ingresso.
     */
    @GetMapping(value = "/{id}/qr", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> qrCode(@PathVariable UUID id,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(ticketService.qrCodeOf(id, principal));
    }
}
