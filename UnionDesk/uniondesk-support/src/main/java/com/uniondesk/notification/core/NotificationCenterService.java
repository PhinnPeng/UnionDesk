package com.uniondesk.notification.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uniondesk.audit.entity.AuditLogWritePo;
import com.uniondesk.audit.repository.AuditLogWriteRepository;
import com.uniondesk.common.repository.IdentityResolutionRepository;
import com.uniondesk.notification.entity.InboxMessagePo;
import com.uniondesk.notification.entity.NotificationLogPo;
import com.uniondesk.notification.repository.InboxMessageRepository;
import com.uniondesk.notification.repository.NotificationLogRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationCenterService {

    private final NotificationLogRepository notificationLogRepository;
    private final InboxMessageRepository inboxMessageRepository;
    private final AuditLogWriteRepository auditLogWriteRepository;
    private final IdentityResolutionRepository identityResolutionRepository;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final boolean smtpEnabled;

    public NotificationCenterService(
            NotificationLogRepository notificationLogRepository,
            InboxMessageRepository inboxMessageRepository,
            AuditLogWriteRepository auditLogWriteRepository,
            IdentityResolutionRepository identityResolutionRepository,
            ObjectMapper objectMapper,
            Clock clock,
            @Value("${uniondesk.notification.smtp-enabled:false}") boolean smtpEnabled) {
        this.notificationLogRepository = notificationLogRepository;
        this.inboxMessageRepository = inboxMessageRepository;
        this.auditLogWriteRepository = auditLogWriteRepository;
        this.identityResolutionRepository = identityResolutionRepository;
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.smtpEnabled = smtpEnabled;
    }

    @Transactional
    public NotificationDispatchResult notifyTicketCreated(long businessDomainId, long ticketId, long recipientUserId, long operatorUserId) {
        return sendTicketNotification(
                businessDomainId,
                ticketId,
                recipientUserId,
                operatorUserId,
                "ticket.created",
                "工单已创建",
                "您的工单已提交，客服会尽快处理。",
                "/api/v1/domains/%d/tickets/my/%d".formatted(businessDomainId, ticketId));
    }

    @Transactional
    public NotificationDispatchResult notifyTicketStatusChanged(
            long businessDomainId,
            long ticketId,
            long recipientUserId,
            long operatorUserId,
            String status) {
        return sendTicketNotification(
                businessDomainId,
                ticketId,
                recipientUserId,
                operatorUserId,
                "ticket.status.changed",
                "工单状态更新",
                "您的工单状态已变更为 %s。".formatted(status),
                "/api/v1/domains/%d/tickets/my/%d".formatted(businessDomainId, ticketId));
    }

    @Transactional
    public NotificationDispatchResult notifyTicketReply(
            long businessDomainId,
            long ticketId,
            long recipientUserId,
            long operatorUserId,
            String replyType) {
        return sendTicketNotification(
                businessDomainId,
                ticketId,
                recipientUserId,
                operatorUserId,
                "ticket.reply",
                "工单新回复",
                "您的工单收到一条%s回复。".formatted(replyType == null ? "" : replyType),
                "/api/v1/domains/%d/tickets/my/%d".formatted(businessDomainId, ticketId));
    }

    @Transactional
    public NotificationDispatchResult notifyTicketMerged(
            long businessDomainId,
            long sourceTicketId,
            long recipientUserId,
            long operatorUserId,
            long targetTicketId) {
        return sendTicketNotification(
                businessDomainId,
                sourceTicketId,
                recipientUserId,
                operatorUserId,
                "ticket.merged",
                "工单已合并",
                "您的工单已合并到 #%d。".formatted(targetTicketId),
                "/api/v1/domains/%d/tickets/my/%d".formatted(businessDomainId, sourceTicketId));
    }

    @Transactional
    public long unreadCount(long recipientUserId) {
        return inboxMessageRepository.countUnread(identityResolutionRepository.ensureIdentitySubject(recipientUserId));
    }

    @Transactional
    public List<InboxMessageView> listInboxMessages(long recipientUserId, boolean unreadOnly, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 500));
        long recipientSubjectId = identityResolutionRepository.ensureIdentitySubject(recipientUserId);
        return inboxMessageRepository.listByRecipient(recipientSubjectId, unreadOnly, safeLimit).stream()
                .map(this::toView)
                .toList();
    }

    @Transactional
    public int markRead(long recipientUserId, long inboxMessageId) {
        return inboxMessageRepository.markRead(
                identityResolutionRepository.ensureIdentitySubject(recipientUserId),
                inboxMessageId);
    }

    @Transactional
    public int markReadBatch(long recipientUserId, List<Long> messageIds) {
        return inboxMessageRepository.markReadBatch(
                identityResolutionRepository.ensureIdentitySubject(recipientUserId),
                messageIds);
    }

    private NotificationDispatchResult sendTicketNotification(
            long businessDomainId,
            long sourceId,
            long recipientUserId,
            long operatorUserId,
            String templateCode,
            String title,
            String content,
            String jumpUrl) {
        long recipientSubjectId = identityResolutionRepository.ensureIdentitySubject(recipientUserId);
        long operatorSubjectId = identityResolutionRepository.ensureIdentitySubject(operatorUserId);
        LocalDateTime now = LocalDateTime.now(clock);
        String payloadJson = serializeJson(Map.of(
                "business_domain_id", businessDomainId,
                "source_id", sourceId,
                "recipient_user_id", recipientUserId,
                "operator_user_id", operatorUserId,
                "template_code", templateCode,
                "title", title,
                "content", content,
                "jump_url", jumpUrl));
        String status = smtpEnabled ? "sent" : "degraded";
        String lastError = smtpEnabled ? null : "smtp disabled";

        NotificationLogPo logPo = new NotificationLogPo();
        logPo.setBusinessDomainId(businessDomainId);
        logPo.setSourceType("ticket");
        logPo.setSourceId(sourceId);
        logPo.setChannel("email");
        logPo.setRecipientSubjectId(recipientSubjectId);
        logPo.setPortalType("customer");
        logPo.setTemplateCode(templateCode);
        logPo.setPayloadJson(payloadJson);
        logPo.setStatus(status);
        logPo.setRetryCount(0);
        logPo.setLastError(lastError);
        logPo.setNextRetryAt(smtpEnabled ? null : now.plusMinutes(10));
        logPo.setSentAt(smtpEnabled ? now : null);
        notificationLogRepository.save(logPo);

        long notificationLogId = logPo.getId() == 0
                ? notificationLogRepository.findLatestId(recipientSubjectId, sourceId, templateCode)
                : logPo.getId();

        InboxMessagePo inboxPo = new InboxMessagePo();
        inboxPo.setNotificationLogId(notificationLogId);
        inboxPo.setRecipientSubjectId(recipientSubjectId);
        inboxPo.setPortalType("customer");
        inboxPo.setBusinessDomainId(businessDomainId);
        inboxPo.setTitle(title);
        inboxPo.setContent(content);
        inboxPo.setJumpUrl(jumpUrl);
        inboxMessageRepository.save(inboxPo);

        long inboxMessageId = inboxPo.getId() == 0
                ? inboxMessageRepository.findLatestId(recipientSubjectId, notificationLogId)
                : inboxPo.getId();

        recordAudit(
                businessDomainId,
                operatorSubjectId,
                "staff",
                "notification:" + sourceId,
                "notification.send",
                Map.of("template_code", templateCode, "recipient_user_id", recipientUserId, "status", status),
                status);
        return new NotificationDispatchResult(notificationLogId, inboxMessageId, status);
    }

    private void recordAudit(
            long businessDomainId,
            long operatorSubjectId,
            String actorType,
            String target,
            String action,
            Map<String, Object> detail,
            String result) {
        AuditLogWritePo po = new AuditLogWritePo();
        po.setBusinessDomainId(businessDomainId);
        po.setOperatorSubjectId(operatorSubjectId);
        po.setOperatorActorType(actorType);
        po.setTarget(target);
        po.setAction(action);
        po.setDetail(serializeJson(detail));
        po.setResult(result);
        auditLogWriteRepository.save(po);
    }

    private InboxMessageView toView(InboxMessagePo po) {
        return new InboxMessageView(
                po.getId(),
                po.getNotificationLogId(),
                po.getRecipientSubjectId(),
                po.getPortalType(),
                po.getBusinessDomainId(),
                po.getTitle(),
                po.getContent(),
                po.getJumpUrl(),
                po.isRead(),
                po.getReadAt(),
                po.getCreatedAt(),
                po.getUpdatedAt());
    }

    private String serializeJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("invalid json payload", ex);
        }
    }

    public record NotificationDispatchResult(long notificationLogId, long inboxMessageId, String status) {
    }

    public record InboxMessageView(
            long id,
            Long notificationLogId,
            long recipientSubjectId,
            String portalType,
            Long businessDomainId,
            String title,
            String content,
            String jumpUrl,
            boolean read,
            LocalDateTime readAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
    }
}
