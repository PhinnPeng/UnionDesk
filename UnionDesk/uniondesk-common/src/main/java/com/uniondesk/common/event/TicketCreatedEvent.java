package com.uniondesk.common.event;

import java.time.LocalDateTime;

/**
 * 工单创建事件（客户视角实时推送：ticket.created）。
 */
public record TicketCreatedEvent(
        long businessDomainId,
        long ticketId,
        String ticketNo,
        Long customerId,
        long ticketTypeId) implements UnionDeskDomainEvent {
}
