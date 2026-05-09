package com.uniondesk.ticket.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uniondesk.auth.core.UserContext;
import com.uniondesk.attachment.core.AttachmentService;
import com.uniondesk.notification.core.NotificationCenterService;
import com.uniondesk.sla.core.SlaService;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class TicketService {

    private static final DateTimeFormatter TICKET_NO_DATE_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final NotificationCenterService notificationCenterService;
    private final SlaService slaService;
    private final AttachmentService attachmentService;

    public TicketService(
            JdbcTemplate jdbcTemplate,
            ObjectMapper objectMapper,
            Clock clock,
            NotificationCenterService notificationCenterService,
            SlaService slaService,
            AttachmentService attachmentService) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.notificationCenterService = notificationCenterService;
        this.slaService = slaService;
        this.attachmentService = attachmentService;
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

        jdbcTemplate.update("""
                        INSERT INTO ticket (
                            ticket_no, business_domain_id, customer_id, ticket_type_id, title, description,
                            status, priority, source, assigned_to, custom_fields, version, result,
                            sla_first_response_deadline, sla_resolution_deadline, sla_first_responded_at,
                            sla_resolved_at, sla_status, sla_paused_duration, sla_pause_started_at
                        )
                        VALUES (?, ?, ?, ?, ?, ?, 'open', ?, ?, NULL, ?, 1, NULL, ?, ?, NULL, NULL, 'tracking', 0, NULL)
                        """,
                ticketNo,
                businessDomainId,
                context.userId(),
                content.ticketTypeId(),
                content.title(),
                content.description(),
                content.priority(),
                source,
                content.customFieldsJson(),
                content.slaFirstResponseDeadline(),
                content.slaResolutionDeadline());

        long ticketId = loadTicketId(ticketNo);
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
        int updated = jdbcTemplate.update("""
                        UPDATE ticket
                        SET status = ?,
                            version = version + 1,
                            updated_at = ?,
                            sla_first_responded_at = CASE
                                WHEN ? IN ('processing', 'resolved', 'closed') AND sla_first_responded_at IS NULL THEN ?
                                ELSE sla_first_responded_at
                            END,
                            sla_resolved_at = CASE
                                WHEN ? IN ('resolved', 'closed') THEN COALESCE(sla_resolved_at, ?)
                                ELSE sla_resolved_at
                            END,
                            sla_status = CASE
                                WHEN ? IN ('closed', 'resolved') THEN 'resolved'
                                WHEN ? = 'withdrawn' OR ? = 'merged' THEN 'stopped'
                                WHEN ? = 'processing' THEN COALESCE(sla_status, 'tracking')
                                ELSE sla_status
                            END
                        WHERE id = ? AND version = ?
                        """,
                command.status(),
                now,
                command.status(),
                now,
                command.status(),
                now,
                command.status(),
                command.status(),
                command.status(),
                command.status(),
                ticketId,
                command.version());
        if (updated == 0) {
            throw new IllegalArgumentException("工单已被他人修改，请刷新");
        }

        recordHistory(ticketId, businessDomainId, "status_change", current.status(), command.status(), context, Map.of(
                "version", command.version(),
                "new_status", command.status()));
        if (List.of("resolved", "closed").contains(command.status())) {
            slaService.recordResolution(businessDomainId, ticketId);
            notificationCenterService.notifyTicketStatusChanged(businessDomainId, ticketId, current.customerId(), context.userId(), command.status());
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
        int updated = jdbcTemplate.update("""
                        UPDATE ticket
                        SET assigned_to = ?,
                            assignee_staff_account_id = ?,
                            status = CASE
                                WHEN status IN ('open', 'new') THEN 'processing'
                                ELSE status
                            END,
                            version = version + 1,
                            updated_at = ?,
                            sla_first_responded_at = COALESCE(sla_first_responded_at, ?)
                        WHERE id = ? AND business_domain_id = ? AND version = ?
                        """,
                context.userId(),
                context.userId(),
                now,
                now,
                ticketId,
                businessDomainId,
                command.version());
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
        int updated = jdbcTemplate.update("""
                        UPDATE ticket
                        SET assigned_to = ?,
                            assignee_staff_account_id = ?,
                            status = CASE
                                WHEN status IN ('open', 'new') THEN 'processing'
                                ELSE status
                            END,
                            version = version + 1,
                            updated_at = ?,
                            sla_first_responded_at = COALESCE(sla_first_responded_at, ?)
                        WHERE id = ? AND business_domain_id = ? AND version = ?
                        """,
                command.assigneeStaffAccountId(),
                command.assigneeStaffAccountId(),
                now,
                now,
                ticketId,
                businessDomainId,
                command.version());
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
        jdbcTemplate.update("""
                        INSERT INTO ticket_reply (
                            ticket_id, business_domain_id, sender_user_id, sender_role, sender_type,
                            staff_account_id, customer_account_id, reply_type, content, attachment_urls, created_at
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                ticketId,
                businessDomainId,
                context.userId(),
                context.role(),
                senderType,
                staffAccountId,
                customerAccountId,
                command.quickReplyTemplateId() == null ? "text" : "quick",
                replyContent,
                attachmentUrlsJson,
                now);
        long replyId = loadLatestReplyId(ticketId);
        if (!command.attachmentIds().isEmpty()) {
            attachmentService.linkAttachments("ticket_reply", replyId, command.attachmentIds(), "reply");
        }
        int updated = jdbcTemplate.update("""
                        UPDATE ticket
                        SET status = CASE
                                WHEN ? = 'staff' AND status IN ('open', 'new') THEN 'processing'
                                ELSE status
                            END,
                            version = version + 1,
                            updated_at = ?,
                            sla_first_responded_at = CASE
                                WHEN ? = 'staff' AND sla_first_responded_at IS NULL THEN ?
                                ELSE sla_first_responded_at
                            END
                        WHERE id = ? AND business_domain_id = ? AND version = ?
                        """,
                senderType,
                now,
                senderType,
                now,
                ticketId,
                businessDomainId,
                command.version());
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
        int updated = jdbcTemplate.update("""
                        UPDATE ticket
                        SET status = 'withdrawn',
                            result = COALESCE(result, ?),
                            sla_status = 'stopped',
                            version = version + 1,
                            updated_at = CURRENT_TIMESTAMP(3)
                        WHERE id = ? AND business_domain_id = ? AND version = ?
                        """,
                StringUtils.hasText(command.reason()) ? command.reason().trim() : null,
                ticketId,
                businessDomainId,
                command.version());
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
        int updated = jdbcTemplate.update("""
                        UPDATE ticket
                        SET status = 'merged',
                            result = COALESCE(result, 'merged'),
                            sla_status = 'stopped',
                            version = version + 1,
                            updated_at = CURRENT_TIMESTAMP(3)
                        WHERE id = ? AND business_domain_id = ? AND version = ?
                        """,
                sourceTicketId,
                businessDomainId,
                command.version());
        if (updated == 0) {
            throw new IllegalArgumentException("工单已被他人修改，请刷新");
        }
        jdbcTemplate.update("""
                        INSERT INTO ticket_relation (
                            source_ticket_id, target_ticket_id, relation_type, created_by_staff_account_id, note
                        )
                        VALUES (?, ?, 'merge', ?, ?)
                        """,
                sourceTicketId,
                command.targetTicketId(),
                context.userId(),
                StringUtils.hasText(command.note()) ? command.note().trim() : null);
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
        return jdbcTemplate.query("""
                        SELECT
                            id,
                            sender_type,
                            sender_role,
                            staff_account_id,
                            customer_account_id,
                            reply_type,
                            content,
                            created_at
                        FROM ticket_reply
                        WHERE ticket_id = ? AND business_domain_id = ?
                        ORDER BY created_at ASC, id ASC
                        """,
                (rs, rowNum) -> new TicketReplyRow(
                        rs.getLong("id"),
                        rs.getString("sender_type"),
                        rs.getString("sender_role"),
                        rs.getObject("staff_account_id", Long.class),
                        rs.getObject("customer_account_id", Long.class),
                        rs.getString("reply_type"),
                        rs.getString("content"),
                        toLocalDateTime(rs.getTimestamp("created_at"))),
                ticketId,
                businessDomainId);
    }

    @Transactional(readOnly = true)
    public List<TicketHistoryRow> listTicketHistory(long businessDomainId, long ticketId) {
        return jdbcTemplate.query("""
                        SELECT
                            id,
                            action,
                            from_value,
                            to_value,
                            operator_subject_id,
                            operator_actor_type,
                            CAST(payload AS CHAR) AS payload_json,
                            created_at
                        FROM ticket_history
                        WHERE ticket_id = ? AND business_domain_id = ?
                        ORDER BY created_at ASC, id ASC
                        """,
                (rs, rowNum) -> new TicketHistoryRow(
                        rs.getLong("id"),
                        rs.getString("action"),
                        rs.getString("from_value"),
                        rs.getString("to_value"),
                        rs.getObject("operator_subject_id", Long.class),
                        rs.getString("operator_actor_type"),
                        rs.getString("payload_json"),
                        toLocalDateTime(rs.getTimestamp("created_at"))),
                ticketId,
                businessDomainId);
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
        StringBuilder sql = new StringBuilder("""
                SELECT
                    t.id,
                    t.ticket_no,
                    t.business_domain_id,
                    d.code AS business_domain_code,
                    d.name AS business_domain_name,
                    t.ticket_type_id,
                    tt.name AS ticket_type_name,
                    t.customer_id,
                    t.assigned_to,
                    t.title,
                    t.description,
                    t.status,
                    t.priority,
                    t.source,
                    t.result,
                    t.version,
                    CAST(t.custom_fields AS CHAR) AS custom_fields_json,
                    t.sla_first_response_deadline,
                    t.sla_resolution_deadline,
                    t.sla_first_responded_at,
                    t.sla_resolved_at,
                    t.sla_status,
                    t.sla_paused_duration,
                    t.sla_pause_started_at,
                    t.created_at,
                    t.updated_at,
                    (
                        SELECT MAX(r.created_at)
                        FROM ticket_reply r
                        WHERE r.ticket_id = t.id
                    ) AS last_reply_at,
                    COALESCE((
                        SELECT COUNT(*)
                        FROM ticket_reply r
                        WHERE r.ticket_id = t.id
                    ), 0) AS reply_count
                FROM ticket t
                JOIN business_domain d ON d.id = t.business_domain_id
                JOIN ticket_type tt ON tt.id = t.ticket_type_id
                WHERE t.business_domain_id = ?
                """);
        java.util.List<Object> args = new java.util.ArrayList<>();
        args.add(businessDomainId);
        if (customerUserId != null) {
            sql.append(" AND t.customer_id = ?");
            args.add(customerUserId);
        }
        if (StringUtils.hasText(status)) {
            sql.append(" AND t.status = ?");
            args.add(status.trim());
        }
        sql.append(" ORDER BY t.updated_at DESC, t.id DESC LIMIT ?");
        args.add(safeLimit);
        return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> new TicketRow(
                        rs.getLong("id"),
                        rs.getString("ticket_no"),
                        rs.getLong("business_domain_id"),
                        rs.getString("business_domain_code"),
                        rs.getString("business_domain_name"),
                        rs.getLong("ticket_type_id"),
                        rs.getString("ticket_type_name"),
                        rs.getLong("customer_id"),
                        rs.getObject("assigned_to", Long.class),
                        rs.getString("title"),
                        rs.getString("description"),
                        rs.getString("status"),
                        rs.getString("priority"),
                        rs.getString("source"),
                        rs.getString("result"),
                        rs.getInt("version"),
                        rs.getString("custom_fields_json"),
                        toLocalDateTime(rs.getTimestamp("sla_first_response_deadline")),
                        toLocalDateTime(rs.getTimestamp("sla_resolution_deadline")),
                        toLocalDateTime(rs.getTimestamp("sla_first_responded_at")),
                        toLocalDateTime(rs.getTimestamp("sla_resolved_at")),
                        rs.getString("sla_status"),
                        rs.getInt("sla_paused_duration"),
                        toLocalDateTime(rs.getTimestamp("sla_pause_started_at")),
                        toLocalDateTime(rs.getTimestamp("created_at")),
                        toLocalDateTime(rs.getTimestamp("updated_at")),
                        toLocalDateTime(rs.getTimestamp("last_reply_at")),
                        rs.getLong("reply_count")),
                args.toArray());
    }

    private long loadLatestReplyId(long ticketId) {
        Long replyId = jdbcTemplate.queryForObject("""
                        SELECT id
                        FROM ticket_reply
                        WHERE ticket_id = ?
                        ORDER BY id DESC
                        LIMIT 1
                        """,
                Long.class,
                ticketId);
        if (replyId == null) {
            throw new IllegalStateException("reply not found");
        }
        return replyId;
    }

    private String resolveReplyContent(Long quickReplyTemplateId, String content, long businessDomainId) {
        String normalizedContent = StringUtils.hasText(content) ? content.trim() : "";
        if (quickReplyTemplateId == null) {
            return normalizedContent;
        }
        QuickReplyTemplateRow template = loadQuickReplyTemplate(businessDomainId, quickReplyTemplateId);
        String templateContent = StringUtils.hasText(template.content()) ? template.content().trim() : "";
        if (!StringUtils.hasText(normalizedContent)) {
            return templateContent;
        }
        if (!StringUtils.hasText(templateContent)) {
            return normalizedContent;
        }
        return normalizedContent + "\n" + templateContent;
    }

    private QuickReplyTemplateRow loadQuickReplyTemplate(long businessDomainId, long quickReplyTemplateId) {
        try {
            return jdbcTemplate.queryForObject("""
                            SELECT id, business_domain_id, scope_type, title, content, category, status, sort_order
                            FROM quick_reply_template
                            WHERE id = ? AND business_domain_id = ? AND status = 'active'
                            LIMIT 1
                            """,
                    (rs, rowNum) -> new QuickReplyTemplateRow(
                            rs.getLong("id"),
                            rs.getLong("business_domain_id"),
                            rs.getString("scope_type"),
                            rs.getString("title"),
                            rs.getString("content"),
                            rs.getString("category"),
                            rs.getString("status"),
                            rs.getInt("sort_order")),
                    quickReplyTemplateId,
                    businessDomainId);
        } catch (EmptyResultDataAccessException ex) {
            throw new IllegalArgumentException("quick reply template not found");
        }
    }

    private void recordAudit(
            long businessDomainId,
            UserContext context,
            String target,
            String action,
            Map<String, Object> detail,
            String result) {
        jdbcTemplate.update("""
                        INSERT INTO audit_log (
                            business_domain_id, operator_subject_id, operator_actor_type, target, action,
                            detail, result, request_id
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                businessDomainId,
                context == null ? null : ensureIdentitySubject(context.userId()),
                context == null ? null : resolveActorType(context.role()),
                target,
                action,
                serializeJson(detail),
                result,
                context == null ? null : context.sessionId());
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
        String code = jdbcTemplate.queryForObject("""
                        SELECT code
                        FROM business_domain
                        WHERE id = ?
                        LIMIT 1
                        """,
                String.class,
                businessDomainId);
        if (code == null) {
            throw new IllegalArgumentException("business domain not found");
        }
        return new DomainRow(businessDomainId, code, code);
    }

    private TicketTemplateRow loadTicketTemplate(long businessDomainId, long templateId) {
        return jdbcTemplate.queryForObject("""
                        SELECT id, business_domain_id, ticket_type_id, scope, name, content_json, status, sort_order
                        FROM ticket_template
                        WHERE id = ? AND business_domain_id = ?
                        LIMIT 1
                        """,
                (rs, rowNum) -> new TicketTemplateRow(
                        rs.getLong("id"),
                        rs.getLong("business_domain_id"),
                        rs.getLong("ticket_type_id"),
                        rs.getString("scope"),
                        rs.getString("name"),
                        rs.getString("content_json"),
                        rs.getString("status"),
                        rs.getInt("sort_order")),
                templateId,
                businessDomainId);
    }

    private TicketRow loadTicketRow(long businessDomainId, long ticketId) {
        try {
            return jdbcTemplate.queryForObject("""
                            SELECT
                                t.id,
                                t.ticket_no,
                                t.business_domain_id,
                                d.code AS business_domain_code,
                                d.name AS business_domain_name,
                                t.ticket_type_id,
                                tt.name AS ticket_type_name,
                                t.customer_id,
                                t.assigned_to,
                                t.title,
                                t.description,
                                t.status,
                                t.priority,
                                t.source,
                                t.result,
                                t.version,
                                CAST(t.custom_fields AS CHAR) AS custom_fields_json,
                                t.sla_first_response_deadline,
                                t.sla_resolution_deadline,
                                t.sla_first_responded_at,
                                t.sla_resolved_at,
                                t.sla_status,
                                t.sla_paused_duration,
                                t.sla_pause_started_at,
                                t.created_at,
                                t.updated_at,
                                (
                                    SELECT MAX(r.created_at)
                                    FROM ticket_reply r
                                    WHERE r.ticket_id = t.id
                                ) AS last_reply_at,
                                COALESCE((
                                    SELECT COUNT(*)
                                    FROM ticket_reply r
                                    WHERE r.ticket_id = t.id
                                ), 0) AS reply_count
                            FROM ticket t
                            JOIN business_domain d ON d.id = t.business_domain_id
                            JOIN ticket_type tt ON tt.id = t.ticket_type_id
                            WHERE t.id = ? AND t.business_domain_id = ?
                            LIMIT 1
                            """,
                    (rs, rowNum) -> new TicketRow(
                            rs.getLong("id"),
                            rs.getString("ticket_no"),
                            rs.getLong("business_domain_id"),
                            rs.getString("business_domain_code"),
                            rs.getString("business_domain_name"),
                            rs.getLong("ticket_type_id"),
                            rs.getString("ticket_type_name"),
                            rs.getLong("customer_id"),
                            rs.getObject("assigned_to", Long.class),
                            rs.getString("title"),
                            rs.getString("description"),
                            rs.getString("status"),
                            rs.getString("priority"),
                            rs.getString("source"),
                            rs.getString("result"),
                            rs.getInt("version"),
                            rs.getString("custom_fields_json"),
                            toLocalDateTime(rs.getTimestamp("sla_first_response_deadline")),
                            toLocalDateTime(rs.getTimestamp("sla_resolution_deadline")),
                            toLocalDateTime(rs.getTimestamp("sla_first_responded_at")),
                            toLocalDateTime(rs.getTimestamp("sla_resolved_at")),
                            rs.getString("sla_status"),
                            rs.getInt("sla_paused_duration"),
                            toLocalDateTime(rs.getTimestamp("sla_pause_started_at")),
                            toLocalDateTime(rs.getTimestamp("created_at")),
                            toLocalDateTime(rs.getTimestamp("updated_at")),
                            toLocalDateTime(rs.getTimestamp("last_reply_at")),
                            rs.getLong("reply_count")),
                    ticketId,
                    businessDomainId);
        } catch (EmptyResultDataAccessException ex) {
            throw new IllegalArgumentException("ticket not found");
        }
    }

    private long loadTicketId(String ticketNo) {
        Long ticketId = jdbcTemplate.queryForObject("""
                        SELECT id
                        FROM ticket
                        WHERE ticket_no = ?
                        """,
                Long.class,
                ticketNo);
        if (ticketId == null) {
            throw new IllegalArgumentException("ticket not found");
        }
        return ticketId;
    }

    private String nextTicketNo(long businessDomainId, String domainCode) {
        String day = LocalDate.now(clock).format(TICKET_NO_DATE_FORMAT);
        Long next = jdbcTemplate.queryForObject("""
                        SELECT COALESCE(MAX(CAST(SUBSTRING_INDEX(ticket_no, '-', -1) AS UNSIGNED)), 0) + 1
                        FROM ticket
                        WHERE business_domain_id = ? AND ticket_no LIKE ?
                        """,
                Long.class,
                businessDomainId,
                domainCode + "-" + day + "-%");
        long sequence = next == null ? 1L : next;
        return domainCode + "-" + day + "-" + sequence;
    }

    private String resolvePriority(long businessDomainId, String requestedPriority) {
        if (StringUtils.hasText(requestedPriority)) {
            return requestedPriority.trim();
        }
        String defaultPriority = jdbcTemplate.queryForObject("""
                        SELECT code
                        FROM ticket_priority_level
                        WHERE business_domain_id = ? AND is_default = 1 AND status = 'active'
                        ORDER BY sort_order ASC, id ASC
                        LIMIT 1
                        """,
                String.class,
                businessDomainId);
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
        jdbcTemplate.update("""
                        INSERT INTO ticket_history (
                            ticket_id, business_domain_id, action, from_value, to_value,
                            operator_subject_id, operator_actor_type, payload
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                ticketId,
                businessDomainId,
                action,
                fromValue,
                toValue,
                context == null ? null : ensureIdentitySubject(context.userId()),
                context == null ? null : resolveActorType(context.role()),
                serializeJson(payload));
    }

    private long ensureIdentitySubject(long userId) {
        Long subjectId = resolveAccountSubjectId("customer_account", userId);
        if (subjectId != null) {
            return subjectId;
        }
        subjectId = resolveAccountSubjectId("staff_account", userId);
        if (subjectId != null) {
            return subjectId;
        }
        Long identitySubjectId;
        try {
            identitySubjectId = jdbcTemplate.queryForObject("""
                            SELECT id
                            FROM identity_subject
                            WHERE id = ?
                            LIMIT 1
                            """, Long.class, userId);
        } catch (EmptyResultDataAccessException ignored) {
            identitySubjectId = null;
        }
        if (identitySubjectId != null) {
            return identitySubjectId;
        }
        UserIdentityRow user = null;
        try {
            user = jdbcTemplate.queryForObject("""
                            SELECT id, mobile, email, username
                            FROM user_account
                            WHERE id = ?
                            LIMIT 1
                            """,
                    (rs, rowNum) -> new UserIdentityRow(
                            rs.getLong("id"),
                            rs.getString("mobile"),
                            rs.getString("email"),
                            rs.getString("username")),
                    userId);
        } catch (EmptyResultDataAccessException ignored) {
            // fall back to a deterministic placeholder phone number
        }
        String phone = user != null && StringUtils.hasText(user.mobile())
                ? user.mobile()
                : "user-" + userId;
        try {
            jdbcTemplate.update("""
                            INSERT INTO identity_subject (id, subject_type, phone, status)
                            VALUES (?, 'person', ?, 'active')
                            """,
                    userId,
                    phone);
            return userId;
        } catch (DuplicateKeyException ignored) {
            Long existingSubjectId = jdbcTemplate.queryForObject("""
                            SELECT id
                            FROM identity_subject
                            WHERE phone = ?
                            LIMIT 1
                            """,
                    Long.class,
                    phone);
            if (existingSubjectId != null) {
                return existingSubjectId;
            }
            throw ignored;
        }
    }

    private Long resolveAccountSubjectId(String tableName, long accountId) {
        try {
            return jdbcTemplate.queryForObject("""
                            SELECT subject_id
                            FROM %s
                            WHERE id = ?
                            LIMIT 1
                            """.formatted(tableName),
                    Long.class,
                    accountId);
        } catch (EmptyResultDataAccessException ignored) {
            return null;
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

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private record DomainRow(long id, String code, String name) {
    }

    private record TicketTemplateRow(long id, long businessDomainId, long ticketTypeId, String scope, String name, String contentJson, String status, int sortOrder) {
    }

    private record UserIdentityRow(long id, String mobile, String email, String username) {
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
