package com.uniondesk.common.event;

public sealed interface UnionDeskDomainEvent permits
        TicketStatusChangedEvent,
        TicketCreatedEvent,
        TicketRepliedEvent,
        ConsultationMessageSentEvent,
        ConsultationSessionChangedEvent,
        ConsultationQueuedEvent,
        InboxCreatedEvent,
        DomainMemberStatusChangedEvent,
        DomainMemberChangedEvent,
        DomainRoleChangedEvent,
        RolePermissionsChangedEvent {
}
