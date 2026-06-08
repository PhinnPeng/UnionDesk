package com.uniondesk.sla.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

class SlaRuleControllerTests {

    @Test
    void listSlaRulesReturnsPageResult() throws Exception {
        SlaService slaService = mock(SlaService.class);
        when(slaService.listSlaRules(1L, 1, 20)).thenReturn(new PageResult<>(1, List.of(new SlaService.SlaRuleView(
                11L,
                1L,
                "默认 SLA",
                null,
                null,
                null,
                30,
                120,
                false,
                Map.of("raise_priority_to", "high"),
                LocalDateTime.parse("2026-05-03T08:00:00"),
                LocalDateTime.parse("2026-05-03T08:05:00")))));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new SlaController(slaService)).build();

        mockMvc.perform(get("/api/v1/admin/domains/1/sla-rules"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.list[0].name").value("默认 SLA"));

        verify(slaService).listSlaRules(1L, 1, 20);
    }

    @Test
    void createSlaRuleReturnsView() throws Exception {
        SlaService slaService = mock(SlaService.class);
        when(slaService.createSlaRule(eq(1L), any())).thenReturn(new SlaService.SlaRuleView(
                12L,
                1L,
                "默认 SLA",
                null,
                null,
                null,
                30,
                120,
                false,
                Map.of("raise_priority_to", "high"),
                LocalDateTime.parse("2026-05-03T08:00:00"),
                LocalDateTime.parse("2026-05-03T08:05:00")));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new SlaController(slaService)).build();

        mockMvc.perform(post("/api/v1/admin/domains/1/sla-rules")
                        .contentType("application/json")
                        .content("""
                                {
                                  "name": "默认 SLA",
                                  "firstResponseMinutes": 30,
                                  "resolutionMinutes": 120
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("默认 SLA"));

        verify(slaService).createSlaRule(eq(1L), any());
    }
}
