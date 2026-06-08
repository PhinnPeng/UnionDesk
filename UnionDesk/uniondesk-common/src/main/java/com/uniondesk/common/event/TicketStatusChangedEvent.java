package com.uniondesk.common.event;

public record TicketStatusChangedEvent(
        long businessDomainId,
        long ticketId,
        Long customerId,
        long actorUserId,
        String newStatus) implements UnionDeskDomainEvent {
}
