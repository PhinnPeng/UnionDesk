package com.uniondesk.auth.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uniondesk.auth.core.AuthCaptchaService;
import com.uniondesk.auth.core.LoginConfigService;
import com.uniondesk.support.IntegrationAuthSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Auth Controller 集成测试
 * 验收用例：
 * - TC-004: 客户端与员工端分别登录
 * - TC-006: Token 策略
 * - TC-007: 敏感操作二次验证
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthCaptchaService authCaptchaService;

    @Autowired
    private LoginConfigService loginConfigService;

    @BeforeEach
    void setUp() {
        IntegrationAuthSupport.mockCaptchaBypass(authCaptchaService);
    }

    @Test
    void testLoginConfigWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/auth/login-config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.captchaEnabled").exists());
    }

    @Test
    void testLoginWithPortalType() throws Exception {
        // TC-004: 客户端与员工端分别登录
        String loginRequest = """
            {
                "username": "admin",
                "password": "admin123",
                "portal_type": "staff",
                "captcha_token": "test-captcha-token"
            }
            """;

        mockMvc.perform(post("/api/v1/auth/login")
                .header("X-UD-Client-Code", "ud-admin-web")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.portalType").value("staff"))
                .andExpect(jsonPath("$.subjectId").exists());

        String loginResponse = mockMvc.perform(post("/api/v1/auth/login")
                .header("X-UD-Client-Code", "ud-admin-web")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginRequest))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode loginJson = objectMapper.readTree(loginResponse);
        String accessToken = loginJson.get("accessToken").asText();

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("X-UD-Client-Code", "ud-admin-web")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.platformRoles").isArray())
                .andExpect(jsonPath("$.platformRoles[0]").value("platform_admin"));
    }

    @Test
    void testRefreshToken() throws Exception {
        // TC-006: Token 策略 - 刷新 Token
        // 首先登录获取 refresh_token
        String loginRequest = """
            {
                "username": "admin",
                "password": "admin123",
                "captcha_token": "test-captcha-token"
            }
            """;

        String loginResponse = mockMvc.perform(post("/api/v1/auth/login")
                .header("X-UD-Client-Code", "ud-admin-web")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginRequest))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // 解析 JSON 提取 refreshToken（驼峰命名）
        JsonNode jsonNode = objectMapper.readTree(loginResponse);
        String refreshToken = jsonNode.get("refreshToken").asText();

        // 使用 refreshToken 刷新（使用驼峰命名）
        String refreshRequest = String.format("""
            {
                "refreshToken": "%s"
            }
            """, refreshToken);

        mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(refreshRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    void testLoginWithCaptchaFailed() throws Exception {
        // 负向测试：验证码失败场景
        // Mock captcha service 抛出异常
        doThrow(new com.uniondesk.auth.core.AuthCaptchaException("验证码已失效"))
                .when(authCaptchaService).consumeToken(anyString());

        String loginRequest = """
            {
                "username": "admin",
                "password": "admin123",
                "captcha_token": "invalid-captcha-token"
            }
            """;

        mockMvc.perform(post("/api/v1/auth/login")
                .header("X-UD-Client-Code", "ud-admin-web")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("10003"))
                .andExpect(jsonPath("$.message").value("验证码校验失败"));
    }

    @Test
    void testStepUpAuthentication() throws Exception {
        // TC-007: 敏感操作二次验证
        String stepUpRequest = """
            {
                "password": "admin123",
                "operation_code": "delete_domain"
            }
            """;

        // 注意：这个测试需要先登录获取 access_token
        // TODO: 实现完整的测试场景
    }

    @Test
    void testGetCurrentUser() throws Exception {
        // 测试 /api/v1/auth/me 接口
        // 注意：需要先登录获取 access_token
        // TODO: 实现完整的测试场景
    }

    @Test
    void testLoginConfigWithoutTokenReturnsOk() throws Exception {
        mockMvc.perform(get("/api/v1/auth/login-config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.captchaEnabled").exists());
    }
}
