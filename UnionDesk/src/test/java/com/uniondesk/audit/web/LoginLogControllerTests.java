package com.uniondesk.audit.web;

import static org.mockito.ArgumentMatchers.any;
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

class LoginLogControllerTests {

    @Test
    void domainLoginLogsReturnsPageResult() throws Exception {
        AuditLogService auditLogService = mock(AuditLogService.class);
        when(auditLogService.listDomainLoginLogs(
                eq(10L),
                eq(1),
                eq(20),
                eq("admin"),
                eq("SUCCESS"),
                any(),
                any()))
                .thenReturn(new PageResult<>(1, List.of(new AuditDtos.LoginLogView(
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
                        LocalDateTime.parse("2026-05-03T08:30:00")))));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new LoginLogController(auditLogService)).build();

        mockMvc.perform(get("/api/v1/admin/domains/10/login-logs")
                        .param("operator", "admin")
                        .param("action", "SUCCESS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.list[0].result").value("success"))
                .andExpect(jsonPath("$.list[0].loginName").value("admin"));

        verify(auditLogService).listDomainLoginLogs(eq(10L), eq(1), eq(20), eq("admin"), eq("SUCCESS"), any(), any());
    }
}
