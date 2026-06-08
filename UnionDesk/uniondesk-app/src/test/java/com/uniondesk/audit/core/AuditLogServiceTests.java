package com.uniondesk.audit.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.uniondesk.audit.entity.AuditLogViewPo;
import com.uniondesk.audit.entity.LoginLogViewPo;
import com.uniondesk.audit.repository.AuditLogRepository;
import com.uniondesk.audit.web.AuditDtos;
import com.uniondesk.common.web.PageResult;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuditLogServiceTests {

    @Mock
    private AuditLogRepository auditLogRepository;

    private AuditLogService auditLogService;

    @BeforeEach
    void setUp() {
        auditLogService = new AuditLogService(auditLogRepository);
    }

    @Test
    void listPlatformAuditLogsAppliesFiltersAndPaging() {
        LocalDateTime start = LocalDateTime.parse("2026-05-03T08:00:00");
        LocalDateTime end = LocalDateTime.parse("2026-05-03T09:00:00");
        AuditLogViewPo row = new AuditLogViewPo();
        row.setId(1L);
        row.setBusinessDomainId(10L);
        row.setOperatorSubjectId(2L);
        row.setOperatorName("admin");
        row.setOperatorActorType("staff");
        row.setTarget("ticket:1");
        row.setAction("ticket.create");
        row.setDetail("{\"k\":\"v\"}");
        row.setResult("success");
        row.setOccurredAt(LocalDateTime.parse("2026-05-03T08:30:00"));
        row.setRequestId("req-1");
        row.setIp("127.0.0.1");

        when(auditLogRepository.countAuditLogs(
                eq(10L), eq("admin"), eq("ticket.create"),
                isNull(), isNull(), isNull(), isNull(), isNull(),
                eq(start), eq(end)))
                .thenReturn(2L);
        when(auditLogRepository.findAuditLogs(
                eq(10L), eq("admin"), eq("ticket.create"),
                isNull(), isNull(), isNull(), isNull(), isNull(),
                eq(start), eq(end),
                eq(0), eq(250)))
                .thenReturn(List.of(row));

        PageResult<AuditDtos.AuditLogView> page = auditLogService.listPlatformAuditLogs(
                0, 250, 10L, "admin", "ticket.create", start, end);

        assertThat(page.total()).isEqualTo(2L);
        assertThat(page.list()).hasSize(1);
        assertThat(page.list().get(0).action()).isEqualTo("ticket.create");

        verify(auditLogRepository).findAuditLogs(
                eq(10L), eq("admin"), eq("ticket.create"),
                isNull(), isNull(), isNull(), isNull(), isNull(),
                eq(start), eq(end),
                eq(0), eq(250));
    }

    @Test
    void listDomainLoginLogsAppliesFiltersAndPaging() {
        LocalDateTime end = LocalDateTime.parse("2026-05-03T09:00:00");
        LoginLogViewPo row = new LoginLogViewPo();
        row.setId(1L);
        row.setSubjectId(2L);
        row.setOperatorName("admin");
        row.setBusinessDomainId(10L);
        row.setDomainName("Demo Domain");
        row.setLoginName("admin");
        row.setPortalType("staff");
        row.setClientCode("ud-admin-web");
        row.setEventType("LOGIN");
        row.setIp("127.0.0.1");
        row.setUserAgent("JUnit");
        row.setResult("success");
        row.setCreatedAt(LocalDateTime.parse("2026-05-03T08:30:00"));

        when(auditLogRepository.countLoginLogs(
                isNull(), eq(10L),
                isNull(), isNull(), eq("LOGIN"), eq("success"),
                eq("admin"), isNull(), isNull(), isNull(),
                isNull(), eq(end)))
                .thenReturn(3L);
        when(auditLogRepository.findLoginLogs(
                isNull(), eq(10L),
                isNull(), isNull(), eq("LOGIN"), eq("success"),
                eq("admin"), isNull(), isNull(), isNull(),
                isNull(), eq(end),
                eq(3), eq(15)))
                .thenReturn(List.of(row));

        PageResult<AuditDtos.LoginLogView> page = auditLogService.listDomainLoginLogs(
                10L, 3, 15, "admin", "SUCCESS", null, end);

        assertThat(page.total()).isEqualTo(3L);
        assertThat(page.list()).hasSize(1);
        assertThat(page.list().get(0).result()).isEqualTo("success");
    }
}
