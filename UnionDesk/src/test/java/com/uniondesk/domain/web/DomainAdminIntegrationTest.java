package com.uniondesk.domain.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uniondesk.auth.core.AuthCaptchaService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class DomainAdminIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthCaptchaService authCaptchaService;

    @BeforeEach
    void setUp() {
        doNothing().when(authCaptchaService).consumeToken(anyString());
    }

    @Test
    void getAndUpdateAdminDomainSucceed() throws Exception {
        String accessToken = loginAsAdmin();

        mockMvc.perform(get("/api/v1/admin/domains/1")
                        .header("X-UD-Client-Code", "ud-admin-web")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.code").exists());

        mockMvc.perform(put("/api/v1/admin/domains/1")
                        .header("X-UD-Client-Code", "ud-admin-web")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Default Domain Updated"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Default Domain Updated"));
    }

    private String loginAsAdmin() throws Exception {
        String loginRequest = """
            {
                "username": "admin",
                "password": "admin123",
                "portal_type": "staff",
                "captcha_token": "test-captcha-token"
            }
            """;

        String loginResponse = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/auth/login")
                        .header("X-UD-Client-Code", "ud-admin-web")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequest))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode loginJson = objectMapper.readTree(loginResponse);
        return loginJson.get("accessToken").asText();
    }
}
