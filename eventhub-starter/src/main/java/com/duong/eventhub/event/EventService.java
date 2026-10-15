package com.duong.eventhub.event;

import com.duong.eventhub.event.dto.EventRequest;
import com.duong.eventhub.event.dto.EventResponse;
import com.duong.eventhub.user.AppUserRepository;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class EventService {
    private final EventRepository events;
    private final AppUserRepository users;

    public EventService(EventRepository events, AppUserRepository users) {
        this.events = events;
        this.users = users;
    }

    @Transactional
    @PreAuthorize("hasRole('ORGANIZER')")
    public EventResponse create(UUID organizerId, EventRequest request) {
        if (!request.endsAt().isAfter(request.startsAt())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "endsAt must be after startsAt");
        }
        Event event = new Event();
        event.setOrganizer(users.findById(organizerId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found")));
        event.setTitle(request.title());
        event.setDescription(request.description());
        event.setVenue(request.venue());
        event.setStartsAt(request.startsAt());
        event.setEndsAt(request.endsAt());
        event.setStatus(EventStatus.DRAFT);
        return EventResponse.from(events.saveAndFlush(event));
    }
}
