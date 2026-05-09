package com.uniondesk.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uniondesk.auth.core.UserContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.jdbc.core.JdbcTemplate;

public abstract class IntegrationTestSupport {

    protected long defaultDomainId(JdbcTemplate jdbcTemplate) {
        Long domainId = jdbcTemplate.queryForObject(
                "SELECT id FROM business_domain WHERE code = 'default' LIMIT 1",
                Long.class);
        if (domainId == null) {
            throw new IllegalStateException("default business domain not found");
        }
        return domainId;
    }

    protected long defaultTicketTypeId(JdbcTemplate jdbcTemplate, long domainId) {
        Long ticketTypeId = jdbcTemplate.queryForObject(
                """
                        SELECT id
                        FROM ticket_type
                        WHERE business_domain_id = ? AND code = 'general'
                        LIMIT 1
                        """,
                Long.class,
                domainId);
        if (ticketTypeId == null) {
            throw new IllegalStateException("default ticket type not found");
        }
        return ticketTypeId;
    }

    protected long defaultPriorityLevelId(JdbcTemplate jdbcTemplate, long domainId) {
        Long priorityLevelId = jdbcTemplate.queryForObject(
                """
                        SELECT id
                        FROM ticket_priority_level
                        WHERE business_domain_id = ? AND code = 'normal'
                        LIMIT 1
                        """,
                Long.class,
                domainId);
        if (priorityLevelId == null) {
            throw new IllegalStateException("default priority level not found");
        }
        return priorityLevelId;
    }

    protected UserContext adminContext() {
        return new UserContext(1L, "super_admin", null, "sid-admin", "ud-admin-web");
    }

    protected UserContext customerContext(long businessDomainId) {
        return new UserContext(1L, "customer", businessDomainId, "sid-customer", "ud-customer-web");
    }

    protected UserContext agentContext(long businessDomainId) {
        return new UserContext(2L, "agent", businessDomainId, "sid-agent", "ud-admin-web");
    }

    protected String adminAccessToken(MockMvc mockMvc, ObjectMapper objectMapper) throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/login")
                        .header("X-UD-Client-Code", "ud-admin-web")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "admin",
                                  "password": "admin123",
                                  "captcha_token": "test-captcha-token"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode jsonNode = objectMapper.readTree(response);
        return jsonNode.get("accessToken").asText();
    }
}
