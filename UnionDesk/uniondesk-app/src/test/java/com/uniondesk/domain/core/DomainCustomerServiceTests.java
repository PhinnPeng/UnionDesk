package com.uniondesk.domain.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.uniondesk.domain.entity.DomainCustomerPo;
import com.uniondesk.domain.repository.DomainCustomerRepository;
import com.uniondesk.domain.web.DomainCustomerDtos;
import com.uniondesk.domain.web.DomainDtos;
import com.uniondesk.iam.core.CustomerAccountService;
import com.uniondesk.iam.core.IdentitySubjectService;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

class DomainCustomerServiceTests {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-05-03T12:00:00Z"), ZoneOffset.UTC);
    private static final LocalDateTime NOW = LocalDateTime.parse("2026-05-03T12:00:00");

    @Test
    void customerStatusTransitionsThroughPendingActiveDisabledActive() {
        DomainCustomerRepository repository = mock(DomainCustomerRepository.class);
        DomainService domainService = mock(DomainService.class);
        when(domainService.getDomain(10L)).thenReturn(domainView("allowed", "allowed"));

        DomainCustomerPo pending = customerPo(1L, "pending");
        DomainCustomerPo active = customerPo(1L, "active");
        active.setActivatedAt(NOW);
        DomainCustomerPo disabled = customerPo(1L, "disabled");
        disabled.setActivatedAt(NOW);
        disabled.setDisabledAt(NOW);
        DomainCustomerPo reactivated = customerPo(1L, "active");
        reactivated.setActivatedAt(NOW);

        when(repository.findById(1L, 10L))
                .thenReturn(pending, active, disabled, reactivated);
        when(repository.updateStatus(any(), any(), any(), eq(1L), eq(10L))).thenReturn(1);

        DomainCustomerService service = new DomainCustomerService(
                repository,
                domainService,
                CLOCK,
                mock(IdentitySubjectService.class),
                mock(CustomerAccountService.class));

        DomainCustomerDtos.DomainCustomerView activeView = service.updateCustomerStatus(
                10L, 1L, new DomainCustomerDtos.UpdateDomainCustomerStatusRequest("active"));
        DomainCustomerDtos.DomainCustomerView disabledView = service.updateCustomerStatus(
                10L, 1L, new DomainCustomerDtos.UpdateDomainCustomerStatusRequest("disabled"));
        DomainCustomerDtos.DomainCustomerView reactivatedView = service.updateCustomerStatus(
                10L, 1L, new DomainCustomerDtos.UpdateDomainCustomerStatusRequest("active"));

        assertEquals("active", activeView.status());
        assertEquals("disabled", disabledView.status());
        assertEquals("active", reactivatedView.status());
    }

    private DomainDtos.DomainView domainView(String registrationEnabled, String invitationEnabled) {
        return new DomainDtos.DomainView(
                10L,
                "default",
                "Default Domain",
                null,
                "logo.png",
                List.of("public"),
                registrationEnabled,
                invitationEnabled,
                1,
                NOW,
                NOW,
                null,
                null,
                null,
                null,
                null);
    }

    private DomainCustomerPo customerPo(long id, String status) {
        DomainCustomerPo po = new DomainCustomerPo();
        po.setId(id);
        po.setBusinessDomainId(10L);
        po.setCustomerAccountId(100L);
        po.setSubjectId(200L);
        po.setUsername("customer");
        po.setNickname("Customer");
        po.setPhone("13800000000");
        po.setEmail("customer@uniondesk.local");
        po.setStatus(status);
        po.setSource("manual");
        po.setCreatedAt(NOW);
        po.setUpdatedAt(NOW);
        return po;
    }
}
