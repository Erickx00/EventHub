package com.eventhub.dashboard.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eventhub.checkin.repository.CheckInRepository;
import com.eventhub.dashboard.dto.DashboardResponse;
import com.eventhub.dashboard.dto.EventDashboardResponse;
import com.eventhub.event.entity.EventStatus;
import com.eventhub.event.repository.EventRepository;
import com.eventhub.event.service.EventService;
import com.eventhub.order.entity.OrderStatus;
import com.eventhub.order.repository.OrderRepository;
import com.eventhub.security.AuthPrincipal;
import com.eventhub.shared.exception.BusinessException;
import com.eventhub.shared.exception.ErrorCode;
import com.eventhub.ticket.repository.TicketRepository;
import com.eventhub.ticket.repository.TicketTypeRepository;
import com.eventhub.user.entity.Role;

@Service
public class DashboardService {

    private final EventRepository events;
    private final TicketRepository tickets;
    private final TicketTypeRepository ticketTypes;
    private final OrderRepository orders;
    private final CheckInRepository checkIns;
    private final EventService eventService;

    public DashboardService(EventRepository events,
            TicketRepository tickets,
            TicketTypeRepository ticketTypes,
            OrderRepository orders,
            CheckInRepository checkIns,
            EventService eventService) {
        this.events = events;
        this.tickets = tickets;
        this.ticketTypes = ticketTypes;
        this.orders = orders;
        this.checkIns = checkIns;
        this.eventService = eventService;
    }

    @Transactional(readOnly = true)
    public DashboardResponse organizer(AuthPrincipal principal) {
        requireOrganizer(principal);
        UUID organizerId = principal.userId();

        return new DashboardResponse(
                events.countByOrganizerId(organizerId),
                events.countByOrganizerIdAndStatus(organizerId, EventStatus.PUBLISHED),
                tickets.countByOrganizer(organizerId),
                ticketTypes.sumAvailableByOrganizer(organizerId),
                orders.sumRevenueByOrganizer(organizerId, OrderStatus.PAID),
                checkIns.countByOrganizer(organizerId),
                tickets.countDistinctAttendeesByOrganizer(organizerId));
    }

    @Transactional(readOnly = true)
    public EventDashboardResponse event(UUID eventId, AuthPrincipal principal) {
        // 404 se nao existe, 403 se e de outro organizador.
        eventService.requireManageable(eventId, principal);

        return new EventDashboardResponse(
                eventId,
                tickets.countByEventId(eventId),
                ticketTypes.sumAvailableByEvent(eventId),
                orders.sumRevenueByEvent(eventId, OrderStatus.PAID),
                checkIns.countByEvent(eventId),
                tickets.countDistinctAttendeesByEvent(eventId));
    }

    private static void requireOrganizer(AuthPrincipal principal) {
        if (principal.role() != Role.ORGANIZER && principal.role() != Role.ADMIN) {
            throw BusinessException.forbidden(ErrorCode.FORBIDDEN,
                    "Apenas organizadores tem dashboard.");
        }
    }
}
