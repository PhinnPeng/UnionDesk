package com.uniondesk.common.event;

public record DomainMemberStatusChangedEvent(
        long businessDomainId,
        long memberId,
        long staffAccountId,
        String previousStatus,
        String newStatus,
        long operatorUserId) implements UnionDeskDomainEvent {
}
