package com.uniondesk.common.event;

public sealed interface UnionDeskDomainEvent permits
        TicketStatusChangedEvent,
        DomainMemberStatusChangedEvent,
        RolePermissionsChangedEvent {
}
