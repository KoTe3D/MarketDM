package com.MarketDM.entity.support;

import lombok.*;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "ticket_messages")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false)
    private SupportTicket ticket;

    @Column(nullable = false)
    private Long senderId;

    private String senderName;

    @Column(nullable = false)
    private boolean fromOperator;

    @Column(length = 10000, nullable = false)
    private String message;

    private LocalDateTime sentAt = LocalDateTime.now();

    private boolean isRead = false;
    private LocalDateTime readAt;

    @ElementCollection
    private List<String> attachments;

    @PrePersist
    public void onCreate() {
        if (sentAt == null) {
            sentAt = LocalDateTime.now();
        }
    }
}