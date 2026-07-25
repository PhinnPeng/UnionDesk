package com.uniondesk.iam.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.uniondesk.iam.repository.StaffAccountRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class StaffAccountServiceTests {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-07-19T02:00:00Z"), ZoneOffset.UTC);

    @Mock
    private StaffAccountRepository staffAccountRepository;

    @Mock
    private IdentitySubjectService identitySubjectService;

    @Mock
    private PlatformRoleService platformRoleService;

    @Mock
    private PasswordEncoder passwordEncoder;

    private StaffAccountService service;

    @BeforeEach
    void setUp() {
        service = new StaffAccountService(
                staffAccountRepository,
                identitySubjectService,
                platformRoleService,
                passwordEncoder,
                CLOCK);
    }

    @Test
    void createRejectsInactiveSubject() {
        when(identitySubjectService.resolveSubjectIdByPhone("13800000001")).thenReturn(100L);
        doThrow(new IllegalArgumentException("身份主体不可用"))
                .when(identitySubjectService)
                .requireActiveSubject(100L);

        assertThatThrownBy(() -> service.create(new StaffAccountService.CreateStaffCommand(
                "agent01",
                "张三",
                null,
                "13800000001",
                null,
                "password123",
                List.of("domain_admin"),
                List.of(1L),
                List.of())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("身份主体不可用");

        verify(identitySubjectService).requireActiveSubject(100L);
    }

    @Test
    void createRequiresUsername() {
        assertThatThrownBy(() -> service.create(new StaffAccountService.CreateStaffCommand(
                " ",
                null,
                null,
                "13800000002",
                null,
                "password123",
                List.of("domain_admin"),
                List.of(1L),
                List.of())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("登录账号");
    }

    @Test
    void disableKeepsEmploymentActiveAndRevokesSessions() {
        StaffAccountService.StaffAccount existing = staff(1L, "active", "active");
        StaffAccountService.StaffAccount disabled = staff(1L, "disabled", "active");
        when(staffAccountRepository.findById(1L))
                .thenReturn(Optional.of(toPo(existing)))
                .thenReturn(Optional.of(toPo(disabled)));

        StaffAccountService.StaffAccount result = service.disable(1L);

        assertThat(result.status()).isEqualTo("disabled");
        assertThat(result.employmentStatus()).isEqualTo("active");
        verify(staffAccountRepository).updateStatus(1L, "disabled");
        verify(staffAccountRepository).revokeActiveSessions(1L, "staff_disabled");
        verify(staffAccountRepository, never()).offboard(anyLong(), any(), any(), any());
    }

    @Test
    void disableRejectsAlreadyOffboardedStaff() {
        when(staffAccountRepository.findById(1L)).thenReturn(Optional.of(toPo(staff(1L, "disabled", "offboarded"))));

        assertThatThrownBy(() -> service.disable(1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("已离职");
        verify(staffAccountRepository, never()).updateStatus(anyLong(), any());
    }

    @Test
    void offboardSetsEmploymentAndRevokesSessions() {
        StaffAccountService.StaffAccount existing = staff(1L, "active", "active");
        StaffAccountService.StaffAccount offboarded = staff(1L, "disabled", "offboarded");
        when(staffAccountRepository.findById(1L))
                .thenReturn(Optional.of(toPo(existing)))
                .thenReturn(Optional.of(toPo(offboarded)));

        StaffAccountService.StaffAccount result = service.offboard(1L, 9L, " 离职 ");

        assertThat(result.employmentStatus()).isEqualTo("offboarded");
        verify(platformRoleService).validateStaffStatusChange(1L, "offboarded");
        verify(staffAccountRepository).offboard(
                eq(1L),
                eq(LocalDateTime.ofInstant(Instant.parse("2026-07-19T02:00:00Z"), ZoneOffset.UTC)),
                eq(9L),
                eq("离职"));
        verify(staffAccountRepository).revokeActiveSessions(1L, "staff_offboarded");
    }

    private static StaffAccountService.StaffAccount staff(long id, String status, String employmentStatus) {
        return new StaffAccountService.StaffAccount(
                id,
                100L + id,
                "staff-" + id,
                "姓名",
                "昵称",
                null,
                "1380000000" + id,
                "staff-" + id + "@uniondesk.local",
                status,
                employmentStatus,
                null,
                null,
                null,
                "local",
                1);
    }

    private static com.uniondesk.iam.entity.StaffAccountPo toPo(StaffAccountService.StaffAccount staff) {
        com.uniondesk.iam.entity.StaffAccountPo po = new com.uniondesk.iam.entity.StaffAccountPo();
        po.setId(staff.id());
        po.setSubjectId(staff.subjectId());
        po.setUsername(staff.username());
        po.setRealName(staff.realName());
        po.setNickname(staff.nickname());
        po.setAvatarUrl(staff.avatarUrl());
        po.setPhone(staff.phone());
        po.setEmail(staff.email());
        po.setStatus(staff.status());
        po.setEmploymentStatus(staff.employmentStatus());
        po.setSource(staff.source());
        po.setAuthVersion(staff.authVersion());
        return po;
    }
}
