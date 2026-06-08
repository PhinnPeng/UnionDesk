package com.uniondesk.iam.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.uniondesk.iam.entity.DomainMemberPresentationPo;
import com.uniondesk.iam.entity.StaffAccountPresentationPo;
import com.uniondesk.iam.repository.IdentityPresentationRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IdentityPresentationServiceTests {

    @Mock
    private IdentityPresentationRepository identityPresentationRepository;

    private IdentityPresentationService service;

    @BeforeEach
    void setUp() {
        service = new IdentityPresentationService(identityPresentationRepository);
    }

    @Test
    void resolveStaffInDomainUsesNicknameFallbackChain() {
        StaffAccountPresentationPo staff = new StaffAccountPresentationPo();
        staff.setUsername("agent01");
        staff.setRealName("张三");
        staff.setPhone("13800000001");
        staff.setEmail("a@test.com");
        DomainMemberPresentationPo member = new DomainMemberPresentationPo();
        member.setDomainNickname("客服小王");
        when(identityPresentationRepository.findStaffPresentation(10L)).thenReturn(Optional.of(staff));
        when(identityPresentationRepository.findDomainMemberPresentation(10L, 20L)).thenReturn(Optional.of(member));

        IdentityPresentationService.ResolvedStaffDomainView view = service.resolveStaffInDomain(10L, 20L);
        assertThat(view.realName()).isEqualTo("张三");
        assertThat(view.nickname()).isEqualTo("客服小王");
    }

    @Test
    void resolveStaffInDomainFallsBackToRealNameWhenNicknameMissing() {
        StaffAccountPresentationPo staff = new StaffAccountPresentationPo();
        staff.setUsername("agent02");
        staff.setRealName("李四");
        staff.setPhone("13800000001");
        staff.setEmail("a@test.com");
        when(identityPresentationRepository.findStaffPresentation(11L)).thenReturn(Optional.of(staff));
        when(identityPresentationRepository.findDomainMemberPresentation(11L, 21L)).thenReturn(Optional.empty());

        IdentityPresentationService.ResolvedStaffDomainView view = service.resolveStaffInDomain(11L, 21L);
        assertThat(view.nickname()).isEqualTo("李四");
    }
}
