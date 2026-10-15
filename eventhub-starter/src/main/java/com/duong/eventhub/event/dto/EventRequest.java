package com.duong.eventhub.event.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record EventRequest(
    @NotBlank
    @Size(max = 200)
    String title,
    String description,
    @NotBlank
    @Size(max = 250)
    String venue,
    @NotNull
    @Future
    Instant startsAt,
    @NotNull
    Instant endsAt
) {
    public EventRequest {
        title = title == null ? null : title.strip();
        venue = venue == null ? null : venue.strip();
    }
}
