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
 * 客服「上线/隐身」状态与接入门控集成测试：
 * presence POST/GET 往返、隐身拒绝接入、恢复上线可接入、已接入会话不受隐身影响。
 * 注意：presence 写入 Redis（非事务），测试间靠 TTL（90s）自然清理。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(FixedClockTestConfiguration.class)
@Transactional
class ConsultationPresenceIntegrationTest extends IntegrationTestSupport {

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
    void presenceRoundtripAndInvisibleBlocksClaim() throws Exception {
        long domainId = defaultDomainId(jdbcTemplate);
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String staffName = "presence_agent_" + suffix;
        String customerName = "presence_customer_" + suffix;

        IntegrationAuthSupport.insertDomainStaff(jdbcTemplate, domainId, staffName, "pass1234", "agent");
        String staffToken = IntegrationAuthSupport.loginAccessToken(
                mockMvc, objectMapper, IntegrationAuthSupport.ADMIN_CLIENT_CODE, staffName, "pass1234");
        String customerToken = IntegrationAuthSupport.registerCustomerAccessToken(
                mockMvc, objectMapper, jdbcTemplate, domainId, customerName, "pass1234").accessToken();

        // 上线（手动）上报 + 只读查询往返
        mockMvc.perform(post("/api/v1/admin/domains/{domainId}/consultations/agent/presence", domainId)
                        .header("Authorization", IntegrationAuthSupport.bearer(staffToken))
                        .header("X-UD-Client-Code", IntegrationAuthSupport.ADMIN_CLIENT_CODE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"online\",\"mode\":\"manual\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("online"))
                .andExpect(jsonPath("$.data.mode").value("manual"));

        mockMvc.perform(get("/api/v1/admin/domains/{domainId}/consultations/agent/presence", domainId)
                        .header("Authorization", IntegrationAuthSupport.bearer(staffToken))
                        .header("X-UD-Client-Code", IntegrationAuthSupport.ADMIN_CLIENT_CODE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("online"))
                .andExpect(jsonPath("$.data.mode").value("manual"));

        // 客户发起两个会话
        String sessionNoA = createConsultation(customerToken, domainId, "隐身门控测试会话A");
        String sessionNoB = createConsultation(customerToken, domainId, "隐身门控测试会话B");

        // 上线可接入（会话 A）
        mockMvc.perform(post("/api/v1/admin/domains/{domainId}/consultations/{sessionNo}/claim", domainId, sessionNoA)
                        .header("Authorization", IntegrationAuthSupport.bearer(staffToken))
                        .header("X-UD-Client-Code", IntegrationAuthSupport.ADMIN_CLIENT_CODE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sessionNo").value(sessionNoA));

        // 切隐身：查询确认 + 接入会话 B 被拒
        mockMvc.perform(post("/api/v1/admin/domains/{domainId}/consultations/agent/presence", domainId)
                        .header("Authorization", IntegrationAuthSupport.bearer(staffToken))
                        .header("X-UD-Client-Code", IntegrationAuthSupport.ADMIN_CLIENT_CODE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"invisible\",\"mode\":\"manual\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("invisible"));

        mockMvc.perform(get("/api/v1/admin/domains/{domainId}/consultations/agent/presence", domainId)
                        .header("Authorization", IntegrationAuthSupport.bearer(staffToken))
                        .header("X-UD-Client-Code", IntegrationAuthSupport.ADMIN_CLIENT_CODE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("invisible"));

        mockMvc.perform(post("/api/v1/admin/domains/{domainId}/consultations/{sessionNo}/claim", domainId, sessionNoB)
                        .header("Authorization", IntegrationAuthSupport.bearer(staffToken))
                        .header("X-UD-Client-Code", IntegrationAuthSupport.ADMIN_CLIENT_CODE))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("隐身状态无法接入会话，请先上线"));

        // 恢复上线：可再次接入（会话 B）
        mockMvc.perform(post("/api/v1/admin/domains/{domainId}/consultations/agent/presence", domainId)
                        .header("Authorization", IntegrationAuthSupport.bearer(staffToken))
                        .header("X-UD-Client-Code", IntegrationAuthSupport.ADMIN_CLIENT_CODE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"online\",\"mode\":\"manual\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("online"));

        mockMvc.perform(post("/api/v1/admin/domains/{domainId}/consultations/{sessionNo}/claim", domainId, sessionNoB)
                        .header("Authorization", IntegrationAuthSupport.bearer(staffToken))
                        .header("X-UD-Client-Code", IntegrationAuthSupport.ADMIN_CLIENT_CODE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sessionNo").value(sessionNoB));

        // 状态参数非法拒绝
        mockMvc.perform(post("/api/v1/admin/domains/{domainId}/consultations/agent/presence", domainId)
                        .header("Authorization", IntegrationAuthSupport.bearer(staffToken))
                        .header("X-UD-Client-Code", IntegrationAuthSupport.ADMIN_CLIENT_CODE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ghost\",\"mode\":\"manual\"}"))
                .andExpect(status().isBadRequest());
    }

    private String createConsultation(String customerToken, long domainId, String content) throws Exception {
        String body = mockMvc.perform(post("/api/v1/domains/{domainId}/consultations", domainId)
                        .header("Authorization", IntegrationAuthSupport.bearer(customerToken))
                        .header("X-UD-Client-Code", IntegrationAuthSupport.CUSTOMER_CLIENT_CODE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"" + content + "\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode json = objectMapper.readTree(body);
        String sessionNo = json.path("data").get("sessionNo").asText();
        assertThat(sessionNo).isNotBlank();
        return sessionNo;
    }
}
