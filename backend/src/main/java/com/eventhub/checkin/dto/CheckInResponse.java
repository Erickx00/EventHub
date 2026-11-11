package com.eventhub.checkin.dto;

import java.time.Instant;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;

public record CheckInResponse(UUID ticketId, UUID eventId, Instant checkedAt, UUID validatedBy) {
}
