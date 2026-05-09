package com.uniondesk.ticket.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uniondesk.ticket.web.TicketConfigDtos;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
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
class TicketConfigServiceTests {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private TicketConfigService ticketConfigService;

    @BeforeEach
    void setUp() {
        ticketConfigService = new TicketConfigService(jdbcTemplate, new ObjectMapper());
    }

    @Test
    void ticketTypeCrudPersistsDynamicFieldsAndMapsList() throws Exception {
        stubTicketTypeListRow();
        when(jdbcTemplate.update(argThat(sql -> sql != null && sql.contains("INSERT INTO ticket_type")), any(Object[].class)))
                .thenReturn(1);
        when(jdbcTemplate.queryForObject(eq("SELECT LAST_INSERT_ID()"), eq(Long.class))).thenReturn(11L);
        when(jdbcTemplate.queryForObject(
                argThat(sql -> sql != null && sql.contains("FROM ticket_type") && sql.contains("WHERE id = ?")),
                any(RowMapper.class),
                any(Object[].class)))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    RowMapper<Object> rowMapper = invocation.getArgument(1);
                    return rowMapper.mapRow(mockTicketTypeResultSet(11L, 1L, "default", "默认类型", "{\"flow\":[\"open\"]}"), 0);
                });

        List<TicketConfigDtos.TicketTypeView> list = ticketConfigService.listTicketTypes(1L);
        assertThat(list).hasSize(1);
        assertThat(list.get(0).dynamic_fields()).isInstanceOf(Map.class);

        TicketConfigDtos.CreateTicketTypeRequest createRequest = new TicketConfigDtos.CreateTicketTypeRequest(
                "technical",
                "技术支持",
                Map.of("flow", List.of("open", "closed")));
        TicketConfigDtos.TicketTypeView created = ticketConfigService.createTicketType(1L, createRequest);
        assertThat(created.code()).isEqualTo("technical");
        assertThat(created.dynamic_fields()).isEqualTo(Map.of("flow", List.of("open", "closed")));
        ArgumentCaptor<Object[]> typeInsert = ArgumentCaptor.forClass(Object[].class);
        verify(jdbcTemplate).update(argThat(sql -> sql != null && sql.contains("INSERT INTO ticket_type")), typeInsert.capture());
        assertThat(typeInsert.getValue()[2]).isEqualTo("技术支持");
        assertThat(typeInsert.getValue()[3].toString()).contains("\"flow\"");

        TicketConfigDtos.UpdateTicketTypeRequest updateRequest = new TicketConfigDtos.UpdateTicketTypeRequest(
                "技术支持-2",
                Map.of("flow", List.of("open")),
                "disabled");
        TicketConfigDtos.TicketTypeView updated = ticketConfigService.updateTicketType(1L, 11L, updateRequest);
        assertThat(updated.name()).isEqualTo("技术支持-2");
        assertThat(updated.dynamic_fields()).isEqualTo(Map.of("flow", List.of("open")));

        when(jdbcTemplate.update(argThat(sql -> sql != null && sql.contains("DELETE FROM ticket_type")), any(Object[].class)))
                .thenReturn(1);
        ticketConfigService.deleteTicketType(1L, 11L);
    }

    @Test
    void ticketTemplateCrudResolvesDefaultTypeAndStoresContentJson() throws Exception {
        stubTicketTemplateListRow();
        when(jdbcTemplate.queryForObject(eq("SELECT LAST_INSERT_ID()"), eq(Long.class))).thenReturn(21L);
        when(jdbcTemplate.queryForObject(
                argThat(sql -> sql != null && sql.contains("FROM ticket_type") && sql.contains("ORDER BY id ASC")),
                eq(Long.class),
                eq(1L))).thenReturn(77L);
        when(jdbcTemplate.update(argThat(sql -> sql != null && sql.contains("INSERT INTO ticket_template")), any(Object[].class)))
                .thenReturn(1);
        when(jdbcTemplate.queryForObject(
                argThat(sql -> sql != null && sql.contains("FROM ticket_template") && sql.contains("WHERE id = ?")),
                any(RowMapper.class),
                any(Object[].class)))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    RowMapper<Object> rowMapper = invocation.getArgument(1);
                    return rowMapper.mapRow(mockTicketTemplateResultSet(
                            21L,
                            1L,
                            77L,
                            "customer",
                            "常见问题",
                            "{\"fields_snapshot\":{\"title\":\"模板\"},\"content\":\"模板内容\"}",
                            10), 0);
                });

        TicketConfigDtos.CreateTicketTemplateRequest createRequest = new TicketConfigDtos.CreateTicketTemplateRequest(
                "常见问题",
                "customer_content",
                null,
                Map.of("title", "模板"),
                "模板内容",
                10);
        TicketConfigDtos.TicketTemplateView created = ticketConfigService.createTicketTemplate(1L, createRequest);
        assertThat(created.type_id()).isEqualTo("77");
        assertThat(created.fields_snapshot()).isEqualTo(Map.of("title", "模板"));
        ArgumentCaptor<Object[]> templateInsert = ArgumentCaptor.forClass(Object[].class);
        verify(jdbcTemplate).update(argThat(sql -> sql != null && sql.contains("INSERT INTO ticket_template")), templateInsert.capture());
        assertThat(templateInsert.getValue()[2]).isEqualTo("customer");
        assertThat(templateInsert.getValue()[4].toString()).contains("\"fields_snapshot\"");

        TicketConfigDtos.UpdateTicketTemplateRequest updateRequest = new TicketConfigDtos.UpdateTicketTemplateRequest(
                "常见问题-2",
                "internal",
                "77",
                Map.of("title", "已更新"),
                "更新内容",
                20);
        TicketConfigDtos.TicketTemplateView updated = ticketConfigService.updateTicketTemplate(1L, 21L, updateRequest);
        assertThat(updated.name()).isEqualTo("常见问题-2");
        assertThat(updated.type()).isEqualTo("internal");

        when(jdbcTemplate.update(argThat(sql -> sql != null && sql.contains("DELETE FROM ticket_template")), any(Object[].class)))
                .thenReturn(1);
        ticketConfigService.deleteTicketTemplate(1L, 21L);
    }

    @Test
    void quickReplyCrudUsesScopeAndTimestampMapping() throws Exception {
        when(jdbcTemplate.query(argThat(sql -> sql != null && sql.contains("FROM quick_reply_template")), any(RowMapper.class), any(Object[].class)))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    RowMapper<Object> rowMapper = invocation.getArgument(1);
                    return List.of(rowMapper.mapRow(mockQuickReplyResultSet(31L, 1L, "问候语", "您好", "ticket", 2, "2026-05-03T12:00:00"), 0));
                });
        when(jdbcTemplate.update(argThat(sql -> sql != null && sql.contains("INSERT INTO quick_reply_template")), any(Object[].class)))
                .thenReturn(1);
        when(jdbcTemplate.queryForObject(eq("SELECT LAST_INSERT_ID()"), eq(Long.class))).thenReturn(31L);
        when(jdbcTemplate.queryForObject(
                argThat(sql -> sql != null && sql.contains("FROM quick_reply_template") && sql.contains("WHERE id = ?")),
                any(RowMapper.class),
                any(Object[].class)))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    RowMapper<Object> rowMapper = invocation.getArgument(1);
                    return rowMapper.mapRow(mockQuickReplyResultSet(31L, 1L, "问候语", "您好", "ticket", 2, "2026-05-03T12:00:00"), 0);
                });

        List<TicketConfigDtos.QuickReplyView> list = ticketConfigService.listQuickReplies(1L);
        assertThat(list).hasSize(1);
        assertThat(list.get(0).created_at()).isEqualTo("2026-05-03T20:00");

        TicketConfigDtos.CreateQuickReplyRequest createRequest = new TicketConfigDtos.CreateQuickReplyRequest(
                "问候语",
                "您好，请问有什么可以帮您",
                "ticket",
                1);
        TicketConfigDtos.QuickReplyView created = ticketConfigService.createQuickReply(1L, createRequest);
        assertThat(created.scope()).isEqualTo("ticket");
        ArgumentCaptor<Object[]> quickReplyInsert = ArgumentCaptor.forClass(Object[].class);
        verify(jdbcTemplate).update(argThat(sql -> sql != null && sql.contains("INSERT INTO quick_reply_template")), quickReplyInsert.capture());
        assertThat(quickReplyInsert.getValue()[2]).isEqualTo("问候语");

        TicketConfigDtos.UpdateQuickReplyRequest updateRequest = new TicketConfigDtos.UpdateQuickReplyRequest(
                "问候语-2",
                "您好，已更新",
                "consultation",
                3);
        TicketConfigDtos.QuickReplyView updated = ticketConfigService.updateQuickReply(1L, 31L, updateRequest);
        assertThat(updated.scope()).isEqualTo("consultation");

        when(jdbcTemplate.update(argThat(sql -> sql != null && sql.contains("DELETE FROM quick_reply_template")), any(Object[].class)))
                .thenReturn(1);
        ticketConfigService.deleteQuickReply(1L, 31L);
    }

    @Test
    void priorityLevelCrudTracksDefaultFlagAndOrdering() throws Exception {
        when(jdbcTemplate.query(argThat(sql -> sql != null && sql.contains("FROM ticket_priority_level")), any(RowMapper.class), any(Object[].class)))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    RowMapper<Object> rowMapper = invocation.getArgument(1);
                    return List.of(rowMapper.mapRow(mockPriorityLevelResultSet(41L, 1L, "normal", 20, 1, "active"), 0));
                });
        when(jdbcTemplate.update(argThat(sql -> sql != null && sql.contains("INSERT INTO ticket_priority_level")), any(Object[].class)))
                .thenReturn(1);
        when(jdbcTemplate.queryForObject(eq("SELECT LAST_INSERT_ID()"), eq(Long.class))).thenReturn(41L);
        when(jdbcTemplate.queryForObject(
                argThat(sql -> sql != null && sql.contains("FROM ticket_priority_level") && sql.contains("WHERE id = ?")),
                any(RowMapper.class),
                any(Object[].class)))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    RowMapper<Object> rowMapper = invocation.getArgument(1);
                    return rowMapper.mapRow(mockPriorityLevelResultSet(41L, 1L, "normal", 20, 1, "active"), 0);
                });

        List<TicketConfigDtos.PriorityLevelView> list = ticketConfigService.listPriorityLevels(1L);
        assertThat(list).hasSize(1);
        assertThat(list.get(0).is_default()).isTrue();

        TicketConfigDtos.CreatePriorityLevelRequest createRequest = new TicketConfigDtos.CreatePriorityLevelRequest(
                "urgent",
                "紧急",
                "#ff0000",
                5,
                true);
        TicketConfigDtos.PriorityLevelView created = ticketConfigService.createPriorityLevel(1L, createRequest);
        assertThat(created.color()).isEqualTo("#ff0000");
        ArgumentCaptor<Object[]> priorityInsert = ArgumentCaptor.forClass(Object[].class);
        verify(jdbcTemplate).update(argThat(sql -> sql != null && sql.contains("INSERT INTO ticket_priority_level")), priorityInsert.capture());
        assertThat(priorityInsert.getValue()[1]).isEqualTo("紧急");

        TicketConfigDtos.UpdatePriorityLevelRequest updateRequest = new TicketConfigDtos.UpdatePriorityLevelRequest(
                "urgent-2",
                "更紧急",
                "#00ff00",
                3,
                false);
        TicketConfigDtos.PriorityLevelView updated = ticketConfigService.updatePriorityLevel(1L, 41L, updateRequest);
        assertThat(updated.display_label()).isEqualTo("更紧急");

        when(jdbcTemplate.update(argThat(sql -> sql != null && sql.contains("DELETE FROM ticket_priority_level")), any(Object[].class)))
                .thenReturn(1);
        ticketConfigService.deletePriorityLevel(1L, 41L);
    }

    private void stubTicketTypeListRow() throws Exception {
        when(jdbcTemplate.query(argThat(sql -> sql != null && sql.contains("FROM ticket_type")), any(RowMapper.class), any(Object[].class)))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    RowMapper<Object> rowMapper = invocation.getArgument(1);
                    return List.of(rowMapper.mapRow(mockTicketTypeResultSet(1L, 1L, "default", "默认类型", "{\"flow\":[\"open\"]}"), 0));
                });
    }

    private void stubTicketTemplateListRow() throws Exception {
        when(jdbcTemplate.query(argThat(sql -> sql != null && sql.contains("FROM ticket_template")), any(RowMapper.class), any(Object[].class)))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    RowMapper<Object> rowMapper = invocation.getArgument(1);
                    return List.of(rowMapper.mapRow(mockTicketTemplateResultSet(
                            21L,
                            1L,
                            77L,
                            "customer",
                            "常见问题",
                            "{\"fields_snapshot\":{\"title\":\"模板\"},\"content\":\"模板内容\"}",
                            10), 0));
                });
    }

    private ResultSet mockTicketTypeResultSet(long id, long domainId, String code, String name, String statusFlowConfig) throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getLong("id")).thenReturn(id);
        when(rs.getLong("business_domain_id")).thenReturn(domainId);
        when(rs.getString("code")).thenReturn(code);
        when(rs.getString("name")).thenReturn(name);
        when(rs.getString("status_flow_config")).thenReturn(statusFlowConfig);
        return rs;
    }

    private ResultSet mockTicketTemplateResultSet(long id, long domainId, long ticketTypeId, String scope, String name, String contentJson, int sortOrder) throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getLong("id")).thenReturn(id);
        when(rs.getLong("business_domain_id")).thenReturn(domainId);
        when(rs.getLong("ticket_type_id")).thenReturn(ticketTypeId);
        when(rs.getString("scope")).thenReturn(scope);
        when(rs.getString("name")).thenReturn(name);
        when(rs.getString("content_json")).thenReturn(contentJson);
        when(rs.getInt("sort_order")).thenReturn(sortOrder);
        return rs;
    }

    private ResultSet mockQuickReplyResultSet(long id, long domainId, String title, String content, String scope, Integer sortOrder, String createdAt) throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getLong("id")).thenReturn(id);
        when(rs.getLong("business_domain_id")).thenReturn(domainId);
        when(rs.getString("title")).thenReturn(title);
        when(rs.getString("content")).thenReturn(content);
        when(rs.getString("scope_type")).thenReturn(scope);
        when(rs.getObject("sort_order")).thenReturn(sortOrder);
        when(rs.getTimestamp("created_at")).thenReturn(Timestamp.from(Instant.parse(createdAt + "Z")));
        return rs;
    }

    private ResultSet mockPriorityLevelResultSet(long id, long domainId, String name, int sortOrder, int isDefault, String status) throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getLong("id")).thenReturn(id);
        when(rs.getLong("business_domain_id")).thenReturn(domainId);
        when(rs.getString("name")).thenReturn(name);
        when(rs.getInt("sort_order")).thenReturn(sortOrder);
        when(rs.getInt("is_default")).thenReturn(isDefault);
        when(rs.getString("status")).thenReturn(status);
        return rs;
    }
}
