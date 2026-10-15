package com.duong.eventhub.checkin;

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
import java.util.UUID;

@Entity
@Table(name = "check_ins")
@Getter @Setter @NoArgsConstructor
public class CheckIn {
    @Id
    @Column(nullable = false, updatable = false)
    private UUID id = UUID.randomUUID();

    @OneToOne(fetch = FetchType.LAZY, optional = false)
        @JoinColumn(name = "ticket_id", nullable = false, unique = true)
    private Ticket ticket;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
        @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
        @JoinColumn(name = "staff_id", nullable = false)
    private AppUser staff;

    @CreationTimestamp
        @Column(nullable = false, updatable = false)
    private Instant checkedInAt;

}
