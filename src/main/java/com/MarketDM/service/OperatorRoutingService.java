package com.MarketDM.service;

import com.MarketDM.entity.support.SupportTicket;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
public class OperatorRoutingService {

    private final Map<Long, OperatorStatus> operators = new ConcurrentHashMap<>();
    private final Map<Long, List<Long>> operatorTickets = new ConcurrentHashMap<>();
    private final AtomicInteger roundRobinIndex = new AtomicInteger(0);
    private final Queue<WaitingUser> waitingQueue = new LinkedList<>();

    public Long assignOperator(SupportTicket ticket) {
        Long operatorId = findAvailableOperator();
        if (operatorId != null) {
            assignTicket(operatorId, ticket.getId());
            log.info("Тикет {} назначен оператору {}", ticket.getId(), operatorId);
            return operatorId;
        }
        waitingQueue.add(new WaitingUser(ticket.getUserId(), ticket.getId()));
        log.warn("Нет свободных операторов. Тикет {} в очереди.", ticket.getId());
        return null;
    }

    private Long findAvailableOperator() {
        return operators.entrySet().stream()
                .filter(e -> e.getValue() == OperatorStatus.ONLINE)
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }

    public void assignTicket(Long operatorId, Long ticketId) {
        operatorTickets.computeIfAbsent(operatorId, k -> new ArrayList<>()).add(ticketId);
        operators.put(operatorId, OperatorStatus.BUSY);
    }

    public void reassignTicket(Long ticketId) {
        log.info("Переназначение тикета {}", ticketId);
    }

    public void operatorOnline(Long operatorId) {
        operators.put(operatorId, OperatorStatus.ONLINE);
        operatorTickets.putIfAbsent(operatorId, new ArrayList<>());
        processWaitingQueue();
    }

    public void operatorOffline(Long operatorId) {
        operators.put(operatorId, OperatorStatus.OFFLINE);
        List<Long> tickets = operatorTickets.get(operatorId);
        if (tickets != null) {
            for (Long ticketId : tickets) reassignTicket(ticketId);
        }
    }

    public void operatorFree(Long operatorId) {
        operators.put(operatorId, OperatorStatus.ONLINE);
        processWaitingQueue();
    }

    private void processWaitingQueue() {
        while (!waitingQueue.isEmpty() && hasAvailableOperator()) {
            WaitingUser user = waitingQueue.poll();
            log.info("Пользователь {} из очереди получил оператора", user.userId);
        }
    }

    private boolean hasAvailableOperator() {
        return operators.values().stream().anyMatch(s -> s == OperatorStatus.ONLINE);
    }

    enum OperatorStatus { ONLINE, BUSY, OFFLINE }

    static class WaitingUser {
        final Long userId;
        final Long ticketId;
        final LocalDateTime waitingSince = LocalDateTime.now();
        WaitingUser(Long userId, Long ticketId) {
            this.userId = userId;
            this.ticketId = ticketId;
        }
    }
}
