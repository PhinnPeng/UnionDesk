package com.uniondesk.domain.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.uniondesk.common.web.PageResult;
import com.uniondesk.domain.core.DomainCustomerService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class DomainCustomerControllerTests {

    private final DomainCustomerService domainCustomerService = mock(DomainCustomerService.class);

    @Test
    void listCustomersReturnsPageResult() throws Exception {
        MockMvc mockMvc = mockMvc();
        when(domainCustomerService.listCustomers(10L, 1, 20, null, null)).thenReturn(new PageResult<>(
                1,
                List.of(customerView(1L, "pending"))));

        mockMvc.perform(get("/api/v1/admin/domains/10/customers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.list[0].status").value("pending"))
                .andExpect(jsonPath("$.list[0].customerAccountId").value(100));
    }

    @Test
    void addCustomerReturnsView() throws Exception {
        MockMvc mockMvc = mockMvc();
        when(domainCustomerService.addCustomer(anyLong(), any())).thenReturn(customerView(2L, "pending"));

        mockMvc.perform(post("/api/v1/admin/domains/10/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerAccountId": 100,
                                  "source": "manual"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.status").value("pending"));
        verify(domainCustomerService).addCustomer(anyLong(), any());
    }

    @Test
    void updateCustomerStatusReturnsView() throws Exception {
        MockMvc mockMvc = mockMvc();
        when(domainCustomerService.updateCustomerStatus(eq(10L), eq(1L), any()))
                .thenReturn(customerView(1L, "active"));

        mockMvc.perform(patch("/api/v1/admin/domains/10/customers/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "active"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("active"));
        verify(domainCustomerService).updateCustomerStatus(eq(10L), eq(1L), any());
    }

    private MockMvc mockMvc() {
        return MockMvcBuilders.standaloneSetup(new DomainCustomerController(domainCustomerService)).build();
    }

    private DomainCustomerDtos.DomainCustomerView customerView(long id, String status) {
        LocalDateTime now = LocalDateTime.parse("2026-05-03T12:00:00");
        return new DomainCustomerDtos.DomainCustomerView(
                id,
                10L,
                100L,
                200L,
                "customer",
                "Customer",
                "13800000000",
                "customer@uniondesk.local",
                status,
                "manual",
                now,
                null,
                now,
                now);
    }
}
