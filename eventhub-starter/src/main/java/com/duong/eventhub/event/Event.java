package com.duong.eventhub.event;

import com.duong.eventhub.user.AppUser;
import com.duong.eventhub.event.Event;
import com.duong.eventhub.ticket.TicketType;
import com.duong.eventhub.ticket.Ticket;
import com.duong.eventhub.reservation.Reservation;
import com.duong.eventhub.promotion.Promotion;
import com.duong.eventhub.orders.TicketOrder;
import com.duong.eventhub.orders.OrderItem;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "events")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Event {
    @Id
    @Column(nullable = false, updatable = false)
    private UUID id = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
        @JoinColumn(name = "organizer_id", nullable = false)
    private AppUser organizer;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Column(nullable = false, length = 250)
    private String venue;

    @Column(nullable = false)
    private Instant startsAt;

    @Column(nullable = false)
    private Instant endsAt;

    @Enumerated(EnumType.STRING)
        @Column(nullable = false, length = 32)
    private EventStatus status;

    @CreationTimestamp
        @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @ManyToMany
    @JoinTable(name = "event_staff", joinColumns = @JoinColumn(name = "event_id"), inverseJoinColumns = @JoinColumn(name = "user_id"))
    private Set<AppUser> staff = new java.util.HashSet<>();

}
