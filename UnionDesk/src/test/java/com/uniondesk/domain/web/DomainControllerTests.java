package com.uniondesk.domain.web;

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
import com.uniondesk.domain.core.DomainService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class DomainControllerTests {

    private final DomainService domainService = mock(DomainService.class);

    @Test
    void listReturnsPageResult() throws Exception {
        MockMvc mockMvc = mockMvc();
        when(domainService.listCustomerDomains(1, 20, null)).thenReturn(new PageResult<>(
                1,
                List.of(new DomainDtos.DomainBriefView(1L, "default", "Default Domain", "logo.png"))));

        mockMvc.perform(get("/api/v1/domains"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.list[0].code").value("default"))
                .andExpect(jsonPath("$.list[0].name").value("Default Domain"));
    }

    @Test
    void getReturnsDomainView() throws Exception {
        MockMvc mockMvc = mockMvc();
        when(domainService.getDomain(1L)).thenReturn(domainView(1L));

        mockMvc.perform(get("/api/v1/domains/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("default"))
                .andExpect(jsonPath("$.registration_policy").value("open"));
    }

    @Test
    void createAdminReturnsIdAndCode() throws Exception {
        MockMvc mockMvc = mockMvc();
        when(domainService.createDomain(any())).thenReturn(new DomainDtos.DomainCreateResponse(2L, "new-domain"));

        mockMvc.perform(post("/api/v1/admin/domains")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "new-domain",
                                  "name": "New Domain",
                                  "logo": "logo.png",
                                  "visibility_policy_codes": ["public"],
                                  "registration_policy": "open"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.code").value("new-domain"));
        verify(domainService).createDomain(any());
    }

    @Test
    void updateAdminReturnsDomainView() throws Exception {
        MockMvc mockMvc = mockMvc();
        when(domainService.updateDomain(eq(1L), any())).thenReturn(domainView(1L));

        mockMvc.perform(put("/api/v1/admin/domains/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Updated Domain",
                                  "status": 1
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Default Domain"));
        verify(domainService).updateDomain(eq(1L), any());
    }

    @Test
    void deleteAdminReturnsNoContent() throws Exception {
        MockMvc mockMvc = mockMvc();

        mockMvc.perform(delete("/api/v1/admin/domains/1"))
                .andExpect(status().isNoContent());
        verify(domainService).deleteDomain(1L);
    }

    private MockMvc mockMvc() {
        return MockMvcBuilders.standaloneSetup(new DomainController(domainService)).build();
    }

    private DomainDtos.DomainView domainView(long id) {
        LocalDateTime now = LocalDateTime.of(2026, 5, 3, 12, 0);
        return new DomainDtos.DomainView(
                id,
                "default",
                "Default Domain",
                "logo.png",
                List.of("public"),
                "open",
                1,
                now,
                now,
                null);
    }
}
