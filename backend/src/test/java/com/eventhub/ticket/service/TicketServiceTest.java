package com.eventhub.ticket.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.eventhub.checkin.entity.CheckIn;
import com.eventhub.checkin.repository.CheckInRepository;
import com.eventhub.event.service.EventService;
import com.eventhub.security.AuthPrincipal;
import com.eventhub.shared.exception.BusinessException;
import com.eventhub.shared.exception.ErrorCode;
import com.eventhub.ticket.dto.TicketIssuance;
import com.eventhub.ticket.entity.Ticket;
import com.eventhub.ticket.entity.TicketStatus;
import com.eventhub.ticket.repository.TicketRepository;
import com.eventhub.user.entity.Role;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    @Mock
    private TicketRepository tickets;

    @Mock
    private CheckInRepository checkIns;

    @Mock
    private EventService events;

    @Mock
    private QrCodeService qrCodes;

    private TicketService ticketService;

    private final UUID userId = UUID.randomUUID();
    private final UUID eventId = UUID.randomUUID();
    private final AuthPrincipal attendee = new AuthPrincipal(userId, "ana@example.com", Role.ATTENDEE);
    private final AuthPrincipal organizer = new AuthPrincipal(UUID.randomUUID(), "org@example.com", Role.ORGANIZER);

    @BeforeEach
    void setUp() {
        ticketService = new TicketService(tickets, checkIns, events, qrCodes);
    }

    private Ticket ticketOwnedBy(UUID owner) {
        Ticket ticket = Ticket.issue(UUID.randomUUID(), UUID.randomUUID(), eventId, UUID.randomUUID(), owner,
                "token-" + UUID.randomUUID());
        setField(ticket, "id", UUID.randomUUID());
        return ticket;
    }

    private static void setField(Object target, String name, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(name);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }

    @Test
    void issuesOneTicketPerPurchasedUnit() {
        var issuance = new TicketIssuance(UUID.randomUUID(), userId, eventId, List.of(
                new TicketIssuance.Line(UUID.randomUUID(), UUID.randomUUID(), 2),
                new TicketIssuance.Line(UUID.randomUUID(), UUID.randomUUID(), 1)));

        ticketService.issueFor(issuance);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Ticket>> saved = ArgumentCaptor.forClass(List.class);
        verify(tickets).saveAll(saved.capture());

        List<Ticket> issued = saved.getValue();
        assertThat(issued).hasSize(3);
        assertThat(issued).allMatch(ticket -> ticket.getUserId().equals(userId));
        assertThat(issued).allMatch(ticket -> ticket.getEventId().equals(eventId));
        assertThat(issued).allMatch(ticket -> ticket.getStatus() == TicketStatus.VALID);
        assertThat(issued.stream().map(Ticket::getQrToken).distinct()).hasSize(3);
    }

    @Test
    void issuesNoTicketWhenOrderHasNoItems() {
        ticketService.issueFor(new TicketIssuance(UUID.randomUUID(), userId, eventId, List.of()));

        verify(tickets).saveAll(List.of());
    }

    @Test
    void listsOnlyOwnTickets() {
        when(tickets.findAllByUserIdOrderByIssuedAtDesc(userId)).thenReturn(List.of(ticketOwnedBy(userId)));

        var mine = ticketService.findMine(attendee);

        assertThat(mine).hasSize(1);
    }

    @Test
    void hidesTicketFromAnotherUser() {
        Ticket ticket = ticketOwnedBy(UUID.randomUUID());
        when(tickets.findById(ticket.getId())).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> ticketService.getById(ticket.getId(), attendee))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.TICKET_NOT_FOUND);
    }

    @Test
    void validatesTicketAndRecordsCheckIn() {
        Ticket ticket = ticketOwnedBy(userId);
        when(tickets.findByQrToken(ticket.getQrToken())).thenReturn(Optional.of(ticket));
        when(tickets.markUsed(ticket.getId(), TicketStatus.USED, TicketStatus.VALID)).thenReturn(1);
        when(checkIns.save(any(CheckIn.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = ticketService.validate(ticket.getQrToken(), organizer);

        assertThat(response.ticketId()).isEqualTo(ticket.getId());
        assertThat(response.validatedBy()).isEqualTo(organizer.userId());
        verify(checkIns).save(any(CheckIn.class));
    }

    @Test
    void refusesSecondValidationOfTheSameTicket() {
        Ticket ticket = ticketOwnedBy(userId);
        when(tickets.findByQrToken(ticket.getQrToken())).thenReturn(Optional.of(ticket));
        // 0 linhas = outra requisisao ja marcou USED primeiro.
        when(tickets.markUsed(ticket.getId(), TicketStatus.USED, TicketStatus.VALID)).thenReturn(0);

        assertThatThrownBy(() -> ticketService.validate(ticket.getQrToken(), organizer))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.TICKET_ALREADY_USED);

        verify(checkIns, never()).save(any());
    }

    @Test
    void refusesUnknownQrToken() {
        when(tickets.findByQrToken(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ticketService.validate("token-inexistente", organizer))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.INVALID_QR_TOKEN);
    }

    @Test
    void refusesValidationBySomeoneWhoDoesNotManageTheEvent() {
        Ticket ticket = ticketOwnedBy(userId);
        when(tickets.findByQrToken(ticket.getQrToken())).thenReturn(Optional.of(ticket));
        when(events.requireManageable(ticket.getEventId(), organizer))
                .thenThrow(BusinessException.forbidden(ErrorCode.FORBIDDEN, "nao gerencia o evento"));

        assertThatThrownBy(() -> ticketService.validate(ticket.getQrToken(), organizer))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.FORBIDDEN);

        verify(tickets, never()).markUsed(any(), any(), any());
    }

    @Test
    void rendersQrCodeOfOwnTicket() {
        Ticket ticket = ticketOwnedBy(userId);
        when(tickets.findById(ticket.getId())).thenReturn(Optional.of(ticket));
        when(qrCodes.pngFor(ticket.getQrToken())).thenReturn(new byte[] { 1, 2, 3 });

        byte[] png = ticketService.qrCodeOf(ticket.getId(), attendee);

        assertThat(png).containsExactly(1, 2, 3);
    }

    @Test
    void ticketKeepsIssuedTimestamp() {
        Ticket ticket = ticketOwnedBy(userId);
        setField(ticket, "issuedAt", Instant.parse("2026-11-10T10:00:00Z"));

        assertThat(ticket.getIssuedAt()).isEqualTo(Instant.parse("2026-11-10T10:00:00Z"));
    }
}
