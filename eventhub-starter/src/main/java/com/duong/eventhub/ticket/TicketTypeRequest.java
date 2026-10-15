package com.duong.eventhub.ticket;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;

public record TicketTypeRequest(
    @NotBlank @Size(max = 100) String name,
    @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal price,
    @NotNull @Positive Integer capacity,
    @NotNull Instant saleStartsAt,
    @NotNull Instant saleEndsAt
) {
    public TicketTypeRequest {
        name = name == null ? null : name.strip();
    }
}
