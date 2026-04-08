package com.MarketDM.repository;

import com.MarketDM.entity.support.SupportTicket;
import com.MarketDM.entity.support.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {

    List<SupportTicket> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<SupportTicket> findByOperatorIdAndStatusIn(Long operatorId, List<TicketStatus> statuses);

    @Query("SELECT t FROM SupportTicket t WHERE " +
            "LOWER(t.subject) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(t.description) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(t.ticketNumber) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<SupportTicket> searchTickets(@Param("query") String query);

    @Query("SELECT COUNT(t) FROM SupportTicket t WHERE t.createdAt BETWEEN :start AND :end")
    long countTicketsInPeriod(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT AVG(t.rating) FROM SupportTicket t WHERE t.operatorId = :operatorId AND t.rating IS NOT NULL")
    Double getOperatorRating(@Param("operatorId") Long operatorId);

    Optional<SupportTicket> findByTicketNumber(String ticketNumber);
}
