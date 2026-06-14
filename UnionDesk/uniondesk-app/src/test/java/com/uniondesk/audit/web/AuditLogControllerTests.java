package com.uniondesk.audit.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.uniondesk.audit.core.AuditLogService;
import com.uniondesk.common.web.PageResult;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AuditLogControllerTests {

    @Test
    void platformAuditLogsReturnsPageResult() throws Exception {
        AuditLogService auditLogService = mock(AuditLogService.class);
        when(auditLogService.listPlatformAuditLogs(
                eq(2),
                eq(10),
                eq(5L),
                eq("admin"),
                eq("ticket.create"),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any()))
                .thenReturn(new PageResult<>(1, List.of(new AuditDtos.AuditLogView(
                        1L,
                        5L,
                        2L,
                        "admin",
                        "staff",
                        "ticket:1",
                        "ticket.create",
                        "ticket.create",
                        "{\"k\":\"v\"}",
                        "success",
                        LocalDateTime.parse("2026-05-03T08:30:00"),
                        "req-1",
                        "127.0.0.1"))));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new AuditLogController(auditLogService)).build();

        mockMvc.perform(get("/api/v1/admin/audit-logs")
                        .param("page", "2")
                        .param("page_size", "10")
                        .param("domainId", "5")
                        .param("operator", "admin")
                        .param("action", "ticket.create"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.list[0].action").value("ticket.create"))
                .andExpect(jsonPath("$.list[0].operatorName").value("admin"));

        verify(auditLogService).listPlatformAuditLogs(
                eq(2),
                eq(10),
                eq(5L),
                eq("admin"),
                eq("ticket.create"),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any());
    }

    @Test
    void domainAuditLogsReturnsPageResult() throws Exception {
        AuditLogService auditLogService = mock(AuditLogService.class);
        when(auditLogService.listDomainAuditLogs(
                eq(10L),
                eq(1),
                eq(20),
                eq("admin"),
                eq("ticket.update"),
                isNull(),
                any(),
                any()))
                .thenReturn(new PageResult<>(1, List.of(new AuditDtos.AuditLogView(
                        1L,
                        10L,
                        2L,
                        "admin",
                        "staff",
                        "ticket:1",
                        "ticket.update",
                        "ticket.update",
                        "{\"k\":\"v\"}",
                        "success",
                        LocalDateTime.parse("2026-05-03T08:30:00"),
                        "req-1",
                        "127.0.0.1"))));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new AuditLogController(auditLogService)).build();

        mockMvc.perform(get("/api/v1/admin/domains/10/audit-logs")
                        .param("operator", "admin")
                        .param("action", "ticket.update"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.list[0].target").value("ticket:1"));

        verify(auditLogService).listDomainAuditLogs(eq(10L), eq(1), eq(20), eq("admin"), eq("ticket.update"), isNull(), any(), any());
    }
}
