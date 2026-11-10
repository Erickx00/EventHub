package com.eventhub.ticket.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.eventhub.shared.exception.BusinessException;
import com.eventhub.shared.exception.ErrorCode;

class TicketTypeTest {

    private static final Instant SALES_START = Instant.now().minus(1, ChronoUnit.DAYS);
    private static final Instant SALES_END = Instant.now().plus(30, ChronoUnit.DAYS);

    private TicketType ticketType(int total, int maxPerOrder) {
        return TicketType.create(UUID.randomUUID(), "Pista", null, new BigDecimal("50.00"), total, maxPerOrder,
                SALES_START, SALES_END);
    }

    /** Simula vendas ja ocorridas (no banco o estoque so muda por UPDATE). */
    private static void setAvailable(TicketType ticketType, int available) {
        try {
            Field field = TicketType.class.getDeclaredField("availableQuantity");
            field.setAccessible(true);
            field.setInt(ticketType, available);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }

    @Test
    void startsWithFullStockAvailable() {
        TicketType created = ticketType(200, 5);

        assertThat(created.getAvailableQuantity()).isEqualTo(200);
        assertThat(created.getTotalQuantity()).isEqualTo(200);
        assertThat(created.hasSales()).isFalse();
    }

    @Test
    void refusesNegativePrice() {
        assertThatThrownBy(() -> TicketType.create(UUID.randomUUID(), "VIP", null, new BigDecimal("-1.00"), 10, 2,
                SALES_START, SALES_END))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.INVALID_TICKET_TYPE);
    }

    @Test
    void refusesLimitPerOrderAboveStock() {
        assertThatThrownBy(() -> ticketType(10, 11))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.INVALID_TICKET_TYPE);
    }

    @Test
    void refusesSalesWindowThatEndsBeforeItStarts() {
        assertThatThrownBy(() -> TicketType.create(UUID.randomUUID(), "Pista", null, BigDecimal.TEN, 10, 2,
                SALES_END, SALES_START))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.INVALID_TICKET_TYPE);
    }

    @Test
    void increasingTotalAddsEquivalentStock() {
        TicketType created = ticketType(100, 4);
        setAvailable(created, 90); // 10 vendidos

        created.updateDetails("Pista", null, new BigDecimal("60.00"), 120, 4, SALES_START, SALES_END);

        assertThat(created.getTotalQuantity()).isEqualTo(120);
        assertThat(created.getAvailableQuantity()).isEqualTo(110);
    }

    @Test
    void refusesReduceBelowSoldQuantity() {
        TicketType created = ticketType(100, 4);
        setAvailable(created, 90); // 10 vendidos

        assertThatThrownBy(() -> created.updateDetails("Pista", null, new BigDecimal("50.00"), 5, 4,
                SALES_START, SALES_END))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.INVALID_TICKET_TYPE)
                .hasMessageContaining("10");
    }

    @Test
    void refusesPurchaseOutsideSalesWindow() {
        TicketType created = ticketType(10, 2);
        Instant afterSales = SALES_END.plus(1, ChronoUnit.DAYS);

        assertThatThrownBy(() -> created.assertPurchasable(1, afterSales))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.SALES_CLOSED);
    }

    @Test
    void refusesQuantityAbovePerOrderLimit() {
        TicketType created = ticketType(10, 2);

        assertThatThrownBy(() -> created.assertPurchasable(3, Instant.now()))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.TICKET_LIMIT_EXCEEDED);
    }

    @Test
    void refusesQuantityAboveAvailableStock() {
        TicketType created = ticketType(10, 5);
        setAvailable(created, 1);

        assertThatThrownBy(() -> created.assertPurchasable(2, Instant.now()))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.TICKET_SOLD_OUT);
    }

    @Test
    void acceptsPurchaseInsideAllLimits() {
        TicketType created = ticketType(10, 5);

        created.assertPurchasable(3, Instant.now());

        assertThat(created.getAvailableQuantity()).isEqualTo(10);
    }
}
