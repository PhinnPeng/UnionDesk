package com.uniondesk.domain.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.uniondesk.domain.web.DomainCustomerDtos;
import com.uniondesk.domain.web.DomainDtos;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class DomainCustomerServiceTests {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-05-03T12:00:00Z"), ZoneOffset.UTC);

    @Test
    void customerStatusTransitionsThroughPendingActiveDisabledActive() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        DomainService domainService = mock(DomainService.class);
        when(domainService.getDomain(10L)).thenReturn(domainView("open"));
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), any(Object[].class))).thenReturn(0);
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class))).thenReturn(1L, 1L, 1L, 1L);
        when(jdbcTemplate.queryForObject(anyString(), any(org.springframework.jdbc.core.RowMapper.class), any(Object[].class)))
                .thenReturn(customerView(1L, "pending"))
                .thenReturn(customerView(1L, "active"))
                .thenReturn(customerView(1L, "disabled"))
                .thenReturn(customerView(1L, "active"));
        when(jdbcTemplate.update(anyString(), any(Object[].class))).thenReturn(1);

        DomainCustomerService service = new DomainCustomerService(jdbcTemplate, domainService, CLOCK);

        DomainCustomerDtos.DomainCustomerView active = service.updateCustomerStatus(10L, 1L, new DomainCustomerDtos.UpdateDomainCustomerStatusRequest("active"));
        DomainCustomerDtos.DomainCustomerView disabled = service.updateCustomerStatus(10L, 1L, new DomainCustomerDtos.UpdateDomainCustomerStatusRequest("disabled"));
        DomainCustomerDtos.DomainCustomerView reactivated = service.updateCustomerStatus(10L, 1L, new DomainCustomerDtos.UpdateDomainCustomerStatusRequest("active"));

        assertEquals("active", active.status());
        assertEquals("disabled", disabled.status());
        assertEquals("active", reactivated.status());
    }

    private DomainDtos.DomainView domainView(String registrationPolicy) {
        LocalDateTime now = LocalDateTime.parse("2026-05-03T12:00:00");
        return new DomainDtos.DomainView(
                10L,
                "default",
                "Default Domain",
                "logo.png",
                List.of("public"),
                registrationPolicy,
                1,
                now,
                now,
                null);
    }

    private DomainCustomerDtos.DomainCustomerView customerView(long id, String status) {
        LocalDateTime now = LocalDateTime.parse("2026-05-03T12:00:00");
        return new DomainCustomerDtos.DomainCustomerView(
                id,
                10L,
                100L,
                200L,
                "customer",
                "Customer",
                "13800000000",
                "customer@uniondesk.local",
                status,
                "manual",
                now,
                null,
                now,
                now);
    }
}
