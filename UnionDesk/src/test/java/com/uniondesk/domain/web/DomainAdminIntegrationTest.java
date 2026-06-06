package com.uniondesk.domain.web;

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

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private AuthCaptchaService authCaptchaService;

    @BeforeEach
    void setUp() {
        IntegrationAuthSupport.mockCaptchaBypass(authCaptchaService);
        IntegrationAuthSupport.ensureActiveDefaultDomain(jdbcTemplate);
        IntegrationAuthSupport.ensureSuperAdminDomainControlPermissions(jdbcTemplate);
    }

    @Test
    void getAndUpdateAdminDomainSucceed() throws Exception {
        long domainId = IntegrationAuthSupport.activeDefaultDomainId(jdbcTemplate);
        String accessToken = IntegrationAuthSupport.adminAccessToken(mockMvc, objectMapper);

        mockMvc.perform(get("/api/v1/admin/domains/{domainId}", domainId)
                        .header("X-UD-Client-Code", IntegrationAuthSupport.ADMIN_CLIENT_CODE)
                        .header("Authorization", IntegrationAuthSupport.bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value((int) domainId))
                .andExpect(jsonPath("$.code").exists());

        mockMvc.perform(put("/api/v1/admin/domains/{domainId}", domainId)
                        .header("X-UD-Client-Code", IntegrationAuthSupport.ADMIN_CLIENT_CODE)
                        .header("Authorization", IntegrationAuthSupport.bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Default Domain Updated"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Default Domain Updated"));
    }
}
