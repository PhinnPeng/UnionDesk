package com.uniondesk.config.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.uniondesk.config.core.SystemConfigService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class SystemConfigControllerTests {

    @Test
    void getSystemConfigReturnsView() throws Exception {
        SystemConfigService systemConfigService = mock(SystemConfigService.class);
        when(systemConfigService.load()).thenReturn(new SystemConfigService.SystemConfigView(List.of(
                new SystemConfigService.ConfigItemView(
                        "site.name",
                        "UnionDesk",
                        "string",
                        "站点名称",
                        LocalDateTime.parse("2026-05-03T08:00:00")))));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new SystemConfigController(systemConfigService)).build();

        mockMvc.perform(get("/api/v1/admin/system-config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].key").value("site.name"))
                .andExpect(jsonPath("$.items[0].value").value("UnionDesk"));

        verify(systemConfigService).load();
    }

    @Test
    void updateSystemConfigReturnsView() throws Exception {
        SystemConfigService systemConfigService = mock(SystemConfigService.class);
        when(systemConfigService.update(any()))
                .thenReturn(new SystemConfigService.SystemConfigView(List.of(
                        new SystemConfigService.ConfigItemView(
                                "site.name",
                                "UnionDesk",
                                "string",
                                "站点名称",
                                LocalDateTime.parse("2026-05-03T08:00:00")))));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new SystemConfigController(systemConfigService)).build();

        mockMvc.perform(put("/api/v1/admin/system-config")
                        .contentType("application/json")
                        .content("""
                                {
                                  "items": [
                                    {
                                      "key": "site.name",
                                      "value": "UnionDesk"
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].key").value("site.name"));

        verify(systemConfigService).update(any());
    }
}
