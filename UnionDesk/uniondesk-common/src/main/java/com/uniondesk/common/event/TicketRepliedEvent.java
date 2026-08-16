package com.uniondesk.common.event;

import java.time.LocalDateTime;

/**
 * 工单回复事件（客户视角实时推送：ticket.replied）。
 */
public record TicketRepliedEvent(
        long businessDomainId,
        long ticketId,
        Long customerId,
        String senderType,
        String content,
        LocalDateTime repliedAt) implements UnionDeskDomainEvent {
}
