package com.uniondesk.sla.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uniondesk.common.web.PageResult;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class SlaService {

    private final JdbcTemplate jdbcTemplate;
    private final Clock clock;
    private final ObjectMapper objectMapper;

    public SlaService(JdbcTemplate jdbcTemplate, Clock clock, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.clock = clock;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void applyOnCreate(long businessDomainId, long ticketId, long ticketTypeId) {
        TicketSlaPolicy policy = loadPolicy(businessDomainId, ticketId, ticketTypeId);
        jdbcTemplate.update("""
                        UPDATE ticket
                        SET sla_first_response_deadline = CASE
                                WHEN ? IS NULL THEN NULL
                                ELSE TIMESTAMPADD(MINUTE, ?, created_at)
                            END,
                            sla_resolution_deadline = CASE
                                WHEN ? IS NULL THEN NULL
                                ELSE TIMESTAMPADD(MINUTE, ?, created_at)
                            END,
                            sla_status = 'tracking',
                            sla_paused_duration = COALESCE(sla_paused_duration, 0),
                            sla_pause_started_at = NULL
                        WHERE id = ?
                        """,
                policy.firstResponseMinutes(),
                policy.firstResponseMinutes(),
                policy.resolutionMinutes(),
                policy.resolutionMinutes(),
                ticketId);
    }

    @Transactional(readOnly = true)
    public PageResult<SlaRuleView> listSlaRules(long businessDomainId, int page, int pageSize) {
        int normalizedPage = Math.max(page, 1);
        int normalizedPageSize = normalizePageSize(pageSize);
        long offset = (normalizedPage - 1L) * normalizedPageSize;
        long total = countByDomain("sla_rule", businessDomainId);
        List<SlaRuleView> items = jdbcTemplate.query("""
                        SELECT
                            id,
                            business_domain_id,
                            name,
                            ticket_type_id,
                            priority_level_id,
                            calendar_id,
                            first_response_minutes,
                            resolution_minutes,
                            is_urgent_config,
                            breach_action_json,
                            created_at,
                            updated_at
                        FROM sla_rule
                        WHERE business_domain_id = ?
                        ORDER BY id DESC
                        LIMIT ? OFFSET ?
                        """,
                this::mapSlaRuleView,
                businessDomainId,
                normalizedPageSize,
                offset);
        return new PageResult<>(total, items);
    }

    @Transactional
    public SlaRuleView createSlaRule(long businessDomainId, SlaRuleCommand command) {
        jdbcTemplate.update("""
                        INSERT INTO sla_rule (
                            business_domain_id, name, ticket_type_id, priority_level_id, calendar_id,
                            first_response_minutes, resolution_minutes, is_urgent_config, breach_action_json
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                businessDomainId,
                normalizeText(command.name(), "SLA规则"),
                command.ticketTypeId(),
                command.priorityLevelId(),
                command.calendarId(),
                command.firstResponseMinutes(),
                command.resolutionMinutes(),
                command.isUrgentConfig() != null && command.isUrgentConfig(),
                serializeMap(command.breachAction()));
        return loadSlaRuleByDomainAndIdentity(businessDomainId, loadLastInsertId());
    }

    @Transactional
    public SlaRuleView updateSlaRule(long businessDomainId, long ruleId, SlaRuleCommand command) {
        jdbcTemplate.update("""
                        UPDATE sla_rule
                        SET name = ?,
                            ticket_type_id = ?,
                            priority_level_id = ?,
                            calendar_id = ?,
                            first_response_minutes = ?,
                            resolution_minutes = ?,
                            is_urgent_config = ?,
                            breach_action_json = ?,
                            updated_at = CURRENT_TIMESTAMP(3)
                        WHERE id = ? AND business_domain_id = ?
                        """,
                normalizeText(command.name(), "SLA规则"),
                command.ticketTypeId(),
                command.priorityLevelId(),
                command.calendarId(),
                command.firstResponseMinutes(),
                command.resolutionMinutes(),
                command.isUrgentConfig() != null && command.isUrgentConfig(),
                serializeMap(command.breachAction()),
                ruleId,
                businessDomainId);
        return loadSlaRule(businessDomainId, ruleId);
    }

    @Transactional(readOnly = true)
    public PageResult<SlaCalendarView> listSlaCalendars(long businessDomainId, int page, int pageSize) {
        int normalizedPage = Math.max(page, 1);
        int normalizedPageSize = normalizePageSize(pageSize);
        long offset = (normalizedPage - 1L) * normalizedPageSize;
        long total = countByDomain("sla_calendar", businessDomainId);
        List<SlaCalendarView> items = jdbcTemplate.query("""
                        SELECT
                            id,
                            business_domain_id,
                            name,
                            config,
                            created_at,
                            updated_at
                        FROM sla_calendar
                        WHERE business_domain_id = ?
                        ORDER BY id DESC
                        LIMIT ? OFFSET ?
                        """,
                this::mapSlaCalendarView,
                businessDomainId,
                normalizedPageSize,
                offset);
        return new PageResult<>(total, items);
    }

    private int normalizePageSize(int pageSize) {
        return Math.max(1, Math.min(pageSize, 100));
    }

    @Transactional
    public SlaCalendarView createSlaCalendar(long businessDomainId, SlaCalendarCommand command) {
        jdbcTemplate.update("""
                        INSERT INTO sla_calendar (
                            business_domain_id, name, config
                        )
                        VALUES (?, ?, ?)
                        """,
                businessDomainId,
                normalizeText(command.name(), "SLA工作日历"),
                serializeMap(command.config()));
        return loadSlaCalendarByDomainAndIdentity(businessDomainId, loadLastInsertId());
    }

    @Transactional
    public SlaCalendarView updateSlaCalendar(long businessDomainId, long calendarId, SlaCalendarCommand command) {
        jdbcTemplate.update("""
                        UPDATE sla_calendar
                        SET name = ?,
                            config = ?,
                            updated_at = CURRENT_TIMESTAMP(3)
                        WHERE id = ? AND business_domain_id = ?
                        """,
                normalizeText(command.name(), "SLA工作日历"),
                serializeMap(command.config()),
                calendarId,
                businessDomainId);
        return loadSlaCalendar(businessDomainId, calendarId);
    }

    @Transactional
    public void deleteSlaRule(long businessDomainId, long ruleId) {
        int updated = jdbcTemplate.update("""
                        DELETE FROM sla_rule
                        WHERE id = ? AND business_domain_id = ?
                        """,
                ruleId,
                businessDomainId);
        if (updated == 0) {
            throw new IllegalArgumentException("sla rule not found");
        }
    }

    @Transactional
    public void recordFirstResponse(long businessDomainId, long ticketId) {
        LocalDateTime now = LocalDateTime.now(clock);
        jdbcTemplate.update("""
                        UPDATE ticket
                        SET sla_first_responded_at = COALESCE(sla_first_responded_at, ?),
                            sla_status = COALESCE(sla_status, 'tracking')
                        WHERE id = ? AND business_domain_id = ?
                        """,
                now,
                ticketId,
                businessDomainId);
    }

    @Transactional
    public void recordResolution(long businessDomainId, long ticketId) {
        LocalDateTime now = LocalDateTime.now(clock);
        jdbcTemplate.update("""
                        UPDATE ticket
                        SET sla_resolved_at = COALESCE(sla_resolved_at, ?),
                            sla_status = 'resolved'
                        WHERE id = ? AND business_domain_id = ?
                        """,
                now,
                ticketId,
                businessDomainId);
    }

    @Transactional
    public SlaBreachDecision evaluateTicket(long businessDomainId, long ticketId) {
        TicketSlaSnapshot snapshot = loadSnapshot(businessDomainId, ticketId);
        LocalDateTime now = LocalDateTime.now(clock);
        boolean firstResponseBreached = snapshot.firstResponseDeadline() != null
                && snapshot.firstResponseRespondedAt() == null
                && now.isAfter(snapshot.firstResponseDeadline());
        boolean resolutionBreached = snapshot.resolutionDeadline() != null
                && snapshot.resolvedAt() == null
                && now.isAfter(snapshot.resolutionDeadline());
        if (!firstResponseBreached && !resolutionBreached) {
            return new SlaBreachDecision(false, false, snapshot.priority(), snapshot.slaStatus());
        }

        String nextPriority = snapshot.priority();
        String nextStatus = "breached";
        Map<String, Object> breachAction = snapshot.breachActionJson() == null
                ? Map.of()
                : parseMap(snapshot.breachActionJson());

        if (firstResponseBreached || resolutionBreached) {
            Object priorityValue = breachAction.get("raise_priority_to");
            if (priorityValue != null && String.valueOf(priorityValue).isBlank() == false) {
                nextPriority = String.valueOf(priorityValue);
            }
            Object statusValue = breachAction.get("sla_status");
            if (statusValue != null && String.valueOf(statusValue).isBlank() == false) {
                nextStatus = String.valueOf(statusValue);
            }
        }

        jdbcTemplate.update("""
                        UPDATE ticket
                        SET priority = ?,
                            sla_status = ?,
                            updated_at = CURRENT_TIMESTAMP(3)
                        WHERE id = ?
                        """,
                nextPriority,
                nextStatus,
                ticketId);

        return new SlaBreachDecision(true, firstResponseBreached, nextPriority, nextStatus);
    }

    private TicketSlaPolicy loadPolicy(long businessDomainId, long ticketId, long ticketTypeId) {
        String priorityCode = loadTicketPriority(ticketId);
        TicketSlaPolicy typePolicy = jdbcTemplate.query("""
                        SELECT
                            sr.first_response_minutes,
                            sr.resolution_minutes,
                            sr.breach_action_json
                        FROM sla_rule sr
                        LEFT JOIN ticket_priority_level tpl ON tpl.id = sr.priority_level_id
                        WHERE sr.business_domain_id = ?
                          AND (sr.ticket_type_id = ? OR sr.ticket_type_id IS NULL)
                          AND (sr.priority_level_id IS NULL OR tpl.code = ?)
                        ORDER BY sr.priority_level_id IS NOT NULL DESC, sr.ticket_type_id IS NOT NULL DESC, sr.id DESC
                        LIMIT 1
                        """,
                (rs, rowNum) -> new TicketSlaPolicy(
                        getInteger(rs, "first_response_minutes"),
                        getInteger(rs, "resolution_minutes"),
                        rs.getString("breach_action_json")),
                businessDomainId,
                ticketTypeId,
                priorityCode).stream().findFirst().orElse(null);
        return typePolicy == null ? new TicketSlaPolicy(null, null, null) : typePolicy;
    }

    private TicketSlaSnapshot loadSnapshot(long businessDomainId, long ticketId) {
        return jdbcTemplate.queryForObject("""
                        SELECT
                            t.priority,
                            t.sla_status,
                            t.sla_first_response_deadline,
                            t.sla_resolution_deadline,
                            t.sla_first_responded_at,
                            t.sla_resolved_at,
                            sr.breach_action_json
                        FROM ticket t
                        LEFT JOIN sla_rule sr
                          ON sr.business_domain_id = t.business_domain_id
                         AND (sr.ticket_type_id = t.ticket_type_id OR sr.ticket_type_id IS NULL)
                         AND (sr.priority_level_id IS NULL OR EXISTS (
                             SELECT 1
                             FROM ticket_priority_level tpl
                             WHERE tpl.id = sr.priority_level_id AND tpl.code = t.priority
                         ))
                        WHERE t.id = ? AND t.business_domain_id = ?
                        ORDER BY sr.priority_level_id IS NOT NULL DESC, sr.ticket_type_id IS NOT NULL DESC, sr.id DESC
                        LIMIT 1
                        """,
                (rs, rowNum) -> new TicketSlaSnapshot(
                        rs.getString("priority"),
                        rs.getString("sla_status"),
                        toLocalDateTime(rs.getTimestamp("sla_first_response_deadline")),
                        toLocalDateTime(rs.getTimestamp("sla_resolution_deadline")),
                        toLocalDateTime(rs.getTimestamp("sla_first_responded_at")),
                        toLocalDateTime(rs.getTimestamp("sla_resolved_at")),
                        rs.getString("breach_action_json")),
                ticketId,
                businessDomainId);
    }

    private SlaRuleView loadSlaRule(long businessDomainId, long ruleId) {
        return loadSlaRuleByDomainAndIdentity(businessDomainId, ruleId);
    }

    private SlaRuleView loadSlaRuleByDomainAndIdentity(long businessDomainId, long ruleId) {
        return jdbcTemplate.queryForObject("""
                        SELECT
                            id,
                            business_domain_id,
                            name,
                            ticket_type_id,
                            priority_level_id,
                            calendar_id,
                            first_response_minutes,
                            resolution_minutes,
                            is_urgent_config,
                            breach_action_json,
                            created_at,
                            updated_at
                        FROM sla_rule
                        WHERE id = ? AND business_domain_id = ?
                        LIMIT 1
                        """,
                this::mapSlaRuleView,
                ruleId,
                businessDomainId);
    }

    private SlaCalendarView loadSlaCalendar(long businessDomainId, long calendarId) {
        return loadSlaCalendarByDomainAndIdentity(businessDomainId, calendarId);
    }

    private SlaCalendarView loadSlaCalendarByDomainAndIdentity(long businessDomainId, long calendarId) {
        return jdbcTemplate.queryForObject("""
                        SELECT
                            id,
                            business_domain_id,
                            name,
                            config,
                            created_at,
                            updated_at
                        FROM sla_calendar
                        WHERE id = ? AND business_domain_id = ?
                        LIMIT 1
                        """,
                this::mapSlaCalendarView,
                calendarId,
                businessDomainId);
    }

    private long loadLastInsertId() {
        Long id = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        if (id == null) {
            throw new IllegalStateException("unable to load inserted id");
        }
        return id;
    }

    private long countByDomain(String tableName, long businessDomainId) {
        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM %s WHERE business_domain_id = ?".formatted(tableName),
                Long.class,
                businessDomainId);
        return total == null ? 0L : total;
    }

    private String loadTicketPriority(long ticketId) {
        String priority = jdbcTemplate.queryForObject("""
                        SELECT priority
                        FROM ticket
                        WHERE id = ?
                        LIMIT 1
                        """,
                String.class,
                ticketId);
        return priority == null ? "" : priority;
    }

    private String normalizeText(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value.trim() : defaultValue;
    }

    private String serializeMap(Map<String, Object> value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("invalid json payload", ex);
        }
    }

    private SlaRuleView mapSlaRuleView(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new SlaRuleView(
                rs.getLong("id"),
                rs.getLong("business_domain_id"),
                rs.getString("name"),
                rs.getObject("ticket_type_id", Long.class),
                rs.getObject("priority_level_id", Long.class),
                rs.getObject("calendar_id", Long.class),
                getInteger(rs, "first_response_minutes"),
                getInteger(rs, "resolution_minutes"),
                rs.getInt("is_urgent_config") == 1,
                rs.getString("breach_action_json") == null ? Map.of() : parseMap(rs.getString("breach_action_json")),
                toLocalDateTime(rs.getTimestamp("created_at")),
                toLocalDateTime(rs.getTimestamp("updated_at")));
    }

    private SlaCalendarView mapSlaCalendarView(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new SlaCalendarView(
                rs.getLong("id"),
                rs.getLong("business_domain_id"),
                rs.getString("name"),
                rs.getString("config") == null ? Map.of() : parseMap(rs.getString("config")),
                toLocalDateTime(rs.getTimestamp("created_at")),
                toLocalDateTime(rs.getTimestamp("updated_at")));
    }

    private Map<String, Object> parseMap(String json) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = objectMapper.readValue(json, Map.class);
            return new LinkedHashMap<>(map);
        } catch (Exception ex) {
            return Map.of();
        }
    }

    private Integer getInteger(java.sql.ResultSet rs, String column) throws java.sql.SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }

    private LocalDateTime toLocalDateTime(java.sql.Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    public record SlaBreachDecision(
            boolean breached,
            boolean firstResponseBreached,
            String nextPriority,
            String nextStatus) {
    }

    public record SlaRuleCommand(
            String name,
            Long ticketTypeId,
            Long priorityLevelId,
            Long calendarId,
            Integer firstResponseMinutes,
            Integer resolutionMinutes,
            Boolean isUrgentConfig,
            Map<String, Object> breachAction) {
    }

    public record SlaRuleView(
            long id,
            long businessDomainId,
            String name,
            Long ticketTypeId,
            Long priorityLevelId,
            Long calendarId,
            Integer firstResponseMinutes,
            Integer resolutionMinutes,
            boolean isUrgentConfig,
            Map<String, Object> breachAction,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
    }

    public record SlaCalendarCommand(
            String name,
            Map<String, Object> config) {
    }

    public record SlaCalendarView(
            long id,
            long businessDomainId,
            String name,
            Map<String, Object> config,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
    }

    private record TicketSlaPolicy(Integer firstResponseMinutes, Integer resolutionMinutes, String breachActionJson) {
    }

    private record TicketSlaSnapshot(
            String priority,
            String slaStatus,
            LocalDateTime firstResponseDeadline,
            LocalDateTime resolutionDeadline,
            LocalDateTime firstResponseRespondedAt,
            LocalDateTime resolvedAt,
            String breachActionJson) {
    }
}
