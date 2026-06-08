package com.uniondesk.domain.core;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.uniondesk.common.event.UnionDeskEventPublisher;
import com.uniondesk.domain.repository.DomainMemberRepository;
import com.uniondesk.iam.core.IdentityPresentationService;
import com.uniondesk.iam.core.StaffAccountService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DomainMemberServiceTests {

    @Mock
    private DomainMemberRepository domainMemberRepository;

    @Mock
    private DomainService domainService;

    @Mock
    private IdentityPresentationService identityPresentationService;

    @Mock
    private StaffAccountService staffAccountService;

    @Mock
    private UnionDeskEventPublisher eventPublisher;

    private DomainMemberService domainMemberService;

    @BeforeEach
    void setUp() {
        domainMemberService = new DomainMemberService(
                domainMemberRepository,
                domainService,
                identityPresentationService,
                staffAccountService,
                eventPublisher);
    }

    @Test
    void guardLastDomainAdminRejectsRemovingLastHolder() {
        when(domainMemberRepository.findRoleCodesByMemberId(11L)).thenReturn(List.of("domain_admin"));
        when(domainMemberRepository.countActiveDomainAdmins(1L, 11L)).thenReturn(0);

        assertThatThrownBy(() -> domainMemberService.guardLastDomainAdmin(1L, 11L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("业务域管理员");
    }

    @Test
    void guardLastDomainSuperAdminRejectsRemovingLastHolder() {
        when(domainMemberRepository.findRoleCodesByMemberId(11L)).thenReturn(List.of("super_admin"));
        when(domainMemberRepository.countActiveDomainSuperAdmins(1L, 11L)).thenReturn(0);

        assertThatThrownBy(() -> domainMemberService.guardLastDomainSuperAdmin(1L, 11L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("业务域所有人");
    }
}
