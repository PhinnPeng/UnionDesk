package com.uniondesk.ticket.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uniondesk.auth.core.AuthCaptchaService;
import com.uniondesk.auth.core.LoginAuditService;
import com.uniondesk.support.FixedClockTestConfiguration;
import com.uniondesk.support.IntegrationAuthSupport;
import com.uniondesk.support.IntegrationTestSupport;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * 内部备注（internal_note）集成测试：
 * 员工可写内部备注；客户详情仅返回公开回复（内部备注与 history 均不下发）。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(FixedClockTestConfiguration.class)
@Transactional
class TicketInternalNoteIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private AuthCaptchaService authCaptchaService;

    @SpyBean
    private LoginAuditService loginAuditService;

    @BeforeEach
    void setUp() {
        IntegrationAuthSupport.mockCaptchaBypass(authCaptchaService);
        doNothing().when(loginAuditService).record(any());
    }

    @Test
    void internalNoteHiddenFromCustomerDetail() throws Exception {
        long domainId = defaultDomainId(jdbcTemplate);
        long ticketTypeId = defaultTicketTypeId(jdbcTemplate, domainId);
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String customerName = "note_customer_" + suffix;
        String staffName = "note_agent_" + suffix;

        String customerToken = IntegrationAuthSupport.registerCustomerAccessToken(
                mockMvc, objectMapper, jdbcTemplate, domainId, customerName, "pass1234").accessToken();
        IntegrationAuthSupport.insertDomainStaff(jdbcTemplate, domainId, staffName, "pass1234", "agent");
        String staffToken = IntegrationAuthSupport.loginAccessToken(
                mockMvc, objectMapper, IntegrationAuthSupport.ADMIN_CLIENT_CODE, staffName, "pass1234");

        long ticketId = createTicket(customerToken, domainId, ticketTypeId, "内部备注测试", "客户提单内容");
        long version = 1L;

        // 员工公开回复
        mockMvc.perform(post("/api/v1/admin/domains/{domainId}/tickets/{ticketId}/replies", domainId, ticketId)
                        .header("Authorization", IntegrationAuthSupport.bearer(staffToken))
                        .header("X-UD-Client-Code", IntegrationAuthSupport.ADMIN_CLIENT_CODE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new com.uniondesk.ticket.core.TicketService.ReplyTicketCommand(
                                        version, "公开回复内容", null, java.util.List.of(), false))))
                .andExpect(status().isOk());

        // 员工内部备注（internal=true）
        mockMvc.perform(post("/api/v1/admin/domains/{domainId}/tickets/{ticketId}/replies", domainId, ticketId)
                        .header("Authorization", IntegrationAuthSupport.bearer(staffToken))
                        .header("X-UD-Client-Code", IntegrationAuthSupport.ADMIN_CLIENT_CODE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new com.uniondesk.ticket.core.TicketService.ReplyTicketCommand(
                                        version + 1, "内部备注内容-客户不可见", null, java.util.List.of(), true))))
                .andExpect(status().isOk());

        // 员工详情：replies 含公开回复与内部备注
        mockMvc.perform(get("/api/v1/admin/domains/{domainId}/tickets/{ticketId}", domainId, ticketId)
                        .header("Authorization", IntegrationAuthSupport.bearer(staffToken))
                        .header("X-UD-Client-Code", IntegrationAuthSupport.ADMIN_CLIENT_CODE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.replies.length()").value(2));

        // 客户详情：仅公开回复（内部备注与 history 不下发）
        mockMvc.perform(get("/api/v1/domains/{domainId}/tickets/my/{ticketId}", domainId, ticketId)
                        .header("Authorization", IntegrationAuthSupport.bearer(customerToken))
                        .header("X-UD-Client-Code", IntegrationAuthSupport.CUSTOMER_CLIENT_CODE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.replies.length()").value(1))
                .andExpect(jsonPath("$.data.replies[0].replyType").value("text"))
                .andExpect(jsonPath("$.data.history.length()").value(0));
    }

    private long createTicket(String customerToken, long domainId, long ticketTypeId, String title, String description)
            throws Exception {
        String body = mockMvc.perform(post("/api/v1/domains/{domainId}/tickets", domainId)
                        .header("Authorization", IntegrationAuthSupport.bearer(customerToken))
                        .header("X-UD-Client-Code", IntegrationAuthSupport.CUSTOMER_CLIENT_CODE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ticketTypeId": %d,
                                  "title": "%s",
                                  "description": "%s",
                                  "attributes": {}
                                }
                                """.formatted(ticketTypeId, title, description)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode json = objectMapper.readTree(body);
        long ticketId = json.path("data").get("id").asLong();
        assertThat(ticketId).isPositive();
        return ticketId;
    }
}
