package com.uniondesk.iam.core;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.uniondesk.iam.repository.CustomerAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class CustomerAccountServiceTests {

    @Mock
    private CustomerAccountRepository customerAccountRepository;

    @Mock
    private IdentitySubjectService identitySubjectService;

    @Mock
    private PasswordEncoder passwordEncoder;

    private CustomerAccountService service;

    @BeforeEach
    void setUp() {
        service = new CustomerAccountService(customerAccountRepository, identitySubjectService, passwordEncoder);
    }

    @Test
    void createValidatesSubjectBeforeInsert() {
        when(customerAccountRepository.countByUsername("demo")).thenReturn(0);
        when(identitySubjectService.resolveSubjectIdByPhone("13900000001")).thenReturn(200L);
        doThrow(new IllegalArgumentException("身份主体不可用"))
                .when(identitySubjectService)
                .requireActiveSubject(200L);

        assertThatThrownBy(() -> service.create(new CustomerAccountService.CreateCustomerCommand(
                "demo",
                "演示客户",
                "13900000001",
                null,
                "secret",
                false)))
                .isInstanceOf(IllegalArgumentException.class);

        verify(identitySubjectService).requireActiveSubject(200L);
    }
}
