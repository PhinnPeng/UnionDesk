package com.uniondesk.audit.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.uniondesk.audit.web.AuditDtos;
import com.uniondesk.common.web.PageResult;
import java.time.LocalDateTime;
import java.util.List;
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
class AuditLogServiceTests {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private AuditLogService auditLogService;

    @BeforeEach
    void setUp() {
        auditLogService = new AuditLogService(jdbcTemplate);
    }

    @Test
    void listPlatformAuditLogsAppliesFiltersAndPaging() {
        when(jdbcTemplate.queryForObject(
                argThat(sql -> sql != null && sql.contains("FROM audit_log a")),
                eq(Long.class),
                any(Object[].class)))
                .thenReturn(2L);
        when(jdbcTemplate.query(
                argThat(sql -> sql != null
                        && sql.contains("FROM audit_log a")
                        && sql.contains("a.business_domain_id = ?")
                        && sql.contains("a.action LIKE ?")
                        && sql.contains("a.occurred_at >= ?")
                        && sql.contains("LIMIT ? OFFSET ?")),
                any(RowMapper.class),
                any(Object[].class)))
                .thenReturn((List) List.of(new AuditDtos.AuditLogView(
                        1L,
                        10L,
                        2L,
                        "admin",
                        "staff",
                        "ticket:1",
                        "ticket.create",
                        "{\"k\":\"v\"}",
                        "success",
                        LocalDateTime.parse("2026-05-03T08:30:00"),
                        "req-1",
                        "127.0.0.1")));

        PageResult<AuditDtos.AuditLogView> page = auditLogService.listPlatformAuditLogs(
                0,
                250,
                10L,
                "admin",
                "ticket.create",
                LocalDateTime.parse("2026-05-03T08:00:00"),
                LocalDateTime.parse("2026-05-03T09:00:00"));

        assertThat(page.total()).isEqualTo(2L);
        assertThat(page.list()).hasSize(1);
        assertThat(page.list().get(0).action()).isEqualTo("ticket.create");

        ArgumentCaptor<Object[]> countArgs = ArgumentCaptor.forClass(Object[].class);
        verify(jdbcTemplate).queryForObject(
                argThat(sql -> sql != null && sql.contains("FROM audit_log a")),
                eq(Long.class),
                countArgs.capture());
        assertThat(countArgs.getValue()).containsExactly(
                10L,
                "%admin%",
                "%admin%",
                "%admin%",
                "%admin%",
                "%ticket.create%",
                LocalDateTime.parse("2026-05-03T08:00:00"),
                LocalDateTime.parse("2026-05-03T09:00:00"));

        ArgumentCaptor<Object[]> queryArgs = ArgumentCaptor.forClass(Object[].class);
        verify(jdbcTemplate).query(
                argThat(sql -> sql != null && sql.contains("LIMIT ? OFFSET ?")),
                any(RowMapper.class),
                queryArgs.capture());
        assertThat(queryArgs.getValue()).endsWith(100, 0L);
    }

    @Test
    void listDomainLoginLogsAppliesFiltersAndPaging() {
        when(jdbcTemplate.queryForObject(
                argThat(sql -> sql != null && sql.contains("FROM login_log l")),
                eq(Long.class),
                any(Object[].class)))
                .thenReturn(3L);
        when(jdbcTemplate.query(
                argThat(sql -> sql != null
                        && sql.contains("FROM login_log l")
                        && sql.contains("l.business_domain_id = ?")
                        && sql.contains("l.result = ?")
                        && sql.contains("l.event_type = ?")
                        && sql.contains("l.created_at <= ?")
                        && sql.contains("LIMIT ? OFFSET ?")),
                any(RowMapper.class),
                any(Object[].class)))
                .thenReturn((List) List.of(new AuditDtos.LoginLogView(
                        1L,
                        2L,
                        "admin",
                        10L,
                        "Demo Domain",
                        "admin",
                        "staff",
                        "ud-admin-web",
                        "LOGIN",
                        "127.0.0.1",
                        "JUnit",
                        "success",
                        null,
                        LocalDateTime.parse("2026-05-03T08:30:00"))));

        PageResult<AuditDtos.LoginLogView> page = auditLogService.listDomainLoginLogs(
                10L,
                3,
                15,
                "admin",
                "SUCCESS",
                null,
                LocalDateTime.parse("2026-05-03T09:00:00"));

        assertThat(page.total()).isEqualTo(3L);
        assertThat(page.list()).hasSize(1);
        assertThat(page.list().get(0).result()).isEqualTo("success");

        ArgumentCaptor<Object[]> countArgs = ArgumentCaptor.forClass(Object[].class);
        verify(jdbcTemplate).queryForObject(
                argThat(sql -> sql != null && sql.contains("FROM login_log l")),
                eq(Long.class),
                countArgs.capture());
        assertThat(countArgs.getValue()).containsExactly(
                10L,
                "LOGIN",
                "success",
                "%admin%",
                "%admin%",
                "%admin%",
                "%admin%",
                "%admin%",
                "%admin%",
                LocalDateTime.parse("2026-05-03T09:00:00"));

        ArgumentCaptor<Object[]> queryArgs = ArgumentCaptor.forClass(Object[].class);
        verify(jdbcTemplate).query(
                argThat(sql -> sql != null && sql.contains("LIMIT ? OFFSET ?")),
                any(RowMapper.class),
                queryArgs.capture());
        assertThat(queryArgs.getValue()).endsWith(15, 30L);
    }
}
