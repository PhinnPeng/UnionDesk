package com.uniondesk.ticket.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyString;
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
import java.util.List;
import java.util.Map;
import com.uniondesk.common.event.UnionDeskEventPublisher;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
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

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@Disabled("待 TicketRepository mock 重写")
class TicketServiceTests {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-05-03T08:00:00Z"), ZoneOffset.UTC);

    @Mock
    private TicketRepository ticketRepository;
    @Mock
    private TicketReplyRepository ticketReplyRepository;
    @Mock
    private TicketHistoryRepository ticketHistoryRepository;
    @Mock
    private TicketRelationRepository ticketRelationRepository;
    @Mock
    private TicketTemplateRepository ticketTemplateRepository;
    @Mock
    private QuickReplyTemplateRepository quickReplyTemplateRepository;
    @Mock
    private AuditLogRepository auditLogRepository;
    @Mock
    private IdentitySubjectRepository identitySubjectRepository;
    @Mock
    private UserAccountRepository userAccountRepository;
    @Mock
    private CustomerAccountRepository customerAccountRepository;
    @Mock
    private StaffAccountRepository staffAccountRepository;
    @Mock
    private UnionDeskEventPublisher eventPublisher;
    @Mock
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

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
                ticketRepository,
                ticketReplyRepository,
                ticketHistoryRepository,
                ticketRelationRepository,
                ticketTemplateRepository,
                quickReplyTemplateRepository,
                auditLogRepository,
                identitySubjectRepository,
                userAccountRepository,
                customerAccountRepository,
                staffAccountRepository,
                new ObjectMapper(),
                CLOCK,
                notificationCenterService,
                slaService,
                attachmentService,
                eventPublisher);
    }

    @Test
    void createCustomerTicketGeneratesDomainDatedNumberAndPersistsHistory() {
        UserContext context = new UserContext(1L, "customer", 1L, "sid-1", "ud-customer-web");
        TicketService.CreateTicketCommand command = new TicketService.CreateTicketCommand(
                11L,
                "无法登录",
                "请帮我看一下登录失败的原因",
                Map.of("channel", "web"),
                List.of(201L, 202L),
                null,
                null,
                null);

        when(jdbcTemplate.queryForObject(anyString(), eq(String.class), any()))
                .thenAnswer(invocation -> {
                    String sql = invocation.getArgument(0);
                    if (sql != null && sql.contains("FROM business_domain")) {
                        return "default";
                    }
                    if (sql != null && sql.contains("FROM ticket_priority_level")) {
                        return null;
                    }
                    return null;
                });
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any()))
                .thenAnswer(invocation -> {
                    String sql = invocation.getArgument(0);
                    if (sql != null && sql.contains("ticket_no LIKE")) {
                        return 1L;
                    }
                    if (sql != null && sql.contains("SELECT id") && sql.contains("FROM ticket")) {
                        return 101L;
                    }
                    return null;
                });
        when(jdbcTemplate.update(argThat(sql -> sql != null && sql.contains("INSERT INTO ticket (")), any(Object[].class)))
                .thenReturn(1);
        when(jdbcTemplate.update(argThat(sql -> sql != null && sql.contains("INSERT INTO ticket_history")), any(Object[].class)))
                .thenReturn(1);

        TicketService.TicketSubmissionResult result = ticketService.createCustomerTicket(context, 1L, command);

        assertThat(result.id()).isEqualTo(101L);
        assertThat(result.ticketNo()).isEqualTo("default-20260503-1");
        ArgumentCaptor<Object[]> ticketInsert = ArgumentCaptor.forClass(Object[].class);
        verify(jdbcTemplate).update(argThat(sql -> sql != null && sql.contains("INSERT INTO ticket (")), ticketInsert.capture());
        assertThat(ticketInsert.getValue()[0]).isEqualTo("default-20260503-1");
        assertThat(ticketInsert.getValue()[1]).isEqualTo(1L);
        assertThat(ticketInsert.getValue()[2]).isEqualTo(1L);
        assertThat(ticketInsert.getValue()[4]).isEqualTo("无法登录");
        ArgumentCaptor<Object[]> historyInsert = ArgumentCaptor.forClass(Object[].class);
        verify(jdbcTemplate).update(argThat(sql -> sql != null && sql.contains("INSERT INTO ticket_history")), historyInsert.capture());
        assertThat(historyInsert.getValue()[5]).isEqualTo(1L);
        assertThat(historyInsert.getValue()[6]).isEqualTo("customer");
        verify(notificationCenterService).notifyTicketCreated(eq(1L), eq(101L), eq(1L), eq(1L));
        verify(slaService).applyOnCreate(eq(1L), eq(101L), eq(11L));
    }

    @Test
    void createCustomerTicketAppliesTemplateFieldsAndHistory() {
        UserContext context = new UserContext(1L, "customer", 1L, "sid-1", "ud-customer-web");
        TicketService.CreateTicketCommand command = new TicketService.CreateTicketCommand(
                null,
                null,
                null,
                Map.of("channel", "web"),
                List.of(301L),
                88L,
                null,
                "portal");

        when(jdbcTemplate.queryForObject(anyString(), eq(String.class), any()))
                .thenAnswer(invocation -> {
                    String sql = invocation.getArgument(0);
                    if (sql != null && sql.contains("FROM business_domain")) {
                        return "default";
                    }
                    if (sql != null && sql.contains("FROM ticket_priority_level")) {
                        return null;
                    }
                    return null;
                });
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any()))
                .thenAnswer(invocation -> {
                    String sql = invocation.getArgument(0);
                    if (sql != null && sql.contains("ticket_no LIKE")) {
                        return 1L;
                    }
                    if (sql != null && sql.contains("SELECT id") && sql.contains("FROM ticket")) {
                        return 202L;
                    }
                    return null;
                });
        when(jdbcTemplate.queryForObject(
                argThat(sql -> sql != null && sql.contains("FROM ticket_template")),
                any(RowMapper.class),
                any(Object[].class)))
                .thenAnswer(invocation -> {
                    RowMapper<?> mapper = invocation.getArgument(1);
                    ResultSet rs = mock(ResultSet.class);
                    when(rs.getLong("id")).thenReturn(88L);
                    when(rs.getLong("business_domain_id")).thenReturn(1L);
                    when(rs.getLong("ticket_type_id")).thenReturn(77L);
                    when(rs.getString("scope")).thenReturn("customer_content");
                    when(rs.getString("name")).thenReturn("常见问题");
                    when(rs.getString("content_json")).thenReturn("""
                            {"title":"模板标题","description":"模板描述","ticket_type_id":77,"dynamic_data":{"channel":"template","from":"content"}}
                            """);
                    when(rs.getString("status")).thenReturn("active");
                    when(rs.getInt("sort_order")).thenReturn(1);
                    return mapper.mapRow(rs, 0);
                });
        when(jdbcTemplate.update(argThat(sql -> sql != null && sql.contains("INSERT INTO ticket (")), any(Object[].class)))
                .thenReturn(1);
        when(jdbcTemplate.update(argThat(sql -> sql != null && sql.contains("INSERT INTO ticket_history")), any(Object[].class)))
                .thenReturn(1);

        TicketService.TicketSubmissionResult result = ticketService.createCustomerTicket(context, 1L, command);

        assertThat(result.id()).isEqualTo(202L);
        ArgumentCaptor<Object[]> ticketInsert = ArgumentCaptor.forClass(Object[].class);
        verify(jdbcTemplate).update(argThat(sql -> sql != null && sql.contains("INSERT INTO ticket (")), ticketInsert.capture());
        assertThat(ticketInsert.getValue()[3]).isEqualTo(77L);
        assertThat(ticketInsert.getValue()[4]).isEqualTo("模板标题");
        assertThat(ticketInsert.getValue()[5]).isEqualTo("模板描述");
        assertThat(ticketInsert.getValue()[8].toString()).contains("\"channel\":\"template\"");
        verify(attachmentService).linkAttachmentsToTicket(202L, List.of(301L), "public");
        verify(slaService).applyOnCreate(eq(1L), eq(202L), eq(77L));
        ArgumentCaptor<Object[]> historyInsert = ArgumentCaptor.forClass(Object[].class);
        verify(jdbcTemplate).update(argThat(sql -> sql != null && sql.contains("INSERT INTO ticket_history")), historyInsert.capture());
        assertThat(historyInsert.getValue()[5]).isEqualTo(1L);
        assertThat(historyInsert.getValue()[6]).isEqualTo("customer");
    }

    @Test
    void changeTicketStatusRejectsIllegalTransitionFromOpenToClosed() throws Exception {
        UserContext context = new UserContext(2L, "agent", 1L, "sid-2", "ud-admin-web");
        TicketService.ChangeTicketStatusCommand command = new TicketService.ChangeTicketStatusCommand(
                "closed",
                1L,
                null,
                null);

        when(jdbcTemplate.queryForObject(
                argThat(sql -> sql != null && sql.contains("FROM ticket t")
                        && sql.contains("WHERE t.id = ?")
                        && sql.contains("t.business_domain_id = ?")),
                any(RowMapper.class),
                any(Object[].class)))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    RowMapper<TicketService.TicketRow> mapper = invocation.getArgument(1);
                    ResultSet rs = mock(ResultSet.class);
                    when(rs.getLong("id")).thenReturn(101L);
                    when(rs.getString("ticket_no")).thenReturn("default-20260503-1");
                    when(rs.getLong("business_domain_id")).thenReturn(1L);
                    when(rs.getString("business_domain_code")).thenReturn("default");
                    when(rs.getString("business_domain_name")).thenReturn("Default Domain");
                    when(rs.getLong("ticket_type_id")).thenReturn(11L);
                    when(rs.getString("ticket_type_name")).thenReturn("技术支持");
                    when(rs.getLong("customer_id")).thenReturn(1L);
                    when(rs.getObject("assigned_to", Long.class)).thenReturn(2L);
                    when(rs.getString("title")).thenReturn("无法登录");
                    when(rs.getString("description")).thenReturn("请帮我看一下登录失败的原因");
                    when(rs.getString("status")).thenReturn("open");
                    when(rs.getString("priority")).thenReturn("normal");
                    when(rs.getString("result")).thenReturn(null);
                    when(rs.getString("source")).thenReturn("web");
                    when(rs.getInt("version")).thenReturn(1);
                    when(rs.getString("custom_fields_json")).thenReturn("{}");
                    when(rs.getTimestamp("sla_first_response_deadline")).thenReturn(null);
                    when(rs.getTimestamp("sla_resolution_deadline")).thenReturn(null);
                    when(rs.getTimestamp("sla_first_responded_at")).thenReturn(null);
                    when(rs.getTimestamp("sla_resolved_at")).thenReturn(null);
                    when(rs.getString("sla_status")).thenReturn("tracking");
                    when(rs.getInt("sla_paused_duration")).thenReturn(0);
                    when(rs.getTimestamp("sla_pause_started_at")).thenReturn(null);
                    when(rs.getTimestamp("created_at")).thenReturn(java.sql.Timestamp.from(Instant.parse("2026-05-03T07:00:00Z")));
                    when(rs.getTimestamp("updated_at")).thenReturn(java.sql.Timestamp.from(Instant.parse("2026-05-03T07:00:00Z")));
                    when(rs.getTimestamp("last_reply_at")).thenReturn(null);
                    when(rs.getLong("reply_count")).thenReturn(0L);
                    return mapper.mapRow(rs, 0);
                });

        assertThatThrownBy(() -> ticketService.changeTicketStatus(context, 1L, 101L, command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("状态流转不合法");
    }
}
