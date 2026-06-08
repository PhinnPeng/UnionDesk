package com.uniondesk.common.demo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uniondesk.common.demo.DemoDtos.BusinessDomainView;
import com.uniondesk.common.demo.DemoDtos.ConsultationFlowResponse;
import com.uniondesk.common.demo.DemoDtos.ConsultationMessageRequest;
import com.uniondesk.common.demo.DemoDtos.ConsultationMessageView;
import com.uniondesk.common.demo.DemoDtos.ConsultationStatsView;
import com.uniondesk.common.demo.DemoDtos.ConsultationSummaryView;
import com.uniondesk.common.demo.DemoDtos.ConvertConsultationToTicketRequest;
import com.uniondesk.common.demo.DemoDtos.CreateTicketRequest;
import com.uniondesk.common.demo.DemoDtos.DashboardResponse;
import com.uniondesk.common.demo.DemoDtos.TicketDetailView;
import com.uniondesk.common.demo.DemoDtos.TicketReplyView;
import com.uniondesk.common.demo.DemoDtos.TicketStatsView;
import com.uniondesk.common.demo.DemoDtos.TicketSummaryView;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * BE-ARCH-ONE Step 2: DemoDataService 迁移暂缓。
 * 该服务跨 ticket / consultation / business_domain 多表聚合查询（725 行），
 * 尚无对应 demo 专用 repository 层；待 consultation 模块 repository 就绪后一并迁移。
 */
@Service
@Profile("demo")
public class DemoDataService {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public DemoDataService(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public List<BusinessDomainView> listBusinessDomains() {
        return jdbcTemplate.query("""
                        SELECT
                            d.id,
                            d.code,
                            d.name,
                            d.visibility_policy,
                            d.status,
                            COALESCE(t.total_tickets, 0) AS ticket_count,
                            COALESCE(t.open_tickets, 0) AS open_ticket_count,
                            COALESCE(c.total_sessions, 0) AS consultation_count,
                            COALESCE(c.open_sessions, 0) AS open_consultation_count
                        FROM business_domain d
                        LEFT JOIN (
                            SELECT business_domain_id,
                                   COUNT(*) AS total_tickets,
                                   SUM(CASE WHEN status IN ('open', 'new') THEN 1 ELSE 0 END) AS open_tickets
                            FROM ticket
                            GROUP BY business_domain_id
                        ) t ON t.business_domain_id = d.id
                        LEFT JOIN (
                            SELECT business_domain_id,
                                   COUNT(*) AS total_sessions,
                                   SUM(CASE WHEN session_status = 'open' THEN 1 ELSE 0 END) AS open_sessions
                            FROM consultation_session
                            GROUP BY business_domain_id
                        ) c ON c.business_domain_id = d.id
                        ORDER BY d.id
                        """,
                (rs, rowNum) -> new BusinessDomainView(
                        rs.getLong("id"),
                        rs.getString("code"),
                        rs.getString("name"),
                        rs.getString("visibility_policy"),
                        rs.getInt("status"),
                        rs.getLong("ticket_count"),
                        rs.getLong("open_ticket_count"),
                        rs.getLong("consultation_count"),
                        rs.getLong("open_consultation_count")));
    }

    public long defaultBusinessDomainId() {
        return requiredLong("SELECT id FROM business_domain WHERE code = 'default' LIMIT 1");
    }

    public long defaultCustomerId() {
        return requiredLong("SELECT id FROM user_account WHERE id = 1 LIMIT 1");
    }

    public DashboardResponse dashboard(Long businessDomainId) {
        long domainId = resolveBusinessDomainId(businessDomainId);
        BusinessDomainView businessDomain = listBusinessDomains().stream()
                .filter(item -> item.id() == domainId)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("business domain not found"));
        return new DashboardResponse(
                businessDomain,
                ticketStats(domainId),
                consultationStats(domainId),
                listTickets(domainId, null, 5),
                listConsultations(domainId, null, 5));
    }

    public List<TicketSummaryView> listTickets(Long businessDomainId, String status) {
        return listTickets(businessDomainId, status, null);
    }

    public TicketDetailView getTicket(String ticketNo) {
        TicketRow row = findTicketRowByTicketNo(ticketNo);
        return buildTicketDetail(row);
    }

    public TicketDetailView getTicketById(long id) {
        TicketRow row = findTicketRowById(id);
        return buildTicketDetail(row);
    }

    @Transactional
    public TicketDetailView createTicket(CreateTicketRequest request) {
        long domainId = resolveBusinessDomainId(request.businessDomainId());
        long customerId = request.customerId() == null ? defaultCustomerId() : request.customerId();
        long ticketTypeId = request.ticketTypeId() == null ? defaultTicketTypeId(domainId) : request.ticketTypeId();
        String ticketNo = nextTicketNo();
        String priority = StringUtils.hasText(request.priority()) ? request.priority() : "normal";
        String source = StringUtils.hasText(request.source()) ? request.source() : "web";

        jdbcTemplate.update("""
                        INSERT INTO ticket (
                            ticket_no, business_domain_id, customer_id, ticket_type_id, title, description,
                            status, priority, source
                        )
                        VALUES (?, ?, ?, ?, ?, ?, 'open', ?, ?)
                        """,
                ticketNo,
                domainId,
                customerId,
                ticketTypeId,
                request.title(),
                request.description(),
                priority,
                source);
        return getTicket(ticketNo);
    }

    @Transactional
    public TicketDetailView updateTicketStatus(String ticketNo, String status) {
        int updated = jdbcTemplate.update("UPDATE ticket SET status = ? WHERE ticket_no = ?", status, ticketNo);
        if (updated == 0) {
            throw new IllegalArgumentException("ticket not found");
        }
        return getTicket(ticketNo);
    }

    @Transactional
    public TicketDetailView updateTicketStatusById(long id, String status) {
        int updated = jdbcTemplate.update("UPDATE ticket SET status = ? WHERE id = ?", status, id);
        if (updated == 0) {
            throw new IllegalArgumentException("ticket not found");
        }
        return getTicketById(id);
    }

    public List<ConsultationSummaryView> listConsultations(Long businessDomainId, Long customerId) {
        return listConsultations(businessDomainId, customerId, null);
    }

    public List<ConsultationMessageView> listMessages(String sessionNo) {
        long sessionId = requiredLong("SELECT id FROM consultation_session WHERE session_no = ? LIMIT 1", sessionNo);
        return jdbcTemplate.query("""
                        SELECT
                            m.id,
                            s.session_no,
                            m.seq_no,
                            m.business_domain_id,
                            m.sender_role,
                            m.message_type,
                            m.content,
                            CAST(m.payload AS CHAR) AS payload_json,
                            m.created_at
                        FROM consultation_message m
                        JOIN consultation_session s ON s.id = m.consultation_session_id
                        WHERE m.consultation_session_id = ?
                        ORDER BY m.seq_no
                        """,
                (rs, rowNum) -> new ConsultationMessageView(
                        rs.getLong("id"),
                        rs.getString("session_no"),
                        rs.getInt("seq_no"),
                        rs.getLong("business_domain_id"),
                        rs.getString("sender_role"),
                        rs.getString("message_type"),
                        rs.getString("content"),
                        rs.getString("payload_json"),
                        toLocalDateTime(rs.getTimestamp("created_at"))),
                sessionId);
    }

    @Transactional
    public ConsultationMessageView sendMessage(ConsultationMessageRequest request) {
        ConsultationRow session = findConsultationRowBySessionNo(request.sessionNo());
        long domainId = request.businessDomainId() == null ? session.businessDomainId() : resolveBusinessDomainId(request.businessDomainId());
        int nextSeqNo = nextConsultationSeqNo(session.id());
        String senderRole = StringUtils.hasText(request.senderRole()) ? request.senderRole() : defaultSenderRole(request.senderUserId());
        String messageType = StringUtils.hasText(request.messageType()) ? request.messageType() : "text";
        String payloadJson = serializePayload(request.payload());
        LocalDateTime now = LocalDateTime.now();

        jdbcTemplate.update("""
                        INSERT INTO consultation_message (
                            consultation_session_id, business_domain_id, seq_no, sender_user_id,
                            sender_role, message_type, content, payload
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                session.id(),
                domainId,
                nextSeqNo,
                request.senderUserId(),
                senderRole,
                messageType,
                request.content(),
                payloadJson);

        jdbcTemplate.update("""
                        UPDATE consultation_session
                        SET last_message_at = ?
                        WHERE id = ?
                        """, now, session.id());

        long messageId = requiredLong("""
                        SELECT id
                        FROM consultation_message
                        WHERE consultation_session_id = ? AND seq_no = ?
                        LIMIT 1
                        """, session.id(), nextSeqNo);

        return new ConsultationMessageView(
                messageId,
                session.sessionNo(),
                nextSeqNo,
                domainId,
                senderRole,
                messageType,
                request.content(),
                payloadJson,
                now);
    }

    @Transactional
    public ConsultationFlowResponse convertConsultationToTicket(String sessionNo, ConvertConsultationToTicketRequest request) {
        ConsultationRow session = findConsultationRowBySessionNo(sessionNo);
        String linkedTicketNo = linkedTicketNo(session.id());
        if (linkedTicketNo != null) {
            return new ConsultationFlowResponse(consultationSummary(session), getTicket(linkedTicketNo));
        }

        TicketDetailView ticket = createTicket(new CreateTicketRequest(
                request.businessDomainId() == null ? session.businessDomainId() : request.businessDomainId(),
                request.ticketTypeId(),
                request.customerId() == null ? session.customerId() : request.customerId(),
                StringUtils.hasText(request.title()) ? request.title() : "咨询转工单",
                request.description(),
                request.priority(),
                request.source()));
        jdbcTemplate.update("""
                        INSERT INTO consultation_ticket_link (
                            consultation_session_id, ticket_id, business_domain_id, converted_by
                        )
                        VALUES (?, ?, ?, ?)
                        """,
                session.id(),
                ticket.id(),
                ticket.businessDomainId(),
                request.customerId());
        return new ConsultationFlowResponse(consultationSummary(session), ticket);
    }

    private TicketStatsView ticketStats(long businessDomainId) {
        return jdbcTemplate.query("""
                        SELECT
                            COUNT(*) AS total_tickets,
                            SUM(CASE WHEN status IN ('open', 'new') THEN 1 ELSE 0 END) AS open_tickets,
                            SUM(CASE WHEN status = 'processing' THEN 1 ELSE 0 END) AS processing_tickets,
                            SUM(CASE WHEN status = 'resolved' THEN 1 ELSE 0 END) AS resolved_tickets,
                            COALESCE((
                                SELECT COUNT(*)
                                FROM ticket_reply r
                                JOIN ticket t ON t.id = r.ticket_id
                                WHERE t.business_domain_id = ?
                            ), 0) AS total_replies
                        FROM ticket
                        WHERE business_domain_id = ?
                        """,
                (rs, rowNum) -> new TicketStatsView(
                        rs.getLong("total_tickets"),
                        rs.getLong("open_tickets"),
                        rs.getLong("processing_tickets"),
                        rs.getLong("resolved_tickets"),
                        rs.getLong("total_replies")),
                businessDomainId,
                businessDomainId)
                .stream()
                .findFirst()
                .orElse(new TicketStatsView(0, 0, 0, 0, 0));
    }

    private ConsultationStatsView consultationStats(long businessDomainId) {
        return jdbcTemplate.query("""
                        SELECT
                            COUNT(*) AS total_sessions,
                            SUM(CASE WHEN session_status = 'open' THEN 1 ELSE 0 END) AS open_sessions,
                            SUM(CASE WHEN session_status = 'closed' THEN 1 ELSE 0 END) AS closed_sessions,
                            COALESCE((
                                SELECT COUNT(*)
                                FROM consultation_message
                                WHERE business_domain_id = ?
                            ), 0) AS total_messages
                        FROM consultation_session
                        WHERE business_domain_id = ?
                        """,
                (rs, rowNum) -> new ConsultationStatsView(
                        rs.getLong("total_sessions"),
                        rs.getLong("open_sessions"),
                        rs.getLong("closed_sessions"),
                        rs.getLong("total_messages")),
                businessDomainId,
                businessDomainId)
                .stream()
                .findFirst()
                .orElse(new ConsultationStatsView(0, 0, 0, 0));
    }

    private List<TicketSummaryView> listTickets(Long businessDomainId, String status, Integer limit) {
        long domainId = resolveBusinessDomainId(businessDomainId);
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                        SELECT
                            t.id,
                            t.ticket_no,
                            t.business_domain_id,
                            d.name AS business_domain_name,
                            t.ticket_type_id,
                            tt.name AS ticket_type_name,
                            t.title,
                            t.status,
                            t.priority,
                            t.source,
                            t.customer_id,
                            t.assigned_to,
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
        args.add(domainId);
        if (StringUtils.hasText(status)) {
            sql.append(" AND t.status = ?");
            args.add(status);
        }
        sql.append(" ORDER BY t.updated_at DESC, t.id DESC");
        if (limit != null) {
            sql.append(" LIMIT ?");
            args.add(limit);
        }
        return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> new TicketSummaryView(
                rs.getLong("id"),
                rs.getString("ticket_no"),
                rs.getLong("business_domain_id"),
                rs.getString("business_domain_name"),
                rs.getLong("ticket_type_id"),
                rs.getString("ticket_type_name"),
                rs.getString("title"),
                rs.getString("status"),
                rs.getString("priority"),
                rs.getString("source"),
                rs.getLong("customer_id"),
                rs.getObject("assigned_to", Long.class),
                toLocalDateTime(rs.getTimestamp("created_at")),
                toLocalDateTime(rs.getTimestamp("updated_at")),
                toLocalDateTime(rs.getTimestamp("last_reply_at")),
                rs.getLong("reply_count")), args.toArray());
    }

    private List<ConsultationSummaryView> listConsultations(Long businessDomainId, Long customerId, Integer limit) {
        long domainId = resolveBusinessDomainId(businessDomainId);
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                        SELECT
                            s.id,
                            s.session_no,
                            s.business_domain_id,
                            d.name AS business_domain_name,
                            s.customer_id,
                            s.session_status,
                            s.assigned_to,
                            (
                                SELECT t.ticket_no
                                FROM consultation_ticket_link l
                                JOIN ticket t ON t.id = l.ticket_id
                                WHERE l.consultation_session_id = s.id
                                LIMIT 1
                            ) AS linked_ticket_no,
                            s.last_message_at,
                            s.created_at,
                            s.updated_at,
                            COALESCE((
                                SELECT COUNT(*)
                                FROM consultation_message m
                                WHERE m.consultation_session_id = s.id
                            ), 0) AS message_count
                        FROM consultation_session s
                        JOIN business_domain d ON d.id = s.business_domain_id
                        WHERE s.business_domain_id = ?
                        """);
        args.add(domainId);
        if (customerId != null) {
            sql.append(" AND s.customer_id = ?");
            args.add(customerId);
        }
        sql.append(" ORDER BY s.updated_at DESC, s.id DESC");
        if (limit != null) {
            sql.append(" LIMIT ?");
            args.add(limit);
        }
        return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> new ConsultationSummaryView(
                rs.getLong("id"),
                rs.getString("session_no"),
                rs.getLong("business_domain_id"),
                rs.getString("business_domain_name"),
                rs.getLong("customer_id"),
                rs.getString("session_status"),
                rs.getObject("assigned_to", Long.class),
                rs.getString("linked_ticket_no"),
                toLocalDateTime(rs.getTimestamp("last_message_at")),
                toLocalDateTime(rs.getTimestamp("created_at")),
                toLocalDateTime(rs.getTimestamp("updated_at")),
                rs.getLong("message_count")), args.toArray());
    }

    private TicketDetailView buildTicketDetail(TicketRow row) {
        List<TicketReplyView> replies = jdbcTemplate.query("""
                        SELECT id, sender_role, reply_type, content, created_at
                        FROM ticket_reply
                        WHERE ticket_id = ?
                        ORDER BY created_at ASC, id ASC
                        """,
                (rs, rowNum) -> new TicketReplyView(
                        rs.getLong("id"),
                        rs.getString("sender_role"),
                        rs.getString("reply_type"),
                        rs.getString("content"),
                        toLocalDateTime(rs.getTimestamp("created_at"))),
                row.id());
        return new TicketDetailView(
                row.id(),
                row.ticketNo(),
                row.businessDomainId(),
                row.businessDomainName(),
                row.ticketTypeId(),
                row.ticketTypeName(),
                row.customerId(),
                row.assignedTo(),
                row.title(),
                row.description(),
                row.status(),
                row.priority(),
                row.source(),
                row.createdAt(),
                row.updatedAt(),
                row.lastReplyAt(),
                row.replyCount(),
                replies);
    }

    private TicketRow findTicketRowByTicketNo(String ticketNo) {
        return jdbcTemplate.query("""
                        SELECT
                            t.id,
                            t.ticket_no,
                            t.business_domain_id,
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
                        WHERE t.ticket_no = ?
                        """,
                (rs, rowNum) -> new TicketRow(
                        rs.getLong("id"),
                        rs.getString("ticket_no"),
                        rs.getLong("business_domain_id"),
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
                        toLocalDateTime(rs.getTimestamp("created_at")),
                        toLocalDateTime(rs.getTimestamp("updated_at")),
                        toLocalDateTime(rs.getTimestamp("last_reply_at")),
                        rs.getLong("reply_count")),
                ticketNo)
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("ticket not found"));
    }

    private TicketRow findTicketRowById(long id) {
        return jdbcTemplate.query("""
                        SELECT
                            t.id,
                            t.ticket_no,
                            t.business_domain_id,
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
                        WHERE t.id = ?
                        """,
                (rs, rowNum) -> new TicketRow(
                        rs.getLong("id"),
                        rs.getString("ticket_no"),
                        rs.getLong("business_domain_id"),
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
                        toLocalDateTime(rs.getTimestamp("created_at")),
                        toLocalDateTime(rs.getTimestamp("updated_at")),
                        toLocalDateTime(rs.getTimestamp("last_reply_at")),
                        rs.getLong("reply_count")),
                id)
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("ticket not found"));
    }

    private ConsultationRow findConsultationRowBySessionNo(String sessionNo) {
        return jdbcTemplate.query("""
                        SELECT
                            s.id,
                            s.session_no,
                            s.business_domain_id,
                            d.name AS business_domain_name,
                            s.customer_id,
                            s.session_status,
                            s.assigned_to,
                            s.last_message_at,
                            s.created_at,
                            s.updated_at
                        FROM consultation_session s
                        JOIN business_domain d ON d.id = s.business_domain_id
                        WHERE s.session_no = ?
                        """,
                (rs, rowNum) -> new ConsultationRow(
                        rs.getLong("id"),
                        rs.getString("session_no"),
                        rs.getLong("business_domain_id"),
                        rs.getString("business_domain_name"),
                        rs.getLong("customer_id"),
                        rs.getString("session_status"),
                        rs.getObject("assigned_to", Long.class),
                        toLocalDateTime(rs.getTimestamp("last_message_at")),
                        toLocalDateTime(rs.getTimestamp("created_at")),
                        toLocalDateTime(rs.getTimestamp("updated_at"))),
                sessionNo)
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("consultation session not found"));
    }

    private ConsultationSummaryView consultationSummary(ConsultationRow row) {
        String linkedTicketNo = linkedTicketNo(row.id());
        Long messageCount = jdbcTemplate.queryForObject("""
                        SELECT COALESCE(COUNT(*), 0)
                        FROM consultation_message
                        WHERE consultation_session_id = ?
                        """, Long.class, row.id());
        return new ConsultationSummaryView(
                row.id(),
                row.sessionNo(),
                row.businessDomainId(),
                row.businessDomainName(),
                row.customerId(),
                row.sessionStatus(),
                row.assignedTo(),
                linkedTicketNo,
                row.lastMessageAt(),
                row.createdAt(),
                row.updatedAt(),
                messageCount == null ? 0 : messageCount);
    }

    private String linkedTicketNo(long consultationSessionId) {
        return jdbcTemplate.query("""
                        SELECT t.ticket_no
                        FROM consultation_ticket_link l
                        JOIN ticket t ON t.id = l.ticket_id
                        WHERE l.consultation_session_id = ?
                        LIMIT 1
                        """,
                (rs, rowNum) -> rs.getString("ticket_no"),
                consultationSessionId)
                .stream()
                .findFirst()
                .orElse(null);
    }

    private long resolveBusinessDomainId(Long businessDomainId) {
        if (businessDomainId == null) {
            return defaultBusinessDomainId();
        }
        return requiredLong("SELECT id FROM business_domain WHERE id = ? LIMIT 1", businessDomainId);
    }

    private long defaultTicketTypeId(long businessDomainId) {
        return requiredLong("""
                        SELECT id
                        FROM ticket_type
                        WHERE business_domain_id = ?
                        ORDER BY id
                        LIMIT 1
                        """, businessDomainId);
    }

    private int nextConsultationSeqNo(long sessionId) {
        Integer next = jdbcTemplate.queryForObject("""
                        SELECT COALESCE(MAX(seq_no), 0) + 1
                        FROM consultation_message
                        WHERE consultation_session_id = ?
                        """, Integer.class, sessionId);
        return next == null ? 1 : next;
    }

    private String nextTicketNo() {
        Long next = jdbcTemplate.queryForObject("""
                        SELECT COALESCE(MAX(CAST(SUBSTRING(ticket_no, 2) AS UNSIGNED)), 0) + 1
                        FROM ticket
                        """, Long.class);
        long nextValue = next == null ? 1L : next;
        return "T" + String.format("%012d", nextValue);
    }

    private String defaultSenderRole(Long senderUserId) {
        return senderUserId == null ? "customer" : "agent";
    }

    private String serializePayload(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("invalid payload");
        }
    }

    private long requiredLong(String sql, Object... args) {
        try {
            Long value = jdbcTemplate.queryForObject(sql, Long.class, args);
            if (value == null) {
                throw new IllegalArgumentException("record not found");
            }
            return value;
        } catch (EmptyResultDataAccessException ex) {
            throw new IllegalArgumentException("record not found");
        }
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private record TicketRow(
            long id,
            String ticketNo,
            long businessDomainId,
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
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            LocalDateTime lastReplyAt,
            long replyCount) {
    }

    private record ConsultationRow(
            long id,
            String sessionNo,
            long businessDomainId,
            String businessDomainName,
            long customerId,
            String sessionStatus,
            Long assignedTo,
            LocalDateTime lastMessageAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
    }
}
