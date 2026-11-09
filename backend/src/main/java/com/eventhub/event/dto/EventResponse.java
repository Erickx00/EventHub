package com.eventhub.event.dto;

import java.time.Instant;
import java.util.UUID;

import com.eventhub.event.entity.Category;
import com.eventhub.event.entity.Event;
import com.eventhub.event.entity.EventStatus;

public record EventResponse(
        UUID id,
        String name,
        String description,
        Category category,
        String city,
        String venue,
        Instant startDate,
        Instant endDate,
        EventStatus status,
        UUID organizerId,
        Instant createdAt,
        Instant updatedAt) {

    public static EventResponse from(Event event) {
        return new EventResponse(
                event.getId(),
                event.getName(),
                event.getDescription(),
                event.getCategory(),
                event.getCity(),
                event.getVenue(),
                event.getStartDate(),
                event.getEndDate(),
                event.getStatus(),
                event.getOrganizerId(),
                event.getCreatedAt(),
                event.getUpdatedAt());
    }
}
