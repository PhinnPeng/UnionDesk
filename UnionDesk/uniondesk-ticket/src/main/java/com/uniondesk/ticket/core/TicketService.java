package com.uniondesk.ticket.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uniondesk.auth.core.UserContext;
import com.uniondesk.attachment.core.AttachmentService;
import com.uniondesk.notification.core.NotificationCenterService;
import com.uniondesk.sla.core.SlaService;
import com.uniondesk.common.event.TicketStatusChangedEvent;
import com.uniondesk.common.event.UnionDeskEventPublisher;
import com.uniondesk.ticket.entity.AuditLogPo;
import com.uniondesk.ticket.entity.IdentitySubjectPo;
import com.uniondesk.ticket.entity.QuickReplyTemplatePo;
import com.uniondesk.ticket.entity.TicketDetailPo;
import com.uniondesk.ticket.entity.TicketHistoryPo;
import com.uniondesk.ticket.entity.TicketPo;
import com.uniondesk.ticket.entity.TicketRelationPo;
import com.uniondesk.ticket.entity.TicketReplyPo;
import com.uniondesk.ticket.entity.TicketTemplatePo;
import com.uniondesk.ticket.entity.UserAccountPo;
import com.uniondesk.ticket.repository.AuditLogRepository;
import com.uniondesk.ticket.repository.CustomerAccountRepository;
import com.uniondesk.ticket.repository.IdentitySubjectRepository;
import com.uniondesk.ticket.repository.QuickReplyTemplateRepository;
import com.uniondesk.ticket.repository.StaffAccountRepository;
import com.uniondesk.ticket.repository.TicketHistoryRepository;
import com.uniondesk.ticket.repository.TicketRelationRepository;
import com.uniondesk.ticket.repository.TicketReplyRepository;
import com.uniondesk.ticket.repository.TicketRepository;
import com.uniondesk.ticket.repository.TicketTemplateRepository;
import com.uniondesk.ticket.repository.UserAccountRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class TicketService {

    private static final DateTimeFormatter TICKET_NO_DATE_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;

    private final TicketRepository ticketRepository;
    private final TicketReplyRepository ticketReplyRepository;
    private final TicketHistoryRepository ticketHistoryRepository;
    private final TicketRelationRepository ticketRelationRepository;
    private final TicketTemplateRepository ticketTemplateRepository;
    private final QuickReplyTemplateRepository quickReplyTemplateRepository;
    private final AuditLogRepository auditLogRepository;
    private final IdentitySubjectRepository identitySubjectRepository;
    private final UserAccountRepository userAccountRepository;
    private final CustomerAccountRepository customerAccountRepository;
    private final StaffAccountRepository staffAccountRepository;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final NotificationCenterService notificationCenterService;
    private final SlaService slaService;
    private final AttachmentService attachmentService;
    private final UnionDeskEventPublisher eventPublisher;

    public TicketService(
            TicketRepository ticketRepository,
            TicketReplyRepository ticketReplyRepository,
            TicketHistoryRepository ticketHistoryRepository,
            TicketRelationRepository ticketRelationRepository,
            TicketTemplateRepository ticketTemplateRepository,
            QuickReplyTemplateRepository quickReplyTemplateRepository,
            AuditLogRepository auditLogRepository,
            IdentitySubjectRepository identitySubjectRepository,
            UserAccountRepository userAccountRepository,
            CustomerAccountRepository customerAccountRepository,
            StaffAccountRepository staffAccountRepository,
            ObjectMapper objectMapper,
            Clock clock,
            NotificationCenterService notificationCenterService,
            SlaService slaService,
            AttachmentService attachmentService,
            UnionDeskEventPublisher eventPublisher) {
        this.ticketRepository = ticketRepository;
        this.ticketReplyRepository = ticketReplyRepository;
        this.ticketHistoryRepository = ticketHistoryRepository;
        this.ticketRelationRepository = ticketRelationRepository;
        this.ticketTemplateRepository = ticketTemplateRepository;
        this.quickReplyTemplateRepository = quickReplyTemplateRepository;
        this.auditLogRepository = auditLogRepository;
        this.identitySubjectRepository = identitySubjectRepository;
        this.userAccountRepository = userAccountRepository;
        this.customerAccountRepository = customerAccountRepository;
        this.staffAccountRepository = staffAccountRepository;
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.notificationCenterService = notificationCenterService;
        this.slaService = slaService;
        this.attachmentService = attachmentService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public TicketSubmissionResult createCustomerTicket(UserContext context, long businessDomainId, CreateTicketCommand command) {
        DomainRow domain = loadDomain(businessDomainId);
        String ticketNo = nextTicketNo(domain.id(), domain.code());
        String priority = resolvePriority(businessDomainId, command.priority());
        String source = StringUtils.hasText(command.source()) ? command.source().trim() : "web";
        String customFieldsJson = serializeJson(command.dynamicData());
        TicketTemplateRow template = command.templateId() == null ? null : loadTicketTemplate(businessDomainId, command.templateId());
        TicketContent content = mergeTemplate(command, template, priority, customFieldsJson);

        TicketPo ticketPo = new TicketPo();
        ticketPo.setTicketNo(ticketNo);
        ticketPo.setBusinessDomainId(businessDomainId);
        ticketPo.setCustomerId(context.userId());
        ticketPo.setTicketTypeId(content.ticketTypeId());
        ticketPo.setTitle(content.title());
        ticketPo.setDescription(content.description());
        ticketPo.setPriority(content.priority());
        ticketPo.setSource(source);
        ticketPo.setCustomFields(content.customFieldsJson());
        ticketPo.setSlaFirstResponseDeadline(content.slaFirstResponseDeadline());
        ticketPo.setSlaResolutionDeadline(content.slaResolutionDeadline());
        ticketRepository.save(ticketPo);

        long ticketId = ticketPo.getId() == 0 ? ticketRepository.findIdByTicketNo(ticketNo) : ticketPo.getId();
        recordHistory(ticketId, businessDomainId, "create", null, "open", context, Map.of(
                "ticket_no", ticketNo,
                "priority", content.priority(),
                "source", source));

        if (!content.attachmentIds().isEmpty()) {
            attachmentService.linkAttachmentsToTicket(ticketId, content.attachmentIds(), "public");
        }

        slaService.applyOnCreate(businessDomainId, ticketId, content.ticketTypeId());
        notificationCenterService.notifyTicketCreated(businessDomainId, ticketId, context.userId(), context.userId());

        return new TicketSubmissionResult(ticketId, ticketNo);
    }

    @Transactional
    public TicketActionResult changeTicketStatus(UserContext context, long businessDomainId, long ticketId, ChangeTicketStatusCommand command) {
        TicketRow current = loadTicketRow(businessDomainId, ticketId);
        if (current.version() != command.version()) {
            throw new IllegalArgumentException("工单已被他人修改，请刷新");
        }
        if (!isAllowedStatusTransition(current.status(), command.status())) {
            throw new IllegalArgumentException("状态流转不合法");
        }

        LocalDateTime now = LocalDateTime.now(clock);
        int updated = ticketRepository.updateStatus(ticketId, command.status(), command.version(), now);
        if (updated == 0) {
            throw new IllegalArgumentException("工单已被他人修改，请刷新");
        }

        recordHistory(ticketId, businessDomainId, "status_change", current.status(), command.status(), context, Map.of(
                "version", command.version(),
                "new_status", command.status()));
        if (List.of("resolved", "closed").contains(command.status())) {
            slaService.recordResolution(businessDomainId, ticketId);
            eventPublisher.publish(new TicketStatusChangedEvent(
                    businessDomainId,
                    ticketId,
                    current.customerId(),
                    context.userId(),
                    command.status()));
        } else if ("processing".equalsIgnoreCase(command.status())) {
            slaService.recordFirstResponse(businessDomainId, ticketId);
        }
        refreshTicketSla(businessDomainId, ticketId);
        return new TicketActionResult(ticketId);
    }

    @Transactional
    public TicketActionResult claimTicket(UserContext context, long businessDomainId, long ticketId, ClaimTicketCommand command) {
        requireStaff(context);
        TicketRow current = loadTicketRow(businessDomainId, ticketId);
        if (current.version() != command.version()) {
            throw new IllegalArgumentException("工单已被他人修改，请刷新");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        int updated = ticketRepository.updateClaim(ticketId, businessDomainId, context.userId(), command.version(), now);
        if (updated == 0) {
            throw new IllegalArgumentException("工单已被他人修改，请刷新");
        }
        recordHistory(ticketId, businessDomainId, "claim", current.assignedTo() == null ? null : String.valueOf(current.assignedTo()),
                String.valueOf(context.userId()), context, Map.of("version", command.version()));
        recordAudit(businessDomainId, context, "ticket:" + current.ticketNo(), "ticket.claim",
                Map.of("ticket_id", ticketId, "assignee_staff_account_id", context.userId()), "success");
        slaService.recordFirstResponse(businessDomainId, ticketId);
        notificationCenterService.notifyTicketStatusChanged(businessDomainId, ticketId, current.customerId(), context.userId(), "processing");
        refreshTicketSla(businessDomainId, ticketId);
        return new TicketActionResult(ticketId);
    }

    @Transactional
    public TicketActionResult assignTicket(UserContext context, long businessDomainId, long ticketId, AssignTicketCommand command) {
        requireStaff(context);
        TicketRow current = loadTicketRow(businessDomainId, ticketId);
        if (current.version() != command.version()) {
            throw new IllegalArgumentException("工单已被他人修改，请刷新");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        int updated = ticketRepository.updateAssign(ticketId, businessDomainId, command.assigneeStaffAccountId(), command.version(), now);
        if (updated == 0) {
            throw new IllegalArgumentException("工单已被他人修改，请刷新");
        }
        recordHistory(ticketId, businessDomainId, "assign", current.assignedTo() == null ? null : String.valueOf(current.assignedTo()),
                String.valueOf(command.assigneeStaffAccountId()), context, Map.of("version", command.version()));
        recordAudit(businessDomainId, context, "ticket:" + current.ticketNo(), "ticket.assign",
                Map.of("ticket_id", ticketId, "assignee_staff_account_id", command.assigneeStaffAccountId()), "success");
        notificationCenterService.notifyTicketStatusChanged(businessDomainId, ticketId, current.customerId(), context.userId(), "processing");
        refreshTicketSla(businessDomainId, ticketId);
        return new TicketActionResult(ticketId);
    }

    @Transactional
    public TicketActionResult replyTicket(UserContext context, long businessDomainId, long ticketId, ReplyTicketCommand command) {
        TicketRow current = loadTicketRow(businessDomainId, ticketId);
        if (current.version() != command.version()) {
            throw new IllegalArgumentException("工单已被他人修改，请刷新");
        }
        if (List.of("merged", "withdrawn").contains(normalize(current.status()))) {
            throw new IllegalArgumentException("工单已终止，关闭后不允许回复");
        }
        String replyContent = resolveReplyContent(command.quickReplyTemplateId(), command.content(), businessDomainId);
        String senderType = resolveSenderType(context);
        Long staffAccountId = isStaffRole(context) ? context.userId() : null;
        Long customerAccountId = isCustomerRole(context) ? context.userId() : null;
        String attachmentUrlsJson = command.attachmentIds().isEmpty() ? null : serializeJson(command.attachmentIds());
        LocalDateTime now = LocalDateTime.now(clock);
        TicketReplyPo replyPo = new TicketReplyPo();
        replyPo.setTicketId(ticketId);
        replyPo.setBusinessDomainId(businessDomainId);
        replyPo.setSenderUserId(context.userId());
        replyPo.setSenderRole(context.role());
        replyPo.setSenderType(senderType);
        replyPo.setStaffAccountId(staffAccountId);
        replyPo.setCustomerAccountId(customerAccountId);
        replyPo.setReplyType(command.quickReplyTemplateId() == null ? "text" : "quick");
        replyPo.setContent(replyContent);
        replyPo.setAttachmentUrls(attachmentUrlsJson);
        replyPo.setCreatedAt(now);
        ticketReplyRepository.save(replyPo);
        long replyId = replyPo.getId() == 0 ? ticketReplyRepository.findLatestIdByTicketId(ticketId) : replyPo.getId();
        if (!command.attachmentIds().isEmpty()) {
            attachmentService.linkAttachments("ticket_reply", replyId, command.attachmentIds(), "reply");
        }
        int updated = ticketRepository.updateOnReply(ticketId, businessDomainId, senderType, command.version(), now);
        if (updated == 0) {
            throw new IllegalArgumentException("工单已被他人修改，请刷新");
        }
        recordHistory(ticketId, businessDomainId, "reply", current.status(), current.status(), context, Map.of(
                "version", command.version(),
                "reply_id", replyId,
                "reply_type", command.quickReplyTemplateId() == null ? "text" : "quick",
                "sender_type", senderType));
        recordAudit(businessDomainId, context, "ticket:" + current.ticketNo(), "ticket.reply",
                Map.of("ticket_id", ticketId, "reply_id", replyId, "reply_type", command.quickReplyTemplateId() == null ? "text" : "quick"), "success");
        if (isStaffRole(context)) {
            slaService.recordFirstResponse(businessDomainId, ticketId);
            notificationCenterService.notifyTicketReply(businessDomainId, ticketId, current.customerId(), context.userId(), "staff");
        } else if (current.assignedTo() != null) {
            notificationCenterService.notifyTicketReply(businessDomainId, ticketId, current.assignedTo(), context.userId(), "customer");
        }
        refreshTicketSla(businessDomainId, ticketId);
        return new TicketActionResult(replyId);
    }

    @Transactional
    public TicketActionResult withdrawCustomerTicket(UserContext context, long businessDomainId, long ticketId, WithdrawTicketCommand command) {
        requireCustomer(context);
        TicketRow current = loadTicketRow(businessDomainId, ticketId);
        if (current.customerId() != context.userId()) {
            throw new IllegalArgumentException("only ticket owner can withdraw");
        }
        if (current.version() != command.version()) {
            throw new IllegalArgumentException("工单已被他人修改，请刷新");
        }
        if (!List.of("open", "new", "processing").contains(normalize(current.status()))) {
            throw new IllegalArgumentException("当前状态不允许撤回");
        }
        int updated = ticketRepository.updateWithdraw(
                ticketId,
                businessDomainId,
                command.version(),
                StringUtils.hasText(command.reason()) ? command.reason().trim() : null);
        if (updated == 0) {
            throw new IllegalArgumentException("工单已被他人修改，请刷新");
        }
        recordHistory(ticketId, businessDomainId, "withdraw", current.status(), "withdrawn", context, Map.of(
                "version", command.version(),
                "reason", command.reason()));
        recordAudit(businessDomainId, context, "ticket:" + current.ticketNo(), "ticket.withdraw",
                Map.of("ticket_id", ticketId, "reason", command.reason()), "success");
        if (current.assignedTo() != null) {
            notificationCenterService.notifyTicketStatusChanged(businessDomainId, ticketId, current.assignedTo(), context.userId(), "withdrawn");
        }
        return new TicketActionResult(ticketId);
    }

    @Transactional
    public TicketActionResult mergeTicket(UserContext context, long businessDomainId, long sourceTicketId, MergeTicketCommand command) {
        requireStaff(context);
        TicketRow source = loadTicketRow(businessDomainId, sourceTicketId);
        TicketRow target = loadTicketRow(businessDomainId, command.targetTicketId());
        if (source.version() != command.version()) {
            throw new IllegalArgumentException("工单已被他人修改，请刷新");
        }
        if (normalize(source.status()).equals("merged")) {
            throw new IllegalArgumentException("工单已合并");
        }
        int updated = ticketRepository.updateMerge(sourceTicketId, businessDomainId, command.version());
        if (updated == 0) {
            throw new IllegalArgumentException("工单已被他人修改，请刷新");
        }
        TicketRelationPo relationPo = new TicketRelationPo();
        relationPo.setSourceTicketId(sourceTicketId);
        relationPo.setTargetTicketId(command.targetTicketId());
        relationPo.setRelationType("merge");
        relationPo.setCreatedByStaffAccountId(context.userId());
        relationPo.setNote(StringUtils.hasText(command.note()) ? command.note().trim() : null);
        ticketRelationRepository.save(relationPo);
        recordHistory(sourceTicketId, businessDomainId, "merge", source.status(), "merged", context, Map.of(
                "version", command.version(),
                "target_ticket_id", command.targetTicketId(),
                "target_ticket_no", target.ticketNo(),
                "note", command.note()));
        recordAudit(businessDomainId, context, "ticket:" + source.ticketNo(), "ticket.merge",
                Map.of("source_ticket_id", sourceTicketId, "target_ticket_id", command.targetTicketId()), "success");
        notificationCenterService.notifyTicketMerged(businessDomainId, sourceTicketId, source.customerId(), context.userId(), command.targetTicketId());
        return new TicketActionResult(sourceTicketId);
    }

    @Transactional(readOnly = true)
    public TicketDetailResult getTicketDetail(long businessDomainId, long ticketId) {
        TicketRow ticket = loadTicketRow(businessDomainId, ticketId);
        return new TicketDetailResult(ticket, listTicketReplies(businessDomainId, ticketId), listTicketHistory(businessDomainId, ticketId));
    }

    @Transactional(readOnly = true)
    public List<TicketRow> listTickets(long businessDomainId, String status, int limit) {
        return listTicketsInternal(businessDomainId, null, status, limit);
    }

    @Transactional(readOnly = true)
    public List<TicketRow> listCustomerTickets(long businessDomainId, long customerUserId, String status, int limit) {
        return listTicketsInternal(businessDomainId, customerUserId, status, limit);
    }

    @Transactional(readOnly = true)
    public List<TicketReplyRow> listTicketReplies(long businessDomainId, long ticketId) {
        return ticketReplyRepository.findByTicketIdAndDomainId(ticketId, businessDomainId).stream()
                .map(this::toTicketReplyRow)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TicketHistoryRow> listTicketHistory(long businessDomainId, long ticketId) {
        return ticketHistoryRepository.findByTicketIdAndDomainId(ticketId, businessDomainId).stream()
                .map(this::toTicketHistoryRow)
                .toList();
    }

    @Transactional(readOnly = true)
    public SlaService.SlaBreachDecision evaluateTicketSla(long businessDomainId, long ticketId) {
        return slaService.evaluateTicket(businessDomainId, ticketId);
    }

    private void refreshTicketSla(long businessDomainId, long ticketId) {
        slaService.evaluateTicket(businessDomainId, ticketId);
    }

    private List<TicketRow> listTicketsInternal(long businessDomainId, Long customerUserId, String status, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 500));
        String normalizedStatus = StringUtils.hasText(status) ? status.trim() : null;
        return ticketRepository.listTickets(businessDomainId, customerUserId, normalizedStatus, safeLimit).stream()
                .map(this::toTicketRow)
                .toList();
    }

    private String resolveReplyContent(Long quickReplyTemplateId, String content, long businessDomainId) {
        String normalizedContent = StringUtils.hasText(content) ? content.trim() : "";
        if (quickReplyTemplateId == null) {
            return normalizedContent;
        }
        QuickReplyTemplatePo template = quickReplyTemplateRepository.findActiveRequiredByIdAndDomainId(quickReplyTemplateId, businessDomainId);
        return resolveReplyContentFromTemplate(template, content);
    }

    private String resolveReplyContentFromTemplate(QuickReplyTemplatePo template, String content) {
        String normalizedContent = StringUtils.hasText(content) ? content.trim() : "";
        String templateContent = StringUtils.hasText(template.getContent()) ? template.getContent().trim() : "";
        if (!StringUtils.hasText(normalizedContent)) {
            return templateContent;
        }
        if (!StringUtils.hasText(templateContent)) {
            return normalizedContent;
        }
        return normalizedContent + "\n" + templateContent;
    }

    private void recordAudit(
            long businessDomainId,
            UserContext context,
            String target,
            String action,
            Map<String, Object> detail,
            String result) {
        AuditLogPo auditPo = new AuditLogPo();
        auditPo.setBusinessDomainId(businessDomainId);
        auditPo.setOperatorSubjectId(context == null ? null : ensureIdentitySubject(context.userId()));
        auditPo.setOperatorActorType(context == null ? null : resolveActorType(context.role()));
        auditPo.setTarget(target);
        auditPo.setAction(action);
        auditPo.setDetail(serializeJson(detail));
        auditPo.setResult(result);
        auditPo.setRequestId(context == null ? null : context.sessionId());
        auditLogRepository.save(auditPo);
    }

    private String resolveActorType(String role) {
        String normalized = normalize(role);
        if ("customer".equals(normalized)) {
            return "customer";
        }
        if ("agent".equals(normalized) || "domain_admin".equals(normalized) || "super_admin".equals(normalized)) {
            return "staff";
        }
        return normalized;
    }

    private String resolveSenderType(UserContext context) {
        if (context == null) {
            return "system";
        }
        return isCustomerRole(context) ? "customer" : "staff";
    }

    private boolean isStaffRole(UserContext context) {
        return context != null && List.of("agent", "domain_admin", "super_admin").contains(normalize(context.role()));
    }

    private boolean isCustomerRole(UserContext context) {
        return context != null && "customer".equals(normalize(context.role()));
    }

    private void requireStaff(UserContext context) {
        if (!isStaffRole(context)) {
            throw new IllegalArgumentException("only staff can perform this action");
        }
    }

    private void requireCustomer(UserContext context) {
        if (!isCustomerRole(context)) {
            throw new IllegalArgumentException("only customer can perform this action");
        }
    }

    private DomainRow loadDomain(long businessDomainId) {
        String code = ticketRepository.findDomainCodeById(businessDomainId);
        if (code == null) {
            throw new IllegalArgumentException("business domain not found");
        }
        return new DomainRow(businessDomainId, code, code);
    }

    private TicketTemplateRow loadTicketTemplate(long businessDomainId, long templateId) {
        TicketTemplatePo po = ticketTemplateRepository.findByIdAndDomainId(templateId, businessDomainId);
        if (po == null) {
            throw new IllegalArgumentException("ticket template not found");
        }
        return toTicketTemplateRow(po);
    }

    private TicketRow loadTicketRow(long businessDomainId, long ticketId) {
        return toTicketRow(ticketRepository.findRequiredByIdAndDomainId(ticketId, businessDomainId));
    }

    private String nextTicketNo(long businessDomainId, String domainCode) {
        String day = LocalDate.now(clock).format(TICKET_NO_DATE_FORMAT);
        long sequence = ticketRepository.findNextTicketSequence(businessDomainId, domainCode + "-" + day + "-%");
        return domainCode + "-" + day + "-" + sequence;
    }

    private String resolvePriority(long businessDomainId, String requestedPriority) {
        if (StringUtils.hasText(requestedPriority)) {
            return requestedPriority.trim();
        }
        String defaultPriority = ticketRepository.findDefaultPriorityCode(businessDomainId);
        return StringUtils.hasText(defaultPriority) ? defaultPriority : "normal";
    }

    private TicketContent mergeTemplate(CreateTicketCommand command, TicketTemplateRow template, String priority, String customFieldsJson) {
        long ticketTypeId = command.ticketTypeId() == null ? 0L : command.ticketTypeId();
        String title = command.title();
        String description = command.description();
        String mergedCustomFields = customFieldsJson;
        if (template == null || !StringUtils.hasText(template.contentJson())) {
            return new TicketContent(ticketTypeId, title, description, priority, mergedCustomFields, List.copyOf(command.attachmentIds()), null, null);
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> templateData = objectMapper.readValue(template.contentJson(), Map.class);
            if (!StringUtils.hasText(title) && templateData.containsKey("title")) {
                title = String.valueOf(templateData.get("title"));
            }
            if (!StringUtils.hasText(description) && templateData.containsKey("description")) {
                description = String.valueOf(templateData.get("description"));
            }
            if (templateData.containsKey("priority") && !StringUtils.hasText(priority)) {
                priority = String.valueOf(templateData.get("priority"));
            }
            if (templateData.containsKey("ticket_type_id")) {
                Object value = templateData.get("ticket_type_id");
                if (value instanceof Number number) {
                    ticketTypeId = number.longValue();
                }
            }
            if (templateData.containsKey("dynamic_data")) {
                mergedCustomFields = serializeJson(templateData.get("dynamic_data"));
            }
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("invalid ticket template");
        }
        return new TicketContent(ticketTypeId, title, description, priority, mergedCustomFields, List.copyOf(command.attachmentIds()), null, null);
    }

    private boolean isAllowedStatusTransition(String currentStatus, String nextStatus) {
        String normalizedCurrent = normalize(currentStatus);
        String normalizedNext = normalize(nextStatus);
        return switch (normalizedCurrent) {
            case "open", "new" -> List.of("processing", "resolved", "withdrawn", "merged").contains(normalizedNext);
            case "processing" -> List.of("resolved", "closed", "merged").contains(normalizedNext);
            case "resolved" -> List.of("closed", "merged").contains(normalizedNext);
            case "closed", "withdrawn", "merged" -> false;
            default -> false;
        };
    }

    private void recordHistory(long ticketId, long businessDomainId, String action, String fromValue, String toValue, UserContext context, Map<String, Object> payload) {
        TicketHistoryPo historyPo = new TicketHistoryPo();
        historyPo.setTicketId(ticketId);
        historyPo.setBusinessDomainId(businessDomainId);
        historyPo.setAction(action);
        historyPo.setFromValue(fromValue);
        historyPo.setToValue(toValue);
        historyPo.setOperatorSubjectId(context == null ? null : ensureIdentitySubject(context.userId()));
        historyPo.setOperatorActorType(context == null ? null : resolveActorType(context.role()));
        historyPo.setPayload(serializeJson(payload));
        ticketHistoryRepository.save(historyPo);
    }

    private long ensureIdentitySubject(long userId) {
        Long subjectId = customerAccountRepository.findSubjectIdById(userId);
        if (subjectId != null) {
            return subjectId;
        }
        subjectId = staffAccountRepository.findSubjectIdById(userId);
        if (subjectId != null) {
            return subjectId;
        }
        if (identitySubjectRepository.findById(userId) != null) {
            return userId;
        }
        UserAccountPo user = userAccountRepository.findById(userId);
        String phone = user != null && StringUtils.hasText(user.getMobile())
                ? user.getMobile()
                : "user-" + userId;
        IdentitySubjectPo subjectPo = new IdentitySubjectPo();
        subjectPo.setId(userId);
        subjectPo.setSubjectType("person");
        subjectPo.setPhone(phone);
        subjectPo.setStatus("active");
        try {
            identitySubjectRepository.insert(subjectPo);
            return userId;
        } catch (DuplicateKeyException ignored) {
            Long existingSubjectId = identitySubjectRepository.findIdByPhone(phone);
            if (existingSubjectId != null) {
                return existingSubjectId;
            }
            throw ignored;
        }
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

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    private TicketRow toTicketRow(TicketDetailPo po) {
        return new TicketRow(
                po.getId(),
                po.getTicketNo(),
                po.getBusinessDomainId(),
                po.getBusinessDomainCode(),
                po.getBusinessDomainName(),
                po.getTicketTypeId(),
                po.getTicketTypeName(),
                po.getCustomerId(),
                po.getAssignedTo(),
                po.getTitle(),
                po.getDescription(),
                po.getStatus(),
                po.getPriority(),
                po.getSource(),
                po.getResult(),
                po.getVersion(),
                po.getCustomFieldsJson(),
                po.getSlaFirstResponseDeadline(),
                po.getSlaResolutionDeadline(),
                po.getSlaFirstRespondedAt(),
                po.getSlaResolvedAt(),
                po.getSlaStatus(),
                po.getSlaPausedDuration(),
                po.getSlaPauseStartedAt(),
                po.getCreatedAt(),
                po.getUpdatedAt(),
                po.getLastReplyAt(),
                po.getReplyCount());
    }

    private TicketReplyRow toTicketReplyRow(TicketReplyPo po) {
        return new TicketReplyRow(
                po.getId(),
                po.getSenderType(),
                po.getSenderRole(),
                po.getStaffAccountId(),
                po.getCustomerAccountId(),
                po.getReplyType(),
                po.getContent(),
                po.getCreatedAt());
    }

    private TicketHistoryRow toTicketHistoryRow(TicketHistoryPo po) {
        return new TicketHistoryRow(
                po.getId(),
                po.getAction(),
                po.getFromValue(),
                po.getToValue(),
                po.getOperatorSubjectId(),
                po.getOperatorActorType(),
                po.getPayload(),
                po.getCreatedAt());
    }

    private QuickReplyTemplateRow toQuickReplyTemplateRow(QuickReplyTemplatePo po) {
        return new QuickReplyTemplateRow(
                po.getId(),
                po.getBusinessDomainId(),
                po.getScopeType(),
                po.getTitle(),
                po.getContent(),
                po.getCategory(),
                po.getStatus(),
                po.getSortOrder());
    }

    private TicketTemplateRow toTicketTemplateRow(TicketTemplatePo po) {
        return new TicketTemplateRow(
                po.getId(),
                po.getBusinessDomainId(),
                po.getTicketTypeId(),
                po.getScope(),
                po.getName(),
                po.getContentJson(),
                po.getStatus(),
                po.getSortOrder());
    }

    private record DomainRow(long id, String code, String name) {
    }

    private record TicketTemplateRow(long id, long businessDomainId, long ticketTypeId, String scope, String name, String contentJson, String status, int sortOrder) {
    }

    public record CreateTicketCommand(
            Long ticketTypeId,
            String title,
            String description,
            Map<String, Object> dynamicData,
            List<Long> attachmentIds,
            Long templateId,
            String priority,
            String source) {

        public CreateTicketCommand {
            attachmentIds = attachmentIds == null ? List.of() : List.copyOf(attachmentIds);
            dynamicData = dynamicData == null ? Map.of() : Map.copyOf(dynamicData);
        }
    }

    public record ChangeTicketStatusCommand(
            String status,
            long version,
            Long quickReplyTemplateId,
            String content) {
    }

    public record TicketSubmissionResult(long id, String ticketNo) {
    }

    public record TicketActionResult(long id) {
    }

    public record TicketDetailResult(
            TicketRow ticket,
            List<TicketReplyRow> replies,
            List<TicketHistoryRow> history) {
    }

    public record ClaimTicketCommand(long version) {
    }

    public record AssignTicketCommand(long version, long assigneeStaffAccountId) {
    }

    public record ReplyTicketCommand(long version, String content, Long quickReplyTemplateId, List<Long> attachmentIds) {

        public ReplyTicketCommand {
            attachmentIds = attachmentIds == null ? List.of() : List.copyOf(attachmentIds);
        }
    }

    public record WithdrawTicketCommand(long version, String reason) {
    }

    public record MergeTicketCommand(long version, long targetTicketId, String note) {
    }

    record TicketContent(
            long ticketTypeId,
            String title,
            String description,
            String priority,
            String customFieldsJson,
            List<Long> attachmentIds,
            LocalDateTime slaFirstResponseDeadline,
            LocalDateTime slaResolutionDeadline) {

        public TicketContent {
            attachmentIds = attachmentIds == null ? List.of() : List.copyOf(attachmentIds);
        }
    }

    public record TicketRow(
            long id,
            String ticketNo,
            long businessDomainId,
            String businessDomainCode,
            String businessDomainName,
            long ticketTypeId,
            String ticketTypeName,
            long customerId,
            Long assignedTo,
            String title,
            String description,
            String status,
            String priority,
            String source,
            String result,
            int version,
            String customFieldsJson,
            LocalDateTime slaFirstResponseDeadline,
            LocalDateTime slaResolutionDeadline,
            LocalDateTime slaFirstRespondedAt,
            LocalDateTime slaResolvedAt,
            String slaStatus,
            int slaPausedDuration,
            LocalDateTime slaPauseStartedAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            LocalDateTime lastReplyAt,
            long replyCount) {
    }

    record QuickReplyTemplateRow(
            long id,
            long businessDomainId,
            String scopeType,
            String title,
            String content,
            String category,
            String status,
            int sortOrder) {
    }

    public record TicketReplyRow(
            long id,
            String senderType,
            String senderRole,
            Long staffAccountId,
            Long customerAccountId,
            String replyType,
            String content,
            LocalDateTime createdAt) {
    }

    public record TicketHistoryRow(
            long id,
            String action,
            String fromValue,
            String toValue,
            Long operatorSubjectId,
            String operatorActorType,
            String payloadJson,
            LocalDateTime createdAt) {
    }
}
