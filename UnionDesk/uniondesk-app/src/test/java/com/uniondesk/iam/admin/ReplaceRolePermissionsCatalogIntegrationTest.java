package com.uniondesk.iam.admin;

import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@Rollback
class ReplaceRolePermissionsCatalogIntegrationTest {

    @Autowired
    private AdminMenuService adminMenuService;

    @Test
    void replaceRolePermissionsAcceptsCatalogNodesInMenuIds() {
        List<Long> menuIds = List.of(
                92L, 38L, 70L, 39L, 40L, 41L, 71L, 80L, 43L, 85L, 109L, 110L, 118L, 105L, 115L, 128L,
                86L, 87L, 88L, 132L, 124L, 48L, 49L, 50L);
        List<Long> buttonIds = List.of(
                56L, 16L, 57L, 72L, 18L, 19L, 79L, 73L, 74L, 77L, 78L, 20L, 75L, 76L, 21L, 22L, 58L, 82L,
                104L, 81L, 111L, 112L, 114L, 113L, 125L, 126L, 127L, 129L, 130L, 131L, 63L, 69L, 89L, 90L,
                94L, 95L, 96L, 97L, 98L, 99L, 100L, 101L, 102L, 103L, 106L, 107L, 108L, 116L, 117L, 119L,
                120L, 121L, 122L, 123L);

        assertThatCode(() -> adminMenuService.replaceRolePermissions(4, menuIds, buttonIds))
                .doesNotThrowAnyException();
    }
}
