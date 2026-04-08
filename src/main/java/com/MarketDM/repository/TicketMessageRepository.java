package com.MarketDM.repository;

import com.MarketDM.entity.support.TicketMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TicketMessageRepository extends JpaRepository<TicketMessage, Long> {
    List<TicketMessage> findByTicketIdOrderBySentAtAsc(Long ticketId);
}
