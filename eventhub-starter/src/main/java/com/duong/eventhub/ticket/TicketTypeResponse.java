package com.duong.eventhub.ticket;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TicketTypeResponse(
    UUID id, UUID eventId, String name, BigDecimal price, int capacity,
    int availableQuantity, Instant saleStartsAt, Instant saleEndsAt, long version
) {
    public static TicketTypeResponse from(TicketType type) {
        return new TicketTypeResponse(type.getId(), type.getEvent().getId(), type.getName(),
            type.getPrice(), type.getCapacity(), type.getAvailableQuantity(), type.getSaleStartsAt(),
            type.getSaleEndsAt(), type.getVersion());
    }
}
