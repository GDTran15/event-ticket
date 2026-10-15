package com.duong.eventhub.ticket;

import com.duong.eventhub.event.Event;
import com.duong.eventhub.event.EventRepository;
import com.duong.eventhub.event.EventStatus;
import java.math.RoundingMode;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TicketTypeService {
    private final TicketTypeRepository types;
    private final EventRepository events;

    public TicketTypeService(TicketTypeRepository types, EventRepository events) {
        this.types = types;
        this.events = events;
    }

    @Transactional
    @PreAuthorize("hasRole('ORGANIZER')")
    public TicketTypeResponse create(UUID eventId, UUID organizerId, TicketTypeRequest request) {
        Event event = events.findById(eventId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));
        if (!event.getOrganizer().getId().equals(organizerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the owning organizer can add ticket types");
        }
        if (event.getStatus() != EventStatus.DRAFT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ticket types can only be added to draft events");
        }
        if (!request.saleEndsAt().isAfter(request.saleStartsAt())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "saleEndsAt must be after saleStartsAt");
        }
        if (request.saleEndsAt().isAfter(event.getEndsAt())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ticket sales must end on or before the event ends");
        }
        if (types.existsByEvent_IdAndName(eventId, request.name())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ticket type name already exists for this event");
        }
        TicketType type = new TicketType();
        type.setEvent(event);
        type.setName(request.name());
        type.setPrice(request.price().setScale(2, RoundingMode.UNNECESSARY));
        type.setCapacity(request.capacity());
        type.setAvailableQuantity(request.capacity());
        type.setSaleStartsAt(request.saleStartsAt());
        type.setSaleEndsAt(request.saleEndsAt());
        return TicketTypeResponse.from(types.saveAndFlush(type));
    }
}
