package com.uniondesk.common.event;

public sealed interface UnionDeskDomainEvent permits
        TicketStatusChangedEvent,
        DomainMemberStatusChangedEvent,
        DomainMemberChangedEvent,
        DomainRoleChangedEvent,
        RolePermissionsChangedEvent {
}
