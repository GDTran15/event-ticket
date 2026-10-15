package com.duong.eventhub.payment;

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
@Table(name = "payments")
@Getter @Setter @NoArgsConstructor
public class Payment {
    @Id
    @Column(nullable = false, updatable = false)
    private UUID id = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
        @JoinColumn(name = "order_id", nullable = false)
    private TicketOrder order;

    @Column(nullable = false, unique = true, length = 128)
    private String idempotencyKey;

    @Column(nullable = false, length = 64)
    private String requestHash;

    @Column(length = 128)
    private String providerReference;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
        @Column(nullable = false, length = 32)
    private PaymentStatus status;

    @CreationTimestamp
        @Column(nullable = false, updatable = false)
    private Instant createdAt;

}
