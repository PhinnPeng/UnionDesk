package com.uniondesk.sla.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.uniondesk.common.web.PageResult;
import com.uniondesk.sla.core.SlaService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class PlatformSlaControllerTests {

    @Test
    void listGlobalSlaRulesReturnsPageResult() throws Exception {
        SlaService slaService = mock(SlaService.class);
        when(slaService.listGlobalSlaRules(1, 20)).thenReturn(new PageResult<>(1, List.of(globalRuleView())));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new PlatformSlaController(slaService)).build();

        mockMvc.perform(get("/api/v1/admin/platform/sla-rules"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.list[0].name").value("全局默认 SLA"));

        verify(slaService).listGlobalSlaRules(1, 20);
    }

    @Test
    void createGlobalSlaRuleReturnsView() throws Exception {
        SlaService slaService = mock(SlaService.class);
        when(slaService.createGlobalSlaRule(any())).thenReturn(globalRuleView());
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new PlatformSlaController(slaService)).build();

        mockMvc.perform(post("/api/v1/admin/platform/sla-rules")
                        .contentType("application/json")
                        .content("""
                                {
                                  "name": "全局默认 SLA",
                                  "firstResponseMinutes": 30,
                                  "resolutionMinutes": 120
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("全局默认 SLA"));

        verify(slaService).createGlobalSlaRule(any());
    }

    @Test
    void updateGlobalSlaRuleDelegates() throws Exception {
        SlaService slaService = mock(SlaService.class);
        when(slaService.updateGlobalSlaRule(eq(11L), any())).thenReturn(globalRuleView());
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new PlatformSlaController(slaService)).build();

        mockMvc.perform(put("/api/v1/admin/platform/sla-rules/11")
                        .contentType("application/json")
                        .content("""
                                {
                                  "name": "全局默认 SLA",
                                  "firstResponseMinutes": 45,
                                  "resolutionMinutes": 120
                                }
                                """))
                .andExpect(status().isOk());

        verify(slaService).updateGlobalSlaRule(eq(11L), any());
    }

    @Test
    void deleteGlobalSlaRuleDelegates() throws Exception {
        SlaService slaService = mock(SlaService.class);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new PlatformSlaController(slaService)).build();

        mockMvc.perform(delete("/api/v1/admin/platform/sla-rules/11"))
                .andExpect(status().isNoContent());

        verify(slaService).deleteGlobalSlaRule(11L);
    }

    private SlaService.SlaRuleView globalRuleView() {
        return new SlaService.SlaRuleView(
                11L,
                null,
                "全局默认 SLA",
                null,
                null,
                null,
                30,
                120,
                false,
                Map.of(),
                LocalDateTime.parse("2026-05-03T08:00:00"),
                LocalDateTime.parse("2026-05-03T08:05:00"));
    }
}
