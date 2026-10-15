package com.duong.eventhub.ticket;

import com.duong.eventhub.user.AppUser;
import com.duong.eventhub.event.Event;
import com.duong.eventhub.ticket.TicketType;
import com.duong.eventhub.ticket.Ticket;
import com.duong.eventhub.reservation.Reservation;
import com.duong.eventhub.promotion.Promotion;
import com.duong.eventhub.orders.TicketOrder;
import com.duong.eventhub.orders.OrderItem;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "ticket_types")
@Getter @Setter @NoArgsConstructor
public class TicketType {
    @Id
    @Column(nullable = false, updatable = false)
    private UUID id = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
        @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private int capacity;

    @Column(nullable = false)
    private int availableQuantity;

    @Column(nullable = false)
    private Instant saleStartsAt;

    @Column(nullable = false)
    private Instant saleEndsAt;

    @Version
        @Column(nullable = false)
    private long version;


}
