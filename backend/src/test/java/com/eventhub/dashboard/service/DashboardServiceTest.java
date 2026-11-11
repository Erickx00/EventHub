package com.eventhub.dashboard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.eventhub.checkin.repository.CheckInRepository;
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

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private EventRepository events;

    @Mock
    private TicketRepository tickets;

    @Mock
    private TicketTypeRepository ticketTypes;

    @Mock
    private OrderRepository orders;

    @Mock
    private CheckInRepository checkIns;

    @Mock
    private EventService eventService;

    private DashboardService dashboardService;

    private final UUID organizerId = UUID.randomUUID();
    private final AuthPrincipal organizer = new AuthPrincipal(organizerId, "org@example.com", Role.ORGANIZER);

    @BeforeEach
    void setUp() {
        dashboardService = new DashboardService(events, tickets, ticketTypes, orders, checkIns, eventService);
    }

    @Test
    void aggregatesOrganizerMetrics() {
        when(events.countByOrganizerId(organizerId)).thenReturn(4L);
        when(events.countByOrganizerIdAndStatus(organizerId, EventStatus.PUBLISHED)).thenReturn(3L);
        when(tickets.countByOrganizer(organizerId)).thenReturn(120L);
        when(ticketTypes.sumAvailableByOrganizer(organizerId)).thenReturn(380L);
        when(orders.sumRevenueByOrganizer(organizerId, OrderStatus.PAID)).thenReturn(new BigDecimal("14500.00"));
        when(checkIns.countByOrganizer(organizerId)).thenReturn(87L);
        when(tickets.countDistinctAttendeesByOrganizer(organizerId)).thenReturn(110L);

        var dashboard = dashboardService.organizer(organizer);

        assertThat(dashboard.totalEvents()).isEqualTo(4);
        assertThat(dashboard.publishedEvents()).isEqualTo(3);
        assertThat(dashboard.ticketsSold()).isEqualTo(120);
        assertThat(dashboard.ticketsAvailable()).isEqualTo(380);
        assertThat(dashboard.revenue()).isEqualByComparingTo("14500.00");
        assertThat(dashboard.checkIns()).isEqualTo(87);
        assertThat(dashboard.uniqueAttendees()).isEqualTo(110);
    }

    @Test
    void refusesDashboardForAttendee() {
        AuthPrincipal attendee = new AuthPrincipal(UUID.randomUUID(), "ana@example.com", Role.ATTENDEE);

        assertThatThrownBy(() -> dashboardService.organizer(attendee))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.FORBIDDEN);
    }

    @Test
    void eventDashboardRequiresOwnership() {
        UUID eventId = UUID.randomUUID();
        when(eventService.requireManageable(eventId, organizer))
                .thenThrow(BusinessException.forbidden(ErrorCode.FORBIDDEN, "nao e dono"));

        assertThatThrownBy(() -> dashboardService.event(eventId, organizer))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.FORBIDDEN);
    }

    @Test
    void mapsEventMetrics() {
        UUID eventId = UUID.randomUUID();
        when(tickets.countByEventId(eventId)).thenReturn(42L);
        when(ticketTypes.sumAvailableByEvent(eventId)).thenReturn(158L);
        when(orders.sumRevenueByEvent(eventId, OrderStatus.PAID)).thenReturn(new BigDecimal("5040.00"));
        when(checkIns.countByEvent(eventId)).thenReturn(30L);
        when(tickets.countDistinctAttendeesByEvent(eventId)).thenReturn(40L);

        var dashboard = dashboardService.event(eventId, organizer);

        assertThat(dashboard.eventId()).isEqualTo(eventId);
        assertThat(dashboard.ticketsSold()).isEqualTo(42);
        assertThat(dashboard.ticketsAvailable()).isEqualTo(158);
        assertThat(dashboard.revenue()).isEqualByComparingTo("5040.00");
        assertThat(dashboard.checkIns()).isEqualTo(30);
        assertThat(dashboard.uniqueAttendees()).isEqualTo(40);
    }

    @Test
    void organizerDashboardOnlyReadsTheAuthenticatedOrganizerData() {
        when(events.countByOrganizerId(organizerId)).thenReturn(0L);
        when(events.countByOrganizerIdAndStatus(organizerId, EventStatus.PUBLISHED)).thenReturn(0L);
        when(tickets.countByOrganizer(organizerId)).thenReturn(0L);
        when(ticketTypes.sumAvailableByOrganizer(organizerId)).thenReturn(0L);
        when(orders.sumRevenueByOrganizer(organizerId, OrderStatus.PAID)).thenReturn(BigDecimal.ZERO);
        when(checkIns.countByOrganizer(organizerId)).thenReturn(0L);
        when(tickets.countDistinctAttendeesByOrganizer(organizerId)).thenReturn(0L);

        var dashboard = dashboardService.organizer(organizer);

        assertThat(dashboard.totalEvents()).isZero();
        assertThat(dashboard.revenue()).isEqualByComparingTo("0");

        org.mockito.Mockito.verify(tickets).countByOrganizer(organizerId);
        org.mockito.Mockito.verify(events).countByOrganizerId(organizerId);
        org.mockito.Mockito.verify(checkIns, org.mockito.Mockito.never()).countByEvent(any());
    }
}
