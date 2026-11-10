package com.eventhub.ticket.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eventhub.event.service.EventService;
import com.eventhub.security.AuthPrincipal;
import com.eventhub.shared.exception.BusinessException;
import com.eventhub.shared.exception.ErrorCode;
import com.eventhub.ticket.dto.CreateTicketTypeRequest;
import com.eventhub.ticket.dto.TicketTypeResponse;
import com.eventhub.ticket.dto.UpdateTicketTypeRequest;
import com.eventhub.ticket.entity.TicketType;
import com.eventhub.ticket.repository.TicketTypeRepository;

@Service
public class TicketTypeService {

    private final TicketTypeRepository ticketTypes;
    private final EventService events;

    public TicketTypeService(TicketTypeRepository ticketTypes, EventService events) {
        this.ticketTypes = ticketTypes;
        this.events = events;
    }

    @Transactional
    public TicketTypeResponse create(UUID eventId, CreateTicketTypeRequest request, AuthPrincipal principal) {
        events.requireManageable(eventId, principal);

        TicketType ticketType = TicketType.create(
                eventId,
                request.name(),
                request.description(),
                request.price(),
                request.totalQuantity(),
                request.maxPerOrder(),
                request.salesStart(),
                request.salesEnd());

        return TicketTypeResponse.from(ticketTypes.save(ticketType));
    }

    /**
     * Publico para evento publicado; rascunho so para o dono/ADMIN.
     */
    @Transactional(readOnly = true)
    public List<TicketTypeResponse> listByEvent(UUID eventId, AuthPrincipal viewer) {
        events.requireVisible(eventId, viewer);

        return ticketTypes.findAllByEventIdOrderByPriceAsc(eventId).stream()
                .map(TicketTypeResponse::from)
                .toList();
    }

    @Transactional
    public TicketTypeResponse update(UUID id, UpdateTicketTypeRequest request, AuthPrincipal principal) {
        TicketType ticketType = requireTicketType(id);
        events.requireManageable(ticketType.getEventId(), principal);

        ticketType.updateDetails(
                request.name(),
                request.description(),
                request.price(),
                request.totalQuantity(),
                request.maxPerOrder(),
                request.salesStart(),
                request.salesEnd());

        return TicketTypeResponse.from(ticketType);
    }

    /**
     * Remover um tipo com vendas apagaria o historico de quem ja comprou.
     */
    @Transactional
    public void delete(UUID id, AuthPrincipal principal) {
        TicketType ticketType = requireTicketType(id);
        events.requireManageable(ticketType.getEventId(), principal);

        if (ticketType.hasSales()) {
            throw BusinessException.conflict(ErrorCode.TICKET_TYPE_HAS_SALES,
                    "Este ingresso ja tem vendas e nao pode ser removido.");
        }

        ticketTypes.delete(ticketType);
    }

    /**
     * Reserva estoque para um pedido. A garantia contra oversell e o UPDATE
     * condicional: se nenhuma linha mudar, outra transacao levou o estoque.
     */
    @Transactional
    public void reserve(UUID id, int quantity) {
        TicketType ticketType = requireTicketType(id);
        ticketType.assertPurchasable(quantity, Instant.now());

        if (ticketTypes.reserveStock(id, quantity) == 0) {
            throw BusinessException.conflict(ErrorCode.TICKET_SOLD_OUT, "Estoque insuficiente.");
        }
    }

    /**
     * Devolve estoque (pedido expirado ou cancelado). Idempotente na pratica: se
     * nada casar, nao ha erro.
     */
    @Transactional
    public void release(UUID id, int quantity) {
        ticketTypes.releaseStock(id, quantity);
    }

    private TicketType requireTicketType(UUID id) {
        return ticketTypes.findById(id)
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.TICKET_TYPE_NOT_FOUND,
                        "Tipo de ingresso nao encontrado."));
    }
}
