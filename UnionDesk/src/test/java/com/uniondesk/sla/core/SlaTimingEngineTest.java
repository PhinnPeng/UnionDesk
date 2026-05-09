package com.uniondesk.sla.core;

import static org.assertj.core.api.Assertions.assertThat;

import com.uniondesk.auth.core.UserContext;
import com.uniondesk.notification.core.NotificationCenterService;
import com.uniondesk.support.FixedClockTestConfiguration;
import com.uniondesk.support.IntegrationTestSupport;
import com.uniondesk.ticket.core.TicketService;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Import(FixedClockTestConfiguration.class)
@Transactional
class SlaTimingEngineTest extends IntegrationTestSupport {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TicketService ticketService;

    @Autowired
    private SlaService slaService;

    @Autowired
    private NotificationCenterService notificationCenterService;

    @Autowired
    private Clock clock;

    @Test
    void createCustomerTicketAppliesConfiguredDeadlines() {
        long domainId = defaultDomainId(jdbcTemplate);
        long ticketTypeId = defaultTicketTypeId(jdbcTemplate, domainId);
        createRule(domainId, ticketTypeId, 30, 90, Map.of("raise_priority_to", "urgent", "sla_status", "escalated"));

        TicketService.TicketSubmissionResult result = ticketService.createCustomerTicket(
                customerContext(domainId),
                domainId,
                new TicketService.CreateTicketCommand(
                        ticketTypeId,
                        "SLA deadline check",
                        "请验证首响和解决截止时间",
                        Map.of("source", "integration-test"),
                        List.of(),
                        null,
                        "normal",
                        "web"));

        TicketSnapshot ticket = loadTicket(result.id());
        assertThat(ticket.status()).isEqualTo("open");
        assertThat(ticket.slaStatus()).isEqualTo("tracking");
        assertThat(ticket.firstResponseDeadline()).isEqualTo(ticket.createdAt().plusMinutes(30));
        assertThat(ticket.resolutionDeadline()).isEqualTo(ticket.createdAt().plusMinutes(90));
        assertThat(ticket.firstRespondedAt()).isNull();
        assertThat(ticket.resolvedAt()).isNull();
    }

    @Test
    void claimTicketRecordsFirstResponseAndKeepsTicketInProcessing() {
        long domainId = defaultDomainId(jdbcTemplate);
        long ticketTypeId = defaultTicketTypeId(jdbcTemplate, domainId);
        createRule(domainId, ticketTypeId, 20, 60, Map.of("raise_priority_to", "urgent", "sla_status", "escalated"));

        TicketService.TicketSubmissionResult result = ticketService.createCustomerTicket(
                customerContext(domainId),
                domainId,
                new TicketService.CreateTicketCommand(
                        ticketTypeId,
                        "首响追踪",
                        "请验证领取后首响时间写入",
                        Map.of(),
                        List.of(),
                        null,
                        "normal",
                        "web"));

        long version = loadTicket(result.id()).version();
        ticketService.claimTicket(agentContext(domainId), domainId, result.id(), new TicketService.ClaimTicketCommand(version));

        TicketSnapshot ticket = loadTicket(result.id());
        assertThat(ticket.status()).isEqualTo("processing");
        assertThat(ticket.assignedTo()).isEqualTo(2L);
        assertThat(ticket.firstRespondedAt()).isNotNull();
        assertThat(ticket.slaStatus()).isEqualTo("tracking");
        assertThat(notificationCenterService.unreadCount(1L)).isEqualTo(2L);
    }

    @Test
    void changeTicketStatusToClosedRecordsResolutionAndStopsSla() {
        long domainId = defaultDomainId(jdbcTemplate);
        long ticketTypeId = defaultTicketTypeId(jdbcTemplate, domainId);
        createRule(domainId, ticketTypeId, 15, 45, Map.of("raise_priority_to", "urgent", "sla_status", "escalated"));

        TicketService.TicketSubmissionResult result = ticketService.createCustomerTicket(
                customerContext(domainId),
                domainId,
                new TicketService.CreateTicketCommand(
                        ticketTypeId,
                        "关闭追踪",
                        "请验证关闭时解决时间写入",
                        Map.of(),
                        List.of(),
                        null,
                        "normal",
                        "web"));

        long ticketId = result.id();
        long claimVersion = loadTicket(ticketId).version();
        ticketService.claimTicket(agentContext(domainId), domainId, ticketId, new TicketService.ClaimTicketCommand(claimVersion));
        long closeVersion = loadTicket(ticketId).version();
        ticketService.changeTicketStatus(
                agentContext(domainId),
                domainId,
                ticketId,
                new TicketService.ChangeTicketStatusCommand("closed", closeVersion, null, null));

        TicketSnapshot ticket = loadTicket(ticketId);
        assertThat(ticket.status()).isEqualTo("closed");
        assertThat(ticket.resolvedAt()).isNotNull();
        assertThat(ticket.slaStatus()).isEqualTo("resolved");
        assertThat(notificationCenterService.unreadCount(1L)).isEqualTo(3L);
    }

    @Test
    void evaluateTicketAppliesBreachActionToPriorityAndStatus() {
        long domainId = defaultDomainId(jdbcTemplate);
        long ticketTypeId = defaultTicketTypeId(jdbcTemplate, domainId);
        createRule(domainId, ticketTypeId, 1, 2, Map.of("raise_priority_to", "urgent", "sla_status", "escalated"));

        assertThat(clock.instant()).isEqualTo(Instant.parse("2026-05-03T08:00:00Z"));

        TicketService.TicketSubmissionResult result = ticketService.createCustomerTicket(
                customerContext(domainId),
                domainId,
                new TicketService.CreateTicketCommand(
                        ticketTypeId,
                        "SLA 违约判断",
                        "请验证违约动作会提升优先级",
                        Map.of(),
                        List.of(),
                        null,
                        "normal",
                        "web"));

        LocalDateTime now = LocalDateTime.now(clock);
        jdbcTemplate.update("""
                        UPDATE ticket
                        SET sla_first_response_deadline = ?,
                            sla_resolution_deadline = ?
                        WHERE id = ?
                        """,
                now.minusHours(8).minusMinutes(2),
                now.minusHours(8).minusMinutes(1),
                result.id());

        TicketSnapshot expiredTicket = loadTicket(result.id());
        assertThat(expiredTicket.firstResponseDeadline()).isBefore(now);
        assertThat(expiredTicket.resolutionDeadline()).isBefore(now);

        SlaService.SlaBreachDecision decision = slaService.evaluateTicket(domainId, result.id());

        assertThat(decision.breached()).isTrue();
        assertThat(decision.firstResponseBreached()).isTrue();
        assertThat(decision.nextPriority()).isEqualTo("urgent");
        assertThat(decision.nextStatus()).isEqualTo("escalated");

        TicketSnapshot ticket = loadTicket(result.id());
        assertThat(ticket.priority()).isEqualTo("urgent");
        assertThat(ticket.slaStatus()).isEqualTo("escalated");
    }

    private long createRule(long domainId, long ticketTypeId, Integer firstResponseMinutes, Integer resolutionMinutes, Map<String, Object> breachAction) {
        return slaService.createSlaRule(
                domainId,
                new SlaService.SlaRuleCommand(
                        "SLA Rule " + firstResponseMinutes + "/" + resolutionMinutes,
                        ticketTypeId,
                        null,
                        null,
                        firstResponseMinutes,
                        resolutionMinutes,
                        false,
                        breachAction)).id();
    }

    private TicketSnapshot loadTicket(long ticketId) {
        return jdbcTemplate.queryForObject("""
                        SELECT
                            id,
                            status,
                            priority,
                            sla_status,
                            assigned_to,
                            version,
                            created_at,
                            sla_first_response_deadline,
                            sla_resolution_deadline,
                            sla_first_responded_at,
                            sla_resolved_at
                        FROM ticket
                        WHERE id = ?
                        """,
                (rs, rowNum) -> new TicketSnapshot(
                        rs.getLong("id"),
                        rs.getString("status"),
                        rs.getString("priority"),
                        rs.getString("sla_status"),
                        rs.getObject("assigned_to", Long.class),
                        rs.getLong("version"),
                        toDateTime(rs.getTimestamp("created_at")),
                        toDateTime(rs.getTimestamp("sla_first_response_deadline")),
                        toDateTime(rs.getTimestamp("sla_resolution_deadline")),
                        toDateTime(rs.getTimestamp("sla_first_responded_at")),
                        toDateTime(rs.getTimestamp("sla_resolved_at"))),
                ticketId);
    }

    private LocalDateTime toDateTime(java.sql.Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private record TicketSnapshot(
            long id,
            String status,
            String priority,
            String slaStatus,
            Long assignedTo,
            long version,
            LocalDateTime createdAt,
            LocalDateTime firstResponseDeadline,
            LocalDateTime resolutionDeadline,
            LocalDateTime firstRespondedAt,
            LocalDateTime resolvedAt) {
    }
}
