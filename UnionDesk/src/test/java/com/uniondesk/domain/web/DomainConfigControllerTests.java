package com.uniondesk.domain.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.uniondesk.domain.core.DomainConfigService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class DomainConfigControllerTests {

    @Test
    void getDomainConfigReturnsView() throws Exception {
        DomainConfigService domainConfigService = mock(DomainConfigService.class);
        when(domainConfigService.load(1L)).thenReturn(new DomainConfigService.DomainConfigView(
                1L,
                List.of(new DomainConfigService.ConfigItemView(
                        "ticket.default_priority",
                        "high",
                        "string",
                        "默认优先级",
                        LocalDateTime.parse("2026-05-03T08:00:00")))));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new DomainConfigController(domainConfigService)).build();

        mockMvc.perform(get("/api/v1/admin/domains/1/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.domainId").value(1))
                .andExpect(jsonPath("$.items[0].key").value("ticket.default_priority"));

        verify(domainConfigService).load(1L);
    }

    @Test
    void updateDomainConfigReturnsView() throws Exception {
        DomainConfigService domainConfigService = mock(DomainConfigService.class);
        when(domainConfigService.update(eq(1L), any())).thenReturn(new DomainConfigService.DomainConfigView(
                1L,
                List.of(new DomainConfigService.ConfigItemView(
                        "ticket.default_priority",
                        "high",
                        "string",
                        "默认优先级",
                        LocalDateTime.parse("2026-05-03T08:00:00")))));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new DomainConfigController(domainConfigService)).build();

        mockMvc.perform(put("/api/v1/admin/domains/1/config")
                        .contentType("application/json")
                        .content("""
                                {
                                  "items": [
                                    {
                                      "key": "ticket.default_priority",
                                      "value": "high"
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].key").value("ticket.default_priority"));

        verify(domainConfigService).update(eq(1L), any());
    }
}
