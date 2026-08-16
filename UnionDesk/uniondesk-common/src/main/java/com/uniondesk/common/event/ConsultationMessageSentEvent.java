package com.uniondesk.common.event;

import java.time.LocalDateTime;

/**
 * 咨询消息发送事件（实时推送：chat.message，仅推消息接收方）。
 */
public record ConsultationMessageSentEvent(
        long businessDomainId,
        String sessionNo,
        long senderUserId,
        String senderRole,
        Long recipientUserId,
        String recipientRole,
        long messageId,
        String content,
        LocalDateTime createdAt) implements UnionDeskDomainEvent {
}
