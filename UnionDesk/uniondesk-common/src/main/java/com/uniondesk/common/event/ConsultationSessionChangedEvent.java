package com.uniondesk.common.event;

/**
 * 咨询会话状态变化事件（实时推送：chat.session，客户感知接入/结束/转单）。
 */
public record ConsultationSessionChangedEvent(
        long businessDomainId,
        String sessionNo,
        long customerId,
        String status,
        Long assignedTo,
        String linkedTicketNo) implements UnionDeskDomainEvent {
}
