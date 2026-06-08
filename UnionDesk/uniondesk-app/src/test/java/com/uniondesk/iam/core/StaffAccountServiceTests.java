package com.uniondesk.iam.core;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.uniondesk.iam.repository.StaffAccountRepository;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class StaffAccountServiceTests {

    @Mock
    private StaffAccountRepository staffAccountRepository;

    @Mock
    private IdentitySubjectService identitySubjectService;

    @Mock
    private PasswordEncoder passwordEncoder;

    private StaffAccountService service;

    @BeforeEach
    void setUp() {
        service = new StaffAccountService(staffAccountRepository, identitySubjectService, passwordEncoder);
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
                List.of(1L))))
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
                List.of(1L))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("登录账号");
    }
}
