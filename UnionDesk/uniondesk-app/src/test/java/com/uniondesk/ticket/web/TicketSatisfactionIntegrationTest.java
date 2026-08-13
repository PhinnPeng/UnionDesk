package com.uniondesk.ticket.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
 * 工单满意度评价链路集成测试（S4-6 satisfaction-survey）：
 * 未关闭不可评（400）→ agent 关闭 → 本人评价成功 → 重复评价 400 → 跨用户 403。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(FixedClockTestConfiguration.class)
@Transactional
class TicketSatisfactionIntegrationTest extends IntegrationTestSupport {

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
    void customerCannotRateOpenTicketThenCanRateClosedTicketOnce() throws Exception {
        long domainId = defaultDomainId(jdbcTemplate);
        long ticketTypeId = defaultTicketTypeId(jdbcTemplate, domainId);
        String customerToken = registerCustomer(domainId, "sat_customer");
        String agentToken = staffToken("sat_agent", "agent123", "agent");

        long ticketId = createTicket(customerToken, domainId, ticketTypeId, "满意度链路工单", "请处理");

        // 未关闭不可评价
        mockMvc.perform(post("/api/v1/domains/{domainId}/tickets/my/{ticketId}/satisfaction", domainId, ticketId)
                        .header("Authorization", IntegrationAuthSupport.bearer(customerToken))
                        .header("X-UD-Client-Code", IntegrationAuthSupport.CUSTOMER_CLIENT_CODE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"rating": 5, "comment": "处理及时"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("工单未关闭不可评价"));

        // 关闭工单（open → claim(processing) → closed）
        mockMvc.perform(post("/api/v1/admin/domains/{domainId}/tickets/{ticketId}/claim", domainId, ticketId)
                        .header("Authorization", IntegrationAuthSupport.bearer(agentToken))
                        .header("X-UD-Client-Code", IntegrationAuthSupport.ADMIN_CLIENT_CODE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TicketService.ClaimTicketCommand(1L))))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/api/v1/admin/domains/{domainId}/tickets/{ticketId}/status", domainId, ticketId)
                        .header("Authorization", IntegrationAuthSupport.bearer(agentToken))
                        .header("X-UD-Client-Code", IntegrationAuthSupport.ADMIN_CLIENT_CODE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TicketService.ChangeTicketStatusCommand(
                                "closed",
                                ticketVersion(ticketId),
                                null,
                                "已关闭"))))
                .andExpect(status().isOk());

        // 关闭后未评价 → GET 返回 null
        mockMvc.perform(get("/api/v1/domains/{domainId}/tickets/my/{ticketId}/satisfaction", domainId, ticketId)
                        .header("Authorization", IntegrationAuthSupport.bearer(customerToken))
                        .header("X-UD-Client-Code", IntegrationAuthSupport.CUSTOMER_CLIENT_CODE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.nullValue()));

        // 本人评价成功
        mockMvc.perform(post("/api/v1/domains/{domainId}/tickets/my/{ticketId}/satisfaction", domainId, ticketId)
                        .header("Authorization", IntegrationAuthSupport.bearer(customerToken))
                        .header("X-UD-Client-Code", IntegrationAuthSupport.CUSTOMER_CLIENT_CODE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"rating": 5, "comment": "处理及时，五星好评"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").exists());

        // 重复评价拒绝
        mockMvc.perform(post("/api/v1/domains/{domainId}/tickets/my/{ticketId}/satisfaction", domainId, ticketId)
                        .header("Authorization", IntegrationAuthSupport.bearer(customerToken))
                        .header("X-UD-Client-Code", IntegrationAuthSupport.CUSTOMER_CLIENT_CODE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"rating": 1, "comment": "重复提交"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("该工单已评价，请勿重复提交"));

        // 评价后可查
        mockMvc.perform(get("/api/v1/domains/{domainId}/tickets/my/{ticketId}/satisfaction", domainId, ticketId)
                        .header("Authorization", IntegrationAuthSupport.bearer(customerToken))
                        .header("X-UD-Client-Code", IntegrationAuthSupport.CUSTOMER_CLIENT_CODE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rating").value(5))
                .andExpect(jsonPath("$.data.comment").value("处理及时，五星好评"))
                .andExpect(jsonPath("$.data.status").value("submitted"));
    }

    @Test
    void customerCannotRateOtherCustomersTicket() throws Exception {
        long domainId = defaultDomainId(jdbcTemplate);
        long ticketTypeId = defaultTicketTypeId(jdbcTemplate, domainId);
        String customerAToken = registerCustomer(domainId, "sat_customer_a");
        String customerBToken = registerCustomer(domainId, "sat_customer_b");
        String agentToken = staffToken("sat_agent_b", "agent123", "agent");

        long ticketId = createTicket(customerAToken, domainId, ticketTypeId, "A 的满意度工单", "仅 A 可评");

        mockMvc.perform(post("/api/v1/admin/domains/{domainId}/tickets/{ticketId}/claim", domainId, ticketId)
                        .header("Authorization", IntegrationAuthSupport.bearer(agentToken))
                        .header("X-UD-Client-Code", IntegrationAuthSupport.ADMIN_CLIENT_CODE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TicketService.ClaimTicketCommand(1L))))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/api/v1/admin/domains/{domainId}/tickets/{ticketId}/status", domainId, ticketId)
                        .header("Authorization", IntegrationAuthSupport.bearer(agentToken))
                        .header("X-UD-Client-Code", IntegrationAuthSupport.ADMIN_CLIENT_CODE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TicketService.ChangeTicketStatusCommand(
                                "closed",
                                ticketVersion(ticketId),
                                null,
                                "已关闭"))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/domains/{domainId}/tickets/my/{ticketId}/satisfaction", domainId, ticketId)
                        .header("Authorization", IntegrationAuthSupport.bearer(customerBToken))
                        .header("X-UD-Client-Code", IntegrationAuthSupport.CUSTOMER_CLIENT_CODE))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("40301"))
                .andExpect(jsonPath("$.message").value("无操作权限"));

        mockMvc.perform(post("/api/v1/domains/{domainId}/tickets/my/{ticketId}/satisfaction", domainId, ticketId)
                        .header("Authorization", IntegrationAuthSupport.bearer(customerBToken))
                        .header("X-UD-Client-Code", IntegrationAuthSupport.CUSTOMER_CLIENT_CODE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"rating": 4, "comment": "越权评价"}
                                """))
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

    private String staffToken(String loginName, String password, String roleCode) throws Exception {
        long domainId = defaultDomainId(jdbcTemplate);
        IntegrationAuthSupport.insertDomainStaff(jdbcTemplate, domainId, loginName, password, roleCode);
        return IntegrationAuthSupport.loginAccessToken(
                mockMvc, objectMapper, IntegrationAuthSupport.ADMIN_CLIENT_CODE, loginName, password);
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
        return version == null ? 0L : version;
    }

    private String uniqueSuffix() {
        return Long.toHexString(System.nanoTime());
    }
}
