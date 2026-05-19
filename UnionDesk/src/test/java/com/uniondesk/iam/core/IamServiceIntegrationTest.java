package com.uniondesk.iam.core;

import static org.assertj.core.api.Assertions.assertThat;

import com.uniondesk.auth.core.UserContext;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * IamService 集成测试。
 * 使用真实数据库验证 permission snapshot 的动作权限是否完整回填。
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class IamServiceIntegrationTest {

    @Autowired
    private IamService iamService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void loadPermissionSnapshotIncludesSuperAdminCrudActions() {
        Long adminUserId = jdbcTemplate.queryForObject(
                "SELECT id FROM user_account WHERE username = 'admin' LIMIT 1",
                Long.class);

        assertThat(adminUserId).as("admin 用户应存在").isNotNull();

        IamService.PermissionSnapshot snapshot = iamService.loadPermissionSnapshot(
                new UserContext(adminUserId, "super_admin", null, "sid-test", "ud-admin-web"));

        assertThat(snapshot.roles()).containsExactly("super_admin");

        Set<String> actionCodes = snapshot.actions().stream()
                .map(IamService.IamResource::resourceCode)
                .collect(Collectors.toSet());

        assertThat(actionCodes).contains(
                "platform.menu.create",
                "platform.menu.update",
                "platform.menu.delete",
                "platform.role.create",
                "platform.role.update",
                "platform.role.delete",
                "platform.role_permission.read",
                "platform.role_permission.update",
                "platform.user.reset_password");
    }
}
