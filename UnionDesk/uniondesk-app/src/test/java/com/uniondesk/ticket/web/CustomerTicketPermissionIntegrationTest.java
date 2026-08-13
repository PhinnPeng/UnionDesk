package com.uniondesk.ticket.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uniondesk.auth.core.AuthCaptchaService;
import com.uniondesk.auth.core.LoginAuditService;
import com.uniondesk.support.FixedClockTestConfiguration;
import com.uniondesk.support.IntegrationAuthSupport;
import com.uniondesk.support.IntegrationTestSupport;
import com.uniondesk.ticket.core.TicketService;
import java.util.List;
import java.util.Map;
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
 * 客户授权链集成测试（08-13-customer-permission-chain）：
 * 客户提单/我的工单/回复/撤回全通；跨用户详情/回复 403；未入域提单 403；admin 端点不回退。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(FixedClockTestConfiguration.class)
@Transactional
class CustomerTicketPermissionIntegrationTest extends IntegrationTestSupport {

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
    void customerCanCreateListDetailReplyAndWithdrawOwnTicket() throws Exception {
        long domainId = defaultDomainId(jdbcTemplate);
        long ticketTypeId = defaultTicketTypeId(jdbcTemplate, domainId);
        String customerToken = registerCustomer(domainId, "perm_customer_full");

        long ticketId = createTicket(customerToken, domainId, ticketTypeId, "权限链路工单", "请处理");

        mockMvc.perform(get("/api/v1/domains/{domainId}/tickets/my", domainId)
                        .header("Authorization", IntegrationAuthSupport.bearer(customerToken))
                        .header("X-UD-Client-Code", IntegrationAuthSupport.CUSTOMER_CLIENT_CODE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].id").value(ticketId));

        mockMvc.perform(get("/api/v1/domains/{domainId}/tickets/my/{ticketId}", domainId, ticketId)
                        .header("Authorization", IntegrationAuthSupport.bearer(customerToken))
                        .header("X-UD-Client-Code", IntegrationAuthSupport.CUSTOMER_CLIENT_CODE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ticket.id").value(ticketId))
                .andExpect(jsonPath("$.data.ticket.customerId").exists());

        mockMvc.perform(post("/api/v1/domains/{domainId}/tickets/my/{ticketId}/replies", domainId, ticketId)
                        .header("Authorization", IntegrationAuthSupport.bearer(customerToken))
                        .header("X-UD-Client-Code", IntegrationAuthSupport.CUSTOMER_CLIENT_CODE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TicketService.ReplyTicketCommand(
                                ticketVersion(ticketId), "补充说明", null, List.of()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").exists());

        mockMvc.perform(post("/api/v1/domains/{domainId}/tickets/my/{ticketId}/withdraw", domainId, ticketId)
                        .header("Authorization", IntegrationAuthSupport.bearer(customerToken))
                        .header("X-UD-Client-Code", IntegrationAuthSupport.CUSTOMER_CLIENT_CODE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TicketService.WithdrawTicketCommand(
                                ticketVersion(ticketId), "无需处理"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(ticketId));
    }

    @Test
    void customerCannotViewOrReplyOtherCustomersTicket() throws Exception {
        long domainId = defaultDomainId(jdbcTemplate);
        long ticketTypeId = defaultTicketTypeId(jdbcTemplate, domainId);
        String customerAToken = registerCustomer(domainId, "perm_customer_a");
        String customerBToken = registerCustomer(domainId, "perm_customer_b");

        long ticketId = createTicket(customerAToken, domainId, ticketTypeId, "A 的工单", "仅 A 可见");

        mockMvc.perform(get("/api/v1/domains/{domainId}/tickets/my/{ticketId}", domainId, ticketId)
                        .header("Authorization", IntegrationAuthSupport.bearer(customerBToken))
                        .header("X-UD-Client-Code", IntegrationAuthSupport.CUSTOMER_CLIENT_CODE))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("40301"))
                .andExpect(jsonPath("$.message").value("无操作权限"));

        mockMvc.perform(post("/api/v1/domains/{domainId}/tickets/my/{ticketId}/replies", domainId, ticketId)
                        .header("Authorization", IntegrationAuthSupport.bearer(customerBToken))
                        .header("X-UD-Client-Code", IntegrationAuthSupport.CUSTOMER_CLIENT_CODE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TicketService.ReplyTicketCommand(
                                ticketVersion(ticketId), "越权回复", null, List.of()))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("40301"))
                .andExpect(jsonPath("$.message").value("无操作权限"));
    }

    @Test
    void customerNotInDomainCannotCreateTicket() throws Exception {
        long domainId = defaultDomainId(jdbcTemplate);
        long otherDomainId = insertDomain("perm-other-" + uniqueSuffix());
        String customerToken = registerCustomer(domainId, "perm_customer_outsider");

        mockMvc.perform(post("/api/v1/domains/{domainId}/tickets", otherDomainId)
                        .header("Authorization", IntegrationAuthSupport.bearer(customerToken))
                        .header("X-UD-Client-Code", IntegrationAuthSupport.CUSTOMER_CLIENT_CODE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TicketService.CreateTicketCommand(
                                0L, "越域提单", "不应成功", Map.of(), List.of(), null, null, "web", null, List.of()))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("40301"))
                .andExpect(jsonPath("$.message").value("无操作权限"));
    }

    @Test
    void customerCannotAccessAdminTicketEndpoints() throws Exception {
        long domainId = defaultDomainId(jdbcTemplate);
        String customerToken = registerCustomer(domainId, "perm_customer_admin");

        mockMvc.perform(get("/api/v1/admin/domains/{domainId}/tickets", domainId)
                        .header("Authorization", IntegrationAuthSupport.bearer(customerToken))
                        .header("X-UD-Client-Code", IntegrationAuthSupport.CUSTOMER_CLIENT_CODE))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("40301"))
                .andExpect(jsonPath("$.message").value("无操作权限"));
    }

    private String registerCustomer(long domainId, String usernamePrefix) throws Exception {
        return IntegrationAuthSupport.registerCustomerAccessToken(
                mockMvc,
                objectMapper,
                jdbcTemplate,
                domainId,
                usernamePrefix + "_" + uniqueSuffix(),
                "customer123").accessToken();
    }

    private long createTicket(String customerToken, long domainId, long ticketTypeId, String title, String description) throws Exception {
        String response = mockMvc.perform(post("/api/v1/domains/{domainId}/tickets", domainId)
                        .header("Authorization", IntegrationAuthSupport.bearer(customerToken))
                        .header("X-UD-Client-Code", IntegrationAuthSupport.CUSTOMER_CLIENT_CODE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TicketService.CreateTicketCommand(ticketTypeId,
                                title,
                                description,
                                Map.of("channel", "web"),
                                List.of(),
                                null,
                                null,
                                "web", null, List.of()))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).path("data").get("id").asLong();
    }

    private long ticketVersion(long ticketId) {
        Long version = jdbcTemplate.queryForObject("SELECT version FROM ticket WHERE id = ?", Long.class, ticketId);
        assertThat(version).as("ticket version should exist").isNotNull();
        return version;
    }

    private long insertDomain(String code) {
        jdbcTemplate.update("""
                INSERT INTO business_domain (code, name, description, visibility_policy, status, created_at, updated_at, visibility_policy_codes)
                VALUES (?, ?, '测试业务域', 'public', 1, CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3), '["public"]')
                """, code, code);
        Long id = jdbcTemplate.queryForObject(
                "SELECT id FROM business_domain WHERE code = ? LIMIT 1", Long.class, code);
        return id;
    }

    private String uniqueSuffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
