package com.uniondesk.ticket.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uniondesk.auth.core.UserContext;
import com.uniondesk.attachment.core.AttachmentService;
import com.uniondesk.notification.core.NotificationCenterService;
import com.uniondesk.sla.core.SlaService;
import java.sql.ResultSet;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TicketWorkflowTests {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-05-03T08:00:00Z"), ZoneOffset.UTC);

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private NotificationCenterService notificationCenterService;

    @Mock
    private SlaService slaService;

    @Mock
    private AttachmentService attachmentService;

    private TicketService ticketService;

    @BeforeEach
    void setUp() {
        ticketService = new TicketService(
                jdbcTemplate,
                new ObjectMapper(),
                CLOCK,
                notificationCenterService,
                slaService,
                attachmentService);
    }

    @Test
    void claimTicketPersistsAssignmentAndNotifiesCustomer() {
        UserContext context = new UserContext(2L, "agent", 1L, "sid-2", "ud-admin-web");
        TicketService.TicketRow row = ticketRow(101L, 1, "open", null, 1L);

        stubTicketRow(row);
        when(jdbcTemplate.update(argThat(sql -> sql != null && sql.contains("UPDATE ticket")), any(Object[].class)))
                .thenReturn(1);
        when(jdbcTemplate.update(argThat(sql -> sql != null && sql.contains("INSERT INTO ticket_history")), any(Object[].class)))
                .thenReturn(1);

        TicketService.TicketActionResult result = ticketService.claimTicket(context, 1L, 101L, new TicketService.ClaimTicketCommand(1L));

        assertThat(result.id()).isEqualTo(101L);
        verify(slaService).recordFirstResponse(1L, 101L);
        verify(slaService).evaluateTicket(1L, 101L);
        verify(notificationCenterService).notifyTicketStatusChanged(1L, 101L, 1L, 2L, "processing");
        ArgumentCaptor<Object[]> historyInsert = ArgumentCaptor.forClass(Object[].class);
        verify(jdbcTemplate).update(argThat(sql -> sql != null && sql.contains("INSERT INTO ticket_history")), historyInsert.capture());
        assertThat(historyInsert.getValue()[5]).isEqualTo(2L);
        assertThat(historyInsert.getValue()[6]).isEqualTo("staff");
    }

    @Test
    void changeTicketStatusMarksClosedAndStopsSla() {
        UserContext context = new UserContext(2L, "agent", 1L, "sid-2", "ud-admin-web");
        TicketService.TicketRow row = ticketRow(101L, 1, "processing", 2L, 1L);

        stubTicketRow(row);
        when(jdbcTemplate.update(argThat(sql -> sql != null && sql.contains("UPDATE ticket")), any(Object[].class)))
                .thenReturn(1);
        when(jdbcTemplate.update(argThat(sql -> sql != null && sql.contains("INSERT INTO ticket_history")), any(Object[].class)))
                .thenReturn(1);

        TicketService.TicketActionResult result = ticketService.changeTicketStatus(
                context,
                1L,
                101L,
                new TicketService.ChangeTicketStatusCommand("closed", 1L, null, null));

        assertThat(result.id()).isEqualTo(101L);
        verify(slaService).recordResolution(1L, 101L);
        verify(slaService).evaluateTicket(1L, 101L);
        verify(notificationCenterService).notifyTicketStatusChanged(1L, 101L, 1L, 2L, "closed");
    }

    @Test
    void changeTicketStatusRecordsSystemActorTypeInHistory() {
        UserContext context = new UserContext(9L, "system", 1L, "sid-9", "ud-admin-web");
        TicketService.TicketRow row = ticketRow(101L, 1, "open", null, 1L);

        stubTicketRow(row);
        when(jdbcTemplate.update(argThat(sql -> sql != null && sql.contains("UPDATE ticket")), any(Object[].class)))
                .thenReturn(1);
        when(jdbcTemplate.update(argThat(sql -> sql != null && sql.contains("INSERT INTO ticket_history")), any(Object[].class)))
                .thenReturn(1);

        TicketService.TicketActionResult result = ticketService.changeTicketStatus(
                context,
                1L,
                101L,
                new TicketService.ChangeTicketStatusCommand("processing", 1L, null, null));

        assertThat(result.id()).isEqualTo(101L);
        verify(slaService).recordFirstResponse(1L, 101L);
        ArgumentCaptor<Object[]> historyInsert = ArgumentCaptor.forClass(Object[].class);
        verify(jdbcTemplate).update(argThat(sql -> sql != null && sql.contains("INSERT INTO ticket_history")), historyInsert.capture());
        assertThat(historyInsert.getValue()[5]).isEqualTo(9L);
        assertThat(historyInsert.getValue()[6]).isEqualTo("system");
    }

    @Test
    void replyTicketAppendsQuickReplyAndLinksAttachments() {
        UserContext context = new UserContext(2L, "agent", 1L, "sid-2", "ud-admin-web");
        TicketService.TicketRow row = ticketRow(101L, 1, "processing", 2L, 1L);
        TicketService.QuickReplyTemplateRow template = new TicketService.QuickReplyTemplateRow(
                8L, 1L, "ticket", "提醒", "请尽快补充资料", "default", "active", 0);

        stubTicketRow(row);
        when(jdbcTemplate.queryForObject(
                argThat(sql -> sql != null && sql.contains("quick_reply_template")),
                any(RowMapper.class),
                any(Object[].class)))
                .thenReturn(template);
        when(jdbcTemplate.update(argThat(sql -> sql != null && sql.contains("INSERT INTO ticket_reply")), any(Object[].class)))
                .thenReturn(1);
        when(jdbcTemplate.update(argThat(sql -> sql != null && sql.contains("UPDATE ticket")), any(Object[].class)))
                .thenReturn(1);
        when(jdbcTemplate.update(argThat(sql -> sql != null && sql.contains("INSERT INTO ticket_history")), any(Object[].class)))
                .thenReturn(1);
        when(jdbcTemplate.queryForObject(argThat((String sql) -> sql != null && sql.contains("FROM ticket_reply")), eq(Long.class), any(Object[].class)))
                .thenReturn(501L);

        TicketService.TicketActionResult result = ticketService.replyTicket(
                context,
                1L,
                101L,
                new TicketService.ReplyTicketCommand(1L, "谢谢", 8L, List.of(7L, 8L)));

        assertThat(result.id()).isEqualTo(501L);
        verify(attachmentService).linkAttachments("ticket_reply", 501L, List.of(7L, 8L), "reply");
        verify(notificationCenterService).notifyTicketReply(1L, 101L, 1L, 2L, "staff");
        verify(slaService).recordFirstResponse(1L, 101L);
        ArgumentCaptor<Object[]> replyInsert = ArgumentCaptor.forClass(Object[].class);
        verify(jdbcTemplate).update(argThat(sql -> sql != null && sql.contains("INSERT INTO ticket_reply")), replyInsert.capture());
        assertThat(replyInsert.getValue()[7]).isEqualTo("quick");
        assertThat(replyInsert.getValue()[8]).isEqualTo("谢谢\n请尽快补充资料");
    }

    @Test
    void withdrawCustomerTicketMarksWithdrawnOnlyForOwner() {
        UserContext context = new UserContext(1L, "customer", 1L, "sid-1", "ud-customer-web");
        TicketService.TicketRow row = ticketRow(101L, 2, "processing", 2L, 1L);

        stubTicketRow(row);
        when(jdbcTemplate.update(argThat(sql -> sql != null && sql.contains("UPDATE ticket")), any(Object[].class)))
                .thenReturn(1);
        when(jdbcTemplate.update(argThat(sql -> sql != null && sql.contains("INSERT INTO ticket_history")), any(Object[].class)))
                .thenReturn(1);

        TicketService.TicketActionResult result = ticketService.withdrawCustomerTicket(
                context,
                1L,
                101L,
                new TicketService.WithdrawTicketCommand(2L, "不用处理了"));

        assertThat(result.id()).isEqualTo(101L);
        verify(notificationCenterService).notifyTicketStatusChanged(1L, 101L, 2L, 1L, "withdrawn");
    }

    @Test
    void mergeTicketCreatesRelationAndStopsSource() {
        UserContext context = new UserContext(2L, "agent", 1L, "sid-2", "ud-admin-web");
        TicketService.TicketRow source = ticketRow(101L, 3, "open", 2L, 1L);
        TicketService.TicketRow target = ticketRow(202L, 1, "processing", 2L, 1L);

        when(jdbcTemplate.queryForObject(
                argThat(sql -> sql != null && sql.contains("FROM ticket t")),
                any(RowMapper.class),
                any(Object[].class)))
                .thenReturn(source, target);
        when(jdbcTemplate.update(argThat(sql -> sql != null && sql.contains("UPDATE ticket")), any(Object[].class)))
                .thenReturn(1);
        when(jdbcTemplate.update(argThat(sql -> sql != null && sql.contains("INSERT INTO ticket_relation")), any(Object[].class)))
                .thenReturn(1);
        when(jdbcTemplate.update(argThat(sql -> sql != null && sql.contains("INSERT INTO ticket_history")), any(Object[].class)))
                .thenReturn(1);

        TicketService.TicketActionResult result = ticketService.mergeTicket(
                context,
                1L,
                101L,
                new TicketService.MergeTicketCommand(3L, 202L, "duplicate"));

        assertThat(result.id()).isEqualTo(101L);
        verify(notificationCenterService).notifyTicketMerged(1L, 101L, 1L, 2L, 202L);
    }

    @Test
    void changeTicketStatusRejectsVersionConflict() {
        UserContext context = new UserContext(2L, "agent", 1L, "sid-2", "ud-admin-web");
        TicketService.TicketRow row = ticketRow(101L, 1, "open", 2L, 1L);

        stubTicketRow(row);

        assertThatThrownBy(() -> ticketService.changeTicketStatus(
                context,
                1L,
                101L,
                new TicketService.ChangeTicketStatusCommand("resolved", 2L, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("工单已被他人修改，请刷新");
    }

    @Test
    void listTicketRepliesUsesAscendingTimelineOrder() {
        TicketService.TicketReplyRow first = new TicketService.TicketReplyRow(
                1L, "staff", "agent", 2L, null, "text", "first", LocalDateTime.parse("2026-05-03T08:01:00"));
        TicketService.TicketReplyRow second = new TicketService.TicketReplyRow(
                2L, "customer", "customer", null, 1L, "text", "second", LocalDateTime.parse("2026-05-03T08:02:00"));

        when(jdbcTemplate.query(
                argThat(sql -> sql != null && sql.contains("ORDER BY created_at ASC, id ASC")),
                any(RowMapper.class),
                any(Object[].class)))
                .thenAnswer(invocation -> List.of(first, second));

        List<TicketService.TicketReplyRow> replies = ticketService.listTicketReplies(1L, 101L);

        assertThat(replies).extracting(TicketService.TicketReplyRow::id).containsExactly(1L, 2L);
    }

    private void stubTicketRow(TicketService.TicketRow row) {
        when(jdbcTemplate.queryForObject(
                argThat(sql -> sql != null && sql.contains("FROM ticket t")),
                any(RowMapper.class),
                any(Object[].class)))
                .thenReturn(row);
    }

    private TicketService.TicketRow ticketRow(long id, int version, String status, Long assignedTo, long customerId) {
        return new TicketService.TicketRow(
                id,
                "default-20260503-1",
                1L,
                "default",
                "Default Domain",
                11L,
                "General Ticket",
                customerId,
                assignedTo,
                "无法登录",
                "登录失败需要帮助",
                status,
                "normal",
                "web",
                null,
                version,
                "{}",
                null,
                null,
                null,
                null,
                "tracking",
                0,
                null,
                LocalDateTime.parse("2026-05-03T07:00:00"),
                LocalDateTime.parse("2026-05-03T07:00:00"),
                null,
                0);
    }
}
