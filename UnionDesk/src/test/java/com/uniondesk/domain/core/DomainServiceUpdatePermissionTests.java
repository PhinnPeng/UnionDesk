package com.uniondesk.domain.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.uniondesk.auth.core.UserContext;
import com.uniondesk.auth.core.UserContextHolder;
import com.uniondesk.domain.web.DomainDtos;
import com.uniondesk.iam.core.IamService;
import com.uniondesk.iam.core.PermissionCodes;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DomainServiceUpdatePermissionTests {

    @Autowired
    private DomainService domainService;

    @MockBean
    private IamService iamService;

    @BeforeEach
    void setUp() {
        UserContextHolder.set(new UserContext(2L, "platform_admin", null, "sid-test", "ud-admin-web"));
    }

    @AfterEach
    void tearDown() {
        UserContextHolder.clear();
    }

    @Test
    void updateStatusOnlyWithUpdateStatusPermissionSucceeds() {
        stubPermissions(false, true);
        long domainId = createDomain();

        DomainDtos.DomainView updated = domainService.updateDomain(
                domainId, new DomainDtos.UpdateDomainRequest(null, null, null, null, null, null, null, 0));

        assertEquals(0, updated.status());
    }

    @Test
    void updateNameOnlyWithUpdateStatusPermissionForbidden() {
        stubPermissions(false, true);
        long domainId = createDomain();

        assertThrows(
                ResponseStatusException.class,
                () -> domainService.updateDomain(
                        domainId,
                        new DomainDtos.UpdateDomainRequest(null, "Renamed Domain", null, null, null, null, null, null)));
    }

    @Test
    void updateStatusOnlyWithUpdatePermissionForbidden() {
        stubPermissions(true, false);
        long domainId = createDomain();

        assertThrows(
                ResponseStatusException.class,
                () -> domainService.updateDomain(
                        domainId, new DomainDtos.UpdateDomainRequest(null, null, null, null, null, null, null, 0)));
    }

    @Test
    void updateNameOnlyWithUpdatePermissionSucceeds() {
        stubPermissions(true, false);
        long domainId = createDomain();

        DomainDtos.DomainView updated = domainService.updateDomain(
                domainId,
                new DomainDtos.UpdateDomainRequest(null, "Renamed Domain", null, null, null, null, null, null));

        assertEquals("Renamed Domain", updated.name());
    }

    private void stubPermissions(boolean hasUpdate, boolean hasUpdateStatus) {
        when(iamService.hasAnyPermission(any(), eq(List.of(PermissionCodes.PLATFORM_DOMAIN_CONTROL_GENERAL_UPDATE))))
                .thenReturn(hasUpdate);
        when(iamService.hasAnyPermission(
                        any(), eq(List.of(PermissionCodes.PLATFORM_DOMAIN_CONTROL_GENERAL_UPDATE_STATUS))))
                .thenReturn(hasUpdateStatus);
    }

    private long createDomain() {
        String code = "upd-perm-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        return domainService
                .createDomain(new DomainDtos.CreateDomainRequest(
                        code,
                        "Permission Test Domain",
                        null,
                        "/default-domain-logo.svg",
                        List.of("public"),
                        "allowed",
                        "allowed"))
                .id();
    }
}
