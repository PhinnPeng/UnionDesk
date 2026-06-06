package com.uniondesk.auth.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

@ExtendWith(MockitoExtension.class)
class LoginAccountServiceTests {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private LoginAccountService service;

    @BeforeEach
    void setUp() {
        service = new LoginAccountService(jdbcTemplate);
    }

    @Test
    void findByIdentifierUsesStaffUsernameColumn() {
        when(jdbcTemplate.query(
                org.mockito.ArgumentMatchers.contains("staff_account"),
                org.mockito.ArgumentMatchers.any(RowMapper.class),
                eq("admin")))
                .thenReturn(List.of(new LoginAccountService.LoginAccount(
                        1L,
                        "admin",
                        "13800000000",
                        "admin@test.com",
                        "hash",
                        1,
                        "staff",
                        "active")));

        assertThat(service.findByIdentifier("admin", LoginIdentifierType.USERNAME, "staff"))
                .isPresent()
                .get()
                .extracting(LoginAccountService.LoginAccount::username)
                .isEqualTo("admin");
    }
}
