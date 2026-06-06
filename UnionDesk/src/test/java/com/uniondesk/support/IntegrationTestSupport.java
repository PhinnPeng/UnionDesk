package com.uniondesk.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uniondesk.auth.core.UserContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

public abstract class IntegrationTestSupport {

    protected long defaultDomainId(JdbcTemplate jdbcTemplate) {
        return IntegrationAuthSupport.activeDefaultDomainId(jdbcTemplate);
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
        return IntegrationAuthSupport.adminAccessToken(mockMvc, objectMapper);
    }
}
