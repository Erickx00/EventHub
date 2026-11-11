package com.eventhub.ticket.service;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eventhub.checkin.dto.CheckInResponse;
import com.eventhub.checkin.entity.CheckIn;
import com.eventhub.checkin.repository.CheckInRepository;
import com.eventhub.event.service.EventService;
import com.eventhub.security.AuthPrincipal;
import com.eventhub.shared.exception.BusinessException;
import com.eventhub.shared.exception.ErrorCode;
import com.eventhub.ticket.dto.TicketIssuance;
import com.eventhub.ticket.dto.TicketResponse;
import com.eventhub.ticket.entity.Ticket;
import com.eventhub.ticket.entity.TicketStatus;
import com.eventhub.ticket.repository.TicketRepository;

@Service
public class TicketService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final TicketRepository tickets;
    private final CheckInRepository checkIns;
    private final EventService events;
    private final QrCodeService qrCodes;

    public TicketService(TicketRepository tickets, CheckInRepository checkIns, EventService events,
            QrCodeService qrCodes) {
        this.tickets = tickets;
        this.checkIns = checkIns;
        this.events = events;
        this.qrCodes = qrCodes;
    }

    /**
     * Emite um ingresso por unidade comprada, cada um com token proprio. Chamado
     * quando o pagamento e aprovado.
     */
    @Transactional
    public void issueFor(TicketIssuance issuance) {
        List<Ticket> created = new ArrayList<>();

        for (TicketIssuance.Line line : issuance.items()) {
            for (int unit = 0; unit < line.quantity(); unit++) {
                created.add(Ticket.issue(
                        issuance.orderId(),
                        line.orderItemId(),
                        issuance.eventId(),
                        line.ticketTypeId(),
                        issuance.userId(),
                        newToken()));
            }
        }

        tickets.saveAll(created);
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> findMine(AuthPrincipal principal) {
        return tickets.findAllByUserIdOrderByIssuedAtDesc(principal.userId()).stream()
                .map(TicketResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public TicketResponse getById(UUID id, AuthPrincipal principal) {
        return TicketResponse.from(requireOwned(id, principal));
    }

    @Transactional(readOnly = true)
    public byte[] qrCodeOf(UUID id, AuthPrincipal principal) {
        return qrCodes.pngFor(requireOwned(id, principal).getQrToken());
    }

    /**
     * Check-in unico. O update condicional e a garantia: duas leituras
     * simultaneas do mesmo QR Code tentam marcar USED, mas so uma altera a linha.
     */
    @Transactional
    public CheckInResponse validate(String qrToken, AuthPrincipal principal) {
        Ticket ticket = tickets.findByQrToken(qrToken)
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.INVALID_QR_TOKEN, "Ingresso invalido."));

        // Somente quem gerencia o evento pode validar entrada nele.
        events.requireManageable(ticket.getEventId(), principal);

        int updated = tickets.markUsed(ticket.getId(), TicketStatus.USED, TicketStatus.VALID);
        if (updated == 0) {
            throw BusinessException.conflict(ErrorCode.TICKET_ALREADY_USED, "Ingresso ja utilizado.");
        }

        CheckIn checkIn = checkIns.save(CheckIn.record(ticket.getId(), principal.userId()));

        return new CheckInResponse(checkIn.getTicketId(), ticket.getEventId(), checkIn.getCheckedAt(),
                checkIn.getValidatedBy());
    }

    /**
     * Invalida todos os ingressos validos do evento. Chamado quando o evento e
     * cancelado: ingresso de evento cancelado nao pode passar na portaria.
     */
    @Transactional
    public int cancelForEvent(UUID eventId) {
        return tickets.cancelByEvent(eventId, TicketStatus.CANCELLED, TicketStatus.VALID);
    }

    private Ticket requireOwned(UUID id, AuthPrincipal principal) {
        Ticket ticket = tickets.findById(id)
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.TICKET_NOT_FOUND, "Ingresso nao encontrado."));

        if (!ticket.getUserId().equals(principal.userId())) {
            throw BusinessException.notFound(ErrorCode.TICKET_NOT_FOUND, "Ingresso nao encontrado.");
        }

        return ticket;
    }

    private static String newToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
