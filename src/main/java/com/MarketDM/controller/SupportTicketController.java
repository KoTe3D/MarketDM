package com.MarketDM.controller;

import com.MarketDM.entity.support.*;
import com.MarketDM.repository.*;
import com.MarketDM.service.OperatorRoutingService;
import com.MarketDM.service.TemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@RestController
@RequestMapping("/api/support")
public class SupportTicketController {

    private final AtomicLong ticketCounter = new AtomicLong(1);

    @Autowired
    private SupportTicketRepository ticketRepository;

    @Autowired
    private TicketMessageRepository messageRepository;

    @Autowired
    private OperatorRoutingService routingService;

    @Autowired
    private TemplateService templateService;

    // НОВЫЙ МЕТОД: для внутреннего использования
    private void sendMessage(Long ticketId, String message, boolean fromOperator, String senderName) {
        SupportTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Тикет не найден"));

        TicketMessage msg = TicketMessage.builder()
                .ticket(ticket)
                .message(message)
                .fromOperator(fromOperator)
                .senderName(senderName)
                .senderId(fromOperator ? 999L : 0L) // системный ID
                .build();

        messageRepository.save(msg);
    }

    @PostMapping("/tickets")
    public ResponseEntity<SupportTicket> createTicket(@RequestBody TicketRequest request) {
        SupportTicket ticket = new SupportTicket();
        ticket.setUserId(1L);
        ticket.setSubject(request.getSubject());
        ticket.setDescription(request.getDescription());
        ticket.setTicketNumber(generateTicketNumber());
        ticket.setStatus(TicketStatus.OPEN);

        ticket = ticketRepository.save(ticket);

        Long operatorId = routingService.assignOperator(ticket);
        if (operatorId != null) {
            ticket.setOperatorId(operatorId);
            ticket.setStatus(TicketStatus.IN_PROGRESS);
            ticketRepository.save(ticket);
        }

        sendMessage(ticket.getId(), "Тикет создан. Ожидайте ответа оператора.", true, "Система");

        return ResponseEntity.ok(ticket);
    }

    @PostMapping("/tickets/{ticketId}/messages")
    public ResponseEntity<TicketMessage> sendMessage(
            @PathVariable Long ticketId,
            @RequestBody MessageRequest request) {

        SupportTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Тикет не найден"));

        String processedMessage = templateService.processMessage(request.getMessage());

        TicketMessage message = TicketMessage.builder()
                .ticket(ticket)
                .senderId(1L)
                .senderName("Пользователь")
                .fromOperator(false)
                .message(processedMessage)
                .build();

        message = messageRepository.save(message);
        return ResponseEntity.ok(message);
    }

    @GetMapping("/tickets/{ticketId}/messages")
    public ResponseEntity<List<TicketMessage>> getMessages(@PathVariable Long ticketId) {
        return ResponseEntity.ok(messageRepository.findByTicketIdOrderBySentAtAsc(ticketId));
    }

    @PostMapping("/rate")
    public ResponseEntity<?> submitRating(@RequestBody RatingRequest request) {
        SupportTicket ticket = ticketRepository.findById(request.getTicketId())
                .orElseThrow();
        ticket.setRating(request.getRating());
        ticket.setFeedback(request.getFeedback());
        ticket.setStatus(TicketStatus.CLOSED);
        ticket.setClosedAt(LocalDateTime.now());
        ticketRepository.save(ticket);
        return ResponseEntity.ok().build();
    }

    private String generateTicketNumber() {
        long num = ticketCounter.getAndIncrement();
        int year = LocalDateTime.now().getYear();
        return String.format("SUP-%d-%06d", year, num);
    }

    static class TicketRequest {
        private String subject, description;
        public String getSubject() { return subject; }
        public void setSubject(String subject) { this.subject = subject; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }

    static class MessageRequest {
        private String message;
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }

    static class RatingRequest {
        private Long ticketId;
        private Integer rating;
        private String feedback;
        // getters
        public Long getTicketId() { return ticketId; }
        public Integer getRating() { return rating; }
        public String getFeedback() { return feedback; }
    }
}
