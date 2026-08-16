package com.uniondesk.realtime;

import com.uniondesk.common.event.ConsultationMessageSentEvent;
import com.uniondesk.common.event.ConsultationQueuedEvent;
import com.uniondesk.common.event.ConsultationSessionChangedEvent;
import com.uniondesk.common.event.InboxCreatedEvent;
import com.uniondesk.common.event.TicketCreatedEvent;
import com.uniondesk.common.event.TicketRepliedEvent;
import com.uniondesk.common.event.TicketStatusChangedEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 业务事件 → 实时推送（AFTER_COMMIT + Async，与站内信监听同语义）：
 * 事务提交后才推送，避免回滚后推送不一致；离线接收人由重连拉取兜底。
 */
@Component
public class RealtimeEventListener {

    private final RealtimeEventPublisher publisher;

    public RealtimeEventListener(RealtimeEventPublisher publisher) {
        this.publisher = publisher;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onConsultationMessageSent(ConsultationMessageSentEvent event) {
        if (event.recipientUserId() == null) {
            return;
        }
        String recipientActor = RealtimeConstants.ACTOR_CUSTOMER.equals(event.recipientRole())
                ? RealtimeConstants.ACTOR_CUSTOMER
                : RealtimeConstants.ACTOR_STAFF;
        publisher.publishToUser(recipientActor, event.recipientUserId(), envelope(
                RealtimeConstants.EVT_CHAT_MESSAGE,
                payload(
                        "sessionNo", event.sessionNo(),
                        "senderRole", event.senderRole(),
                        "messageId", event.messageId(),
                        "content", event.content(),
                        "createdAt", event.createdAt() == null ? null : event.createdAt().toString())));
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onConsultationSessionChanged(ConsultationSessionChangedEvent event) {
        publisher.publishToUser(RealtimeConstants.ACTOR_CUSTOMER, event.customerId(), envelope(
                RealtimeConstants.EVT_CHAT_SESSION,
                payload(
                        "sessionNo", event.sessionNo(),
                        "status", event.status(),
                        "assignedTo", event.assignedTo(),
                        "linkedTicketNo", event.linkedTicketNo())));
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onConsultationQueued(ConsultationQueuedEvent event) {
        publisher.publishToDomainStaff(event.businessDomainId(), envelope(
                RealtimeConstants.EVT_CHAT_QUEUE,
                payload("queueSize", event.queueSize())));
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTicketCreated(TicketCreatedEvent event) {
        if (event.customerId() == null) {
            return;
        }
        publisher.publishToUser(RealtimeConstants.ACTOR_CUSTOMER, event.customerId(), envelope(
                RealtimeConstants.EVT_TICKET_CREATED,
                payload(
                        "ticketId", event.ticketId(),
                        "ticketNo", event.ticketNo(),
                        "ticketTypeId", event.ticketTypeId(),
                        "domainId", event.businessDomainId())));
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTicketReplied(TicketRepliedEvent event) {
        if (event.customerId() == null) {
            return;
        }
        publisher.publishToUser(RealtimeConstants.ACTOR_CUSTOMER, event.customerId(), envelope(
                RealtimeConstants.EVT_TICKET_REPLIED,
                payload(
                        "ticketId", event.ticketId(),
                        "senderType", event.senderType(),
                        "content", event.content(),
                        "repliedAt", event.repliedAt() == null ? null : event.repliedAt().toString())));
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTicketStatusChanged(TicketStatusChangedEvent event) {
        if (event.customerId() == null) {
            return;
        }
        publisher.publishToUser(RealtimeConstants.ACTOR_CUSTOMER, event.customerId(), envelope(
                RealtimeConstants.EVT_TICKET_UPDATED,
                payload(
                        "ticketId", event.ticketId(),
                        "status", event.newStatus(),
                        "actorUserId", event.actorUserId())));
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onInboxCreated(InboxCreatedEvent event) {
        publisher.publishToUser(event.recipientActor(), event.recipientUserId(), envelope(
                RealtimeConstants.EVT_INBOX_NEW,
                payload(
                        "messageId", event.messageId(),
                        "templateCode", event.templateCode(),
                        "unreadCount", event.unreadCount())));
    }

    /** 构建可空 payload（Map.of 拒绝 null，推送帧允许 null 字段） */
    private static Map<String, Object> payload(Object... kv) {
        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            map.put(String.valueOf(kv[i]), kv[i + 1]);
        }
        return map;
    }

    private RealtimeEnvelope envelope(String type, Map<String, Object> payload) {
        return RealtimeEnvelope.of("evt_" + UUID.randomUUID(), type, System.currentTimeMillis(), payload);
    }
}
