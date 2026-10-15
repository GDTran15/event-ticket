package com.duong.eventhub.event.dto;

import com.duong.eventhub.event.Event;
import com.duong.eventhub.event.EventStatus;

import java.time.Instant;
import java.util.UUID;

public record EventResponse(
        UUID id, UUID organizerId, String title, String description, String venue,
        Instant startsAt, Instant endsAt, EventStatus status, Instant createdAt
) {
    public static EventResponse from(Event event) {
        return new EventResponse(event.getId(), event.getOrganizer().getId(), event.getTitle(),
            event.getDescription(), event.getVenue(), event.getStartsAt(), event.getEndsAt(),
            event.getStatus(), event.getCreatedAt());
    }
}
