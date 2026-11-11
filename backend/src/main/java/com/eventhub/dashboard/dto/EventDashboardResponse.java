package com.eventhub.dashboard.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record EventDashboardResponse(
        UUID eventId,
        long ticketsSold,
        long ticketsAvailable,
        BigDecimal revenue,
        long checkIns,
        long uniqueAttendees) {
}
