package com.duong.eventhub.ticket;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/events/{eventId}/ticket-types")
public class TicketTypeController {
    private final TicketTypeService types;

    public TicketTypeController(TicketTypeService types) {
        this.types = types;
    }

    @PostMapping
    public ResponseEntity<TicketTypeResponse> create(@PathVariable UUID eventId,
            @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody TicketTypeRequest request) {
        TicketTypeResponse type = types.create(eventId, UUID.fromString(jwt.getSubject()), request);
        return ResponseEntity.created(URI.create("/api/events/" + eventId + "/ticket-types/" + type.id())).body(type);
    }
}
