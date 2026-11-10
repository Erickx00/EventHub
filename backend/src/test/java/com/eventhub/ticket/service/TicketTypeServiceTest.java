package com.eventhub.ticket.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.eventhub.event.service.EventService;
import com.eventhub.security.AuthPrincipal;
import com.eventhub.shared.exception.BusinessException;
import com.eventhub.shared.exception.ErrorCode;
import com.eventhub.ticket.dto.CreateTicketTypeRequest;
import com.eventhub.ticket.dto.UpdateTicketTypeRequest;
import com.eventhub.ticket.entity.TicketType;
import com.eventhub.ticket.repository.TicketTypeRepository;
import com.eventhub.user.entity.Role;

@ExtendWith(MockitoExtension.class)
class TicketTypeServiceTest {

    private static final Instant SALES_START = Instant.now().minus(1, ChronoUnit.DAYS);
    private static final Instant SALES_END = Instant.now().plus(30, ChronoUnit.DAYS);

    @Mock
    private TicketTypeRepository ticketTypes;

    @Mock
    private EventService events;

    private TicketTypeService ticketTypeService;

    private final UUID eventId = UUID.randomUUID();
    private final AuthPrincipal organizer = new AuthPrincipal(UUID.randomUUID(), "org@example.com", Role.ORGANIZER);

    @BeforeEach
    void setUp() {
        ticketTypeService = new TicketTypeService(ticketTypes, events);
    }

    private TicketType existingType() {
        return TicketType.create(eventId, "Pista", "Area comum", new BigDecimal("50.00"), 200, 5,
                SALES_START, SALES_END);
    }

    private CreateTicketTypeRequest createRequest() {
        return new CreateTicketTypeRequest("VIP", "Area premium", new BigDecimal("120.00"), 100, 4,
                SALES_START, SALES_END);
    }

    @Test
    void createsTicketTypeForOwnedEvent() {
        when(ticketTypes.save(any(TicketType.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = ticketTypeService.create(eventId, createRequest(), organizer);

        verify(events).requireManageable(eventId, organizer);
        assertThat(response.name()).isEqualTo("VIP");
        assertThat(response.price()).isEqualByComparingTo("120.00");
        assertThat(response.availableQuantity()).isEqualTo(100);
        assertThat(response.soldOut()).isFalse();
    }

    @Test
    void propagatesForbiddenWhenEventIsNotOwned() {
        when(events.requireManageable(eventId, organizer))
                .thenThrow(BusinessException.forbidden(ErrorCode.FORBIDDEN, "nao e dono"));

        assertThatThrownBy(() -> ticketTypeService.create(eventId, createRequest(), organizer))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.FORBIDDEN);

        verify(ticketTypes, never()).save(any());
    }

    @Test
    void listsTicketTypesOrderedForVisibleEvent() {
        when(ticketTypes.findAllByEventIdOrderByPriceAsc(eventId)).thenReturn(List.of(existingType()));

        var response = ticketTypeService.listByEvent(eventId, null);

        assertThat(response).hasSize(1);
        assertThat(response.get(0).name()).isEqualTo("Pista");
    }

    @Test
    void updatesTicketTypeOfOwnedEvent() {
        TicketType existing = existingType();
        when(ticketTypes.findById(any())).thenReturn(Optional.of(existing));

        var response = ticketTypeService.update(UUID.randomUUID(),
                new UpdateTicketTypeRequest("Camarote", null, new BigDecimal("250.00"), 50, 2, SALES_START, SALES_END),
                organizer);

        assertThat(response.name()).isEqualTo("Camarote");
        assertThat(response.totalQuantity()).isEqualTo(50);
        assertThat(response.availableQuantity()).isEqualTo(50);
    }

    @Test
    void refusesDeleteWhenTicketsWereAlreadySold() {
        TicketType existing = existingType();
        // 200 total, 200 disponiveis: sem vendas ainda. Simula uma venda.
        try {
            var field = TicketType.class.getDeclaredField("availableQuantity");
            field.setAccessible(true);
            field.setInt(existing, 199);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }

        when(ticketTypes.findById(any())).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> ticketTypeService.delete(UUID.randomUUID(), organizer))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.TICKET_TYPE_HAS_SALES);

        verify(ticketTypes, never()).delete(any());
    }

    @Test
    void deletesTicketTypeWithoutSales() {
        TicketType existing = existingType();
        when(ticketTypes.findById(any())).thenReturn(Optional.of(existing));

        ticketTypeService.delete(UUID.randomUUID(), organizer);

        verify(ticketTypes).delete(existing);
    }

    @Test
    void reserveUsesAtomicUpdateInsteadOfSavingTheEntity() {
        UUID id = UUID.randomUUID();
        TicketType existing = existingType();
        when(ticketTypes.findById(id)).thenReturn(Optional.of(existing));
        when(ticketTypes.reserveStock(id, 2)).thenReturn(1);

        ticketTypeService.reserve(id, 2);

        verify(ticketTypes).reserveStock(id, 2);
        verify(ticketTypes, never()).save(any());
    }

    @Test
    void reserveFailsWhenStockWasTakenByAnotherTransaction() {
        UUID id = UUID.randomUUID();
        TicketType existing = existingType();
        when(ticketTypes.findById(id)).thenReturn(Optional.of(existing));
        // 0 linhas = o WHERE nao casou, ou seja, o estoque acabou entre a leitura e o update.
        when(ticketTypes.reserveStock(id, 2)).thenReturn(0);

        assertThatThrownBy(() -> ticketTypeService.reserve(id, 2))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.TICKET_SOLD_OUT);
    }

    @Test
    void reserveRejectsQuantityAbovePerOrderLimitBeforeTouchingStock() {
        UUID id = UUID.randomUUID();
        TicketType existing = existingType();
        when(ticketTypes.findById(id)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> ticketTypeService.reserve(id, 6))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.TICKET_LIMIT_EXCEEDED);

        verify(ticketTypes, never()).reserveStock(any(), anyInt());
    }

    @Test
    void releaseDelegatesToAtomicIncrease() {
        ticketTypeService.release(UUID.randomUUID(), 3);

        verify(ticketTypes).releaseStock(any(), anyInt());
    }
}
