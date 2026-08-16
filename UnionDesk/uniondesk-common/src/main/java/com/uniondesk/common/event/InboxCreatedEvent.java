package com.uniondesk.common.event;

/**
 * 站内信创建事件（实时推送：inbox.new，收件人即时角标）。
 * recipientActor：customer / staff（决定推送路由维度）。
 */
public record InboxCreatedEvent(
        long recipientUserId,
        String recipientActor,
        long messageId,
        String templateCode,
        long unreadCount) implements UnionDeskDomainEvent {
}
