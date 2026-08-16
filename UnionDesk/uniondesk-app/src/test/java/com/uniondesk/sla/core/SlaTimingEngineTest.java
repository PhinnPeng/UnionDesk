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
import org.junit.jupiter.api.BeforeEach;
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

    @BeforeEach
    void setUp() {
        // 客户提单要求客户在域内存在 active 的 domain_customer 关系（FR-05）
        jdbcTemplate.update("""
                        INSERT INTO domain_customer (
                            customer_account_id, business_domain_id, status, source,
                            activated_at, disabled_at, deleted_at, created_at, updated_at
                        )
                        VALUES (1, ?, 'active', 'manual',
                            CURRENT_TIMESTAMP(3), NULL, NULL, CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3))
                        ON DUPLICATE KEY UPDATE
                            status = 'active',
                            deleted_at = NULL,
                            activated_at = COALESCE(activated_at, CURRENT_TIMESTAMP(3))
                        """,
                defaultDomainId(jdbcTemplate));
    }

    @Test
    void createCustomerTicketAppliesConfiguredDeadlines() {
        long domainId = defaultDomainId(jdbcTemplate);
        long ticketTypeId = defaultTicketTypeId(jdbcTemplate, domainId);
        createRule(domainId, ticketTypeId, 30, 90, Map.of("raise_priority_to", "urgent", "sla_status", "escalated"));

        TicketService.TicketSubmissionResult result = ticketService.createCustomerTicket(
                customerContext(domainId),
                domainId,
                new TicketService.CreateTicketCommand(ticketTypeId,
                        "SLA deadline check",
                        "请验证首响和解决截止时间",
                        Map.of("source", "integration-test"),
                        List.of(),
                        null,
                        "normal",
                        "web", null, List.of()));

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
                new TicketService.CreateTicketCommand(ticketTypeId,
                        "首响追踪",
                        "请验证领取后首响时间写入",
                        Map.of(),
                        List.of(),
                        null,
                        "normal",
                        "web", null, List.of()));

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
                new TicketService.CreateTicketCommand(ticketTypeId,
                        "关闭追踪",
                        "请验证关闭时解决时间写入",
                        Map.of(),
                        List.of(),
                        null,
                        "normal",
                        "web", null, List.of()));

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
        // 终态统一：取消 resolved，唯一最终态 stopped
        assertThat(ticket.slaStatus()).isEqualTo("stopped");
        assertThat(notificationCenterService.unreadCount(1L)).isEqualTo(2L);
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
                new TicketService.CreateTicketCommand(ticketTypeId,
                        "SLA 违约判断",
                        "请验证违约动作会提升优先级",
                        Map.of(),
                        List.of(),
                        null,
                        "normal",
                        "web", null, List.of()));

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

    @Test
    void globalRuleAppliesDeadlinesWhenNoDomainRuleMatches() {
        long domainId = defaultDomainId(jdbcTemplate);
        long ticketTypeId = defaultTicketTypeId(jdbcTemplate, domainId);
        slaService.createGlobalSlaRule(new SlaService.SlaRuleCommand(
                "全局默认规则", null, null, null, 10, 25, false, Map.of()));

        TicketService.TicketSubmissionResult result = ticketService.createCustomerTicket(
                customerContext(domainId),
                domainId,
                new TicketService.CreateTicketCommand(ticketTypeId,
                        "全局兜底",
                        "验证无域规则时全局规则兜底设置 deadline",
                        Map.of(),
                        List.of(),
                        null,
                        "normal",
                        "web", null, List.of()));

        TicketSnapshot ticket = loadTicket(result.id());
        assertThat(ticket.slaStatus()).isEqualTo("tracking");
        assertThat(ticket.firstResponseDeadline()).isEqualTo(ticket.createdAt().plusMinutes(10));
        assertThat(ticket.resolutionDeadline()).isEqualTo(ticket.createdAt().plusMinutes(25));
    }

    @Test
    void domainRuleTakesPrecedenceOverGlobalRule() {
        long domainId = defaultDomainId(jdbcTemplate);
        long ticketTypeId = defaultTicketTypeId(jdbcTemplate, domainId);
        slaService.createGlobalSlaRule(new SlaService.SlaRuleCommand(
                "全局默认规则", null, null, null, 10, 25, false, Map.of()));
        createRule(domainId, ticketTypeId, 30, 90, Map.of());

        TicketService.TicketSubmissionResult result = ticketService.createCustomerTicket(
                customerContext(domainId),
                domainId,
                new TicketService.CreateTicketCommand(ticketTypeId,
                        "事项优先",
                        "验证域规则命中时全局规则不生效",
                        Map.of(),
                        List.of(),
                        null,
                        "normal",
                        "web", null, List.of()));

        TicketSnapshot ticket = loadTicket(result.id());
        assertThat(ticket.firstResponseDeadline()).isEqualTo(ticket.createdAt().plusMinutes(30));
        assertThat(ticket.resolutionDeadline()).isEqualTo(ticket.createdAt().plusMinutes(90));
    }

    @Test
    void breachEscalatesPriorityBySortOrderAndIsIdempotent() {
        long domainId = defaultDomainId(jdbcTemplate);
        long ticketTypeId = defaultTicketTypeId(jdbcTemplate, domainId);
        createRule(domainId, ticketTypeId, 1, 2, Map.of("escalate_priority", true));

        TicketService.TicketSubmissionResult result = ticketService.createCustomerTicket(
                customerContext(domainId),
                domainId,
                new TicketService.CreateTicketCommand(ticketTypeId,
                        "按序升级",
                        "验证违约时 normal → high（sort_order 更紧急下一级）",
                        Map.of(),
                        List.of(),
                        null,
                        "normal",
                        "web", null, List.of()));
        backdateDeadlines(result.id());

        ticketService.processSlaBreach(domainId, result.id());

        TicketSnapshot ticket = loadTicket(result.id());
        assertThat(ticket.slaStatus()).isEqualTo("breached");
        assertThat(ticket.priority()).isEqualTo("high");

        // 幂等：动作每工单仅执行一次，再次评估不重复升级
        ticketService.processSlaBreach(domainId, result.id());
        assertThat(loadTicket(result.id()).priority()).isEqualTo("high");
    }

    @Test
    void breachExecutesAssignAndWatcherActionsOnce() {
        long domainId = defaultDomainId(jdbcTemplate);
        long ticketTypeId = defaultTicketTypeId(jdbcTemplate, domainId);
        createRule(domainId, ticketTypeId, 1, 2, Map.of(
                "assign_to_staff_account_id", 2,
                "add_watcher_staff_account_ids", List.of(2, 9)));

        TicketService.TicketSubmissionResult result = ticketService.createCustomerTicket(
                customerContext(domainId),
                domainId,
                new TicketService.CreateTicketCommand(ticketTypeId,
                        "违约动作",
                        "验证超时强制换处理人并追加关注人",
                        Map.of(),
                        List.of(),
                        null,
                        "normal",
                        "web", null, List.of()));
        backdateDeadlines(result.id());

        ticketService.processSlaBreach(domainId, result.id());

        TicketSnapshot ticket = loadTicket(result.id());
        assertThat(ticket.assignedTo()).isEqualTo(2L);
        assertThat(watcherIds(result.id())).containsExactlyInAnyOrder(2L, 9L);

        // 幂等：二次评估不重复追加
        ticketService.processSlaBreach(domainId, result.id());
        assertThat(watcherIds(result.id())).containsExactlyInAnyOrder(2L, 9L);
    }

    @Test
    void resolutionBreachPersistsAfterFirstResponse() {
        long domainId = defaultDomainId(jdbcTemplate);
        long ticketTypeId = defaultTicketTypeId(jdbcTemplate, domainId);
        createRule(domainId, ticketTypeId, 1, 1, Map.of("escalate_priority", true));

        TicketService.TicketSubmissionResult result = ticketService.createCustomerTicket(
                customerContext(domainId),
                domainId,
                new TicketService.CreateTicketCommand(ticketTypeId,
                        "回复不解超时",
                        "验证首响后解决时限仍超时保持 breached",
                        Map.of(),
                        List.of(),
                        null,
                        "normal",
                        "web", null, List.of()));
        backdateDeadlines(result.id());
        jdbcTemplate.update(
                "UPDATE ticket SET sla_first_responded_at = ? WHERE id = ?",
                LocalDateTime.now(clock),
                result.id());

        SlaService.SlaBreachDecision decision = slaService.evaluateTicket(domainId, result.id());

        assertThat(decision.breached()).isTrue();
        assertThat(decision.firstResponseBreached()).isFalse();
        assertThat(decision.nextStatus()).isEqualTo("breached");
    }

    @Test
    void terminalStoppedStatusIsNotOverwrittenByEvaluation() {
        long domainId = defaultDomainId(jdbcTemplate);
        long ticketTypeId = defaultTicketTypeId(jdbcTemplate, domainId);
        createRule(domainId, ticketTypeId, 1, 2, Map.of("raise_priority_to", "urgent"));

        TicketService.TicketSubmissionResult result = ticketService.createCustomerTicket(
                customerContext(domainId),
                domainId,
                new TicketService.CreateTicketCommand(ticketTypeId,
                        "终态保护",
                        "验证流转到终态后 SLA 状态 stopped 不被评估覆盖",
                        Map.of(),
                        List.of(),
                        null,
                        "normal",
                        "web", null, List.of()));

        long ticketId = result.id();
        long claimVersion = loadTicket(ticketId).version();
        ticketService.claimTicket(agentContext(domainId), domainId, ticketId, new TicketService.ClaimTicketCommand(claimVersion));
        long closeVersion = loadTicket(ticketId).version();
        ticketService.changeTicketStatus(
                agentContext(domainId),
                domainId,
                ticketId,
                new TicketService.ChangeTicketStatusCommand("resolved", closeVersion, null, null));
        assertThat(loadTicket(ticketId).slaStatus()).isEqualTo("stopped");

        backdateDeadlines(ticketId);
        SlaService.SlaBreachDecision decision = slaService.evaluateTicket(domainId, ticketId);

        assertThat(decision.breached()).isFalse();
        assertThat(loadTicket(ticketId).slaStatus()).isEqualTo("stopped");
        assertThat(loadTicket(ticketId).priority()).isEqualTo("normal");
    }

    private void backdateDeadlines(long ticketId) {
        LocalDateTime now = LocalDateTime.now(clock);
        jdbcTemplate.update("""
                        UPDATE ticket
                        SET sla_first_response_deadline = ?,
                            sla_resolution_deadline = ?
                        WHERE id = ?
                        """,
                now.minusHours(8).minusMinutes(2),
                now.minusHours(8).minusMinutes(1),
                ticketId);
    }

    private List<Long> watcherIds(long ticketId) {
        return jdbcTemplate.queryForList(
                "SELECT staff_account_id FROM ticket_watcher WHERE ticket_id = ? ORDER BY staff_account_id",
                Long.class,
                ticketId);
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
