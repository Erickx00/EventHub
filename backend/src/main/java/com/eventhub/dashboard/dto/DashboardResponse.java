package com.eventhub.dashboard.dto;

import java.math.BigDecimal;

/**
 * Numeros do organizador. Receita considera apenas pedidos PAID; ingressos
 * disponiveis vem do estoque atual de cada tipo.
 */
public record DashboardResponse(
        long totalEvents,
        long publishedEvents,
        long ticketsSold,
        long ticketsAvailable,
        BigDecimal revenue,
        long checkIns,
        long uniqueAttendees) {
}
