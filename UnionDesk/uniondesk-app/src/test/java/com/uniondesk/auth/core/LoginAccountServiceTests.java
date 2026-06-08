package com.uniondesk.auth.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.uniondesk.auth.entity.LoginAccountPo;
import com.uniondesk.auth.repository.LoginAccountRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class LoginAccountServiceTests {

    @Mock
    private LoginAccountRepository loginAccountRepository;

    private final PasswordEncoder passwordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();

    private LoginAccountService service;

    @BeforeEach
    void setUp() {
        service = new LoginAccountService(loginAccountRepository, passwordEncoder);
    }

    @Test
    void findByIdentifierUsesStaffUsernameColumn() {
        LoginAccountPo po = new LoginAccountPo();
        po.setId(1L);
        po.setUsername("admin");
        po.setMobile("13800000000");
        po.setEmail("admin@test.com");
        po.setPasswordHash("hash");
        po.setStatus(1);
        po.setAccountType("staff");
        po.setEmploymentStatus("active");
        when(loginAccountRepository.findStaffByIdentifier(eq("username"), eq("admin"))).thenReturn(Optional.of(po));

        assertThat(service.findByIdentifier("admin", LoginIdentifierType.USERNAME, "staff"))
                .isPresent()
                .get()
                .extracting(LoginAccountService.LoginAccount::username)
                .isEqualTo("admin");
    }
}
