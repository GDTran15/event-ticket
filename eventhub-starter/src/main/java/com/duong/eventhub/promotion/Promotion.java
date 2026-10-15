package com.duong.eventhub.promotion;

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
@Table(name = "promotions")
@Getter @Setter @NoArgsConstructor
public class Promotion {
    @Id
    @Column(nullable = false, updatable = false)
    private UUID id = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
        @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(nullable = false, length = 64, unique = true)
    private String code;

    @Enumerated(EnumType.STRING)
        @Column(nullable = false, length = 32)
    private DiscountType discountType;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal discountValue;

    @Column(nullable = false)
    private Instant startsAt;

    @Column(nullable = false)
    private Instant endsAt;

    @Column(nullable = false)
    private int maxUses;

    @Column(nullable = false)
    private int usedCount;

}
