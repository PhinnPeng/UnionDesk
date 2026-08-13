package com.uniondesk.iam.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uniondesk.auth.core.AuthCaptchaService;
import com.uniondesk.support.IntegrationAuthSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * P0-③ 平台角色绑定 step-up 真实校验集成测试。
 *
 * <p>验证 {@code PUT /api/v1/admin/staff/{staffId}/platform-roles} 对
 * {@code X-UD-Step-Up-Token} 的校验：
 * <ul>
 *   <li>缺失头 → 403</li>
 *   <li>无效 token → 403</li>
 *   <li>有效 token（先调 {@code POST /api/v1/auth/step-up} 签发）→ 200</li>
 *   <li>过期 token（库内构造已过期 step_up 会话）→ 403</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class StepUpTokenValidationTest {

    private static final String ADMIN_PASSWORD = "admin123";
    private static final String EXPIRED_STEP_UP_TOKEN = "expired-step-up-token-it";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private AuthCaptchaService authCaptchaService;

    @BeforeEach
    void setUp() {
        IntegrationAuthSupport.mockCaptchaBypass(authCaptchaService);
    }

    @Test
    void missingStepUpTokenHeaderForbidden() throws Exception {
        mockMvc.perform(updatePlatformRolesRequest(null))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("40301"))
                .andExpect(jsonPath("$.message").value("无操作权限"));
    }

    @Test
    void invalidStepUpTokenForbidden() throws Exception {
        mockMvc.perform(updatePlatformRolesRequest("invalid-step-up-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("40301"))
                .andExpect(jsonPath("$.message").value("无操作权限"));
    }

    @Test
    void expiredStepUpTokenForbidden() throws Exception {
        insertExpiredStepUpSession();
        mockMvc.perform(updatePlatformRolesRequest(EXPIRED_STEP_UP_TOKEN))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("40301"));
    }

    @Test
    void validStepUpTokenAllowsPlatformRoleBinding() throws Exception {
        String stepUpToken = obtainStepUpToken(adminAccessToken());
        mockMvc.perform(updatePlatformRolesRequest(stepUpToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"));
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder updatePlatformRolesRequest(
            String stepUpToken) throws Exception {
        var builder = put("/api/v1/admin/staff/2/platform-roles")
                .header("X-UD-Client-Code", IntegrationAuthSupport.ADMIN_CLIENT_CODE)
                .header("Authorization", IntegrationAuthSupport.bearer(adminAccessToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "roleCodes": ["platform_admin"]
                        }
                        """);
        if (stepUpToken != null) {
            builder.header("X-UD-Step-Up-Token", stepUpToken);
        }
        return builder;
    }

    private String adminAccessToken() throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/login")
                        .header("X-UD-Client-Code", IntegrationAuthSupport.ADMIN_CLIENT_CODE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "admin",
                                  "password": "%s",
                                  "captcha_token": "%s"
                                }
                                """.formatted(ADMIN_PASSWORD, IntegrationAuthSupport.TEST_CAPTCHA_TOKEN)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).path("data").path("accessToken").asText();
    }

    private String obtainStepUpToken(String accessToken) throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/step-up")
                        .header("X-UD-Client-Code", IntegrationAuthSupport.ADMIN_CLIENT_CODE)
                        .header("Authorization", IntegrationAuthSupport.bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "password": "%s"
                                }
                                """.formatted(ADMIN_PASSWORD)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).path("data").path("stepUpToken").asText();
    }

    private void insertExpiredStepUpSession() {
        jdbcTemplate.update("""
                        INSERT INTO auth_login_session
                            (sid, session_type, user_id, client_code, account_type, role_code,
                             login_identifier_masked, session_status, issued_at, expires_at)
                        VALUES (?, 'step_up', 2, 'ud-admin-web', 'staff', 'super_admin', 'ad***', 'active',
                                DATE_SUB(NOW(3), INTERVAL 2 HOUR), DATE_SUB(NOW(3), INTERVAL 1 HOUR))
                        """,
                EXPIRED_STEP_UP_TOKEN);
    }
}
