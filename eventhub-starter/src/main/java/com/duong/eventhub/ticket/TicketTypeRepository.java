package com.duong.eventhub.ticket;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketTypeRepository extends JpaRepository<TicketType, UUID> {
    boolean existsByEvent_IdAndName(UUID eventId, String name);
}
