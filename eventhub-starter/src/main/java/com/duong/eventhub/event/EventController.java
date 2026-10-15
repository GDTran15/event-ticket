package com.duong.eventhub.event;

import com.duong.eventhub.event.dto.EventRequest;
import com.duong.eventhub.event.dto.EventResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/events")
public class EventController {
    private final EventService events;

    public EventController(EventService events) {
        this.events = events;
    }

    @PostMapping
    public ResponseEntity<EventResponse> create(@AuthenticationPrincipal Jwt jwt,
                                                @Valid @RequestBody EventRequest request) {
        EventResponse event = events.create(UUID.fromString(jwt.getSubject()), request);
        return ResponseEntity.created(URI.create("/api/events/" + event.id())).body(event);
    }
}
