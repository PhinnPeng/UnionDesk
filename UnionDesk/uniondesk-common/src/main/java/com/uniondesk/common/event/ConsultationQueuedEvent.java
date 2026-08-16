package com.uniondesk.common.event;

/**
 * 咨询排队数变化事件（实时推送：chat.queue，域内在线客服感知排队规模）。
 */
public record ConsultationQueuedEvent(
        long businessDomainId,
        long queueSize) implements UnionDeskDomainEvent {
}
