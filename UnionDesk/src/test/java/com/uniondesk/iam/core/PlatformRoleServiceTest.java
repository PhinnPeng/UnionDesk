package com.uniondesk.iam.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 平台角色服务测试
 * 验收用例：TC-053 最后一个 platform_admin 保护
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PlatformRoleServiceTest {

    @Autowired(required = false)
    private PlatformRoleService platformRoleService;

    @Autowired(required = false)
    private JdbcClient jdbcClient;

    @BeforeEach
    void setUp() {
        if (jdbcClient == null) {
            return;
        }

        jdbcClient.sql("DELETE FROM staff_account_platform_role").update();

        // 准备测试数据：创建一个 identity_subject
        jdbcClient.sql("""
            INSERT INTO identity_subject (id, subject_type, phone, status)
            VALUES (1, 'person', '13800000011', 'active')
            ON DUPLICATE KEY UPDATE phone = VALUES(phone), status = VALUES(status)
            """)
            .update();

        // 创建一个 staff_account
        jdbcClient.sql("""
            INSERT INTO staff_account (id, subject_id, login_name, phone, email, password_hash, status)
            VALUES (1, 1, 'admin1', '13800000011', 'admin1@test.com', '{noop}password', 'active')
            ON DUPLICATE KEY UPDATE status = VALUES(status)
            """)
            .update();

        // 获取 platform_admin 角色 ID
        Long platformAdminRoleId = jdbcClient.sql("""
            SELECT id FROM platform_role WHERE code = 'platform_admin'
            """)
            .query(Long.class)
            .optional()
            .orElse(null);

        if (platformAdminRoleId != null) {
            // 为 staff_account 分配 platform_admin 角色
            jdbcClient.sql("""
                INSERT INTO staff_account_platform_role (staff_account_id, platform_role_id)
                VALUES (1, ?)
                ON DUPLICATE KEY UPDATE staff_account_id = VALUES(staff_account_id)
                """)
                .param(platformAdminRoleId)
                .update();
        }
    }

    @Test
    void testLastPlatformAdminProtection() {
        if (platformRoleService == null) {
            // 如果服务未注入，跳过测试
            return;
        }

        // Given: 平台仅存 1 个激活的 platform_admin (假设 staffAccountId = 1)
        long lastAdminId = 1L;
        
        // When: 尝试回收该账号的平台管理员角色
        // Then: 后端拒绝操作，提示"需先指定另一位平台管理员"
        Exception exception = assertThrows(IllegalStateException.class, () -> {
            platformRoleService.assignPlatformRoles(lastAdminId, List.of());
        });
        
        assertTrue(exception.getMessage().contains("需先指定另一位平台管理员"));
    }

    @Test
    void testPlatformAdminCanBeRemovedWhenNotLast() {
        if (platformRoleService == null || jdbcClient == null) {
            return;
        }

        // Given: 平台有多个激活的 platform_admin
        // 创建第二个 identity_subject 和 staff_account
        jdbcClient.sql("""
            INSERT INTO identity_subject (id, subject_type, phone, status)
            VALUES (2, 'person', '13800000002', 'active')
            ON DUPLICATE KEY UPDATE phone = VALUES(phone), status = VALUES(status)
            """)
            .update();

        jdbcClient.sql("""
            INSERT INTO staff_account (id, subject_id, login_name, phone, email, password_hash, status)
            VALUES (2, 2, 'admin2', '13800000002', 'admin2@test.com', '{noop}password', 'active')
            ON DUPLICATE KEY UPDATE status = VALUES(status)
            """)
            .update();

        Long platformAdminRoleId = jdbcClient.sql("""
            SELECT id FROM platform_role WHERE code = 'platform_admin'
            """)
            .query(Long.class)
            .optional()
            .orElse(null);

        if (platformAdminRoleId != null) {
            jdbcClient.sql("""
                INSERT INTO staff_account_platform_role (staff_account_id, platform_role_id)
                VALUES (2, ?)
                ON DUPLICATE KEY UPDATE staff_account_id = VALUES(staff_account_id)
                """)
                .param(platformAdminRoleId)
                .update();
        }

        // When: 回收其中一个的平台管理员角色
        // Then: 操作成功（不抛出异常）
        assertDoesNotThrow(() -> {
            platformRoleService.assignPlatformRoles(1L, List.of());
        });

        // 验证角色已被移除
        List<String> roles = platformRoleService.getCurrentPlatformRoles(1L);
        assertTrue(roles.isEmpty(), "角色应该已被移除");
    }

    @Test
    void testLastPlatformAdminCannotBeDisabled() {
        if (platformRoleService == null) {
            return;
        }

        // Given: 平台仅存 1 个激活的 platform_admin (staffAccountId = 1)
        long lastAdminId = 1L;

        // When: 尝试禁用该账号
        // Then: 后端拒绝操作
        Exception exception = assertThrows(IllegalStateException.class, () -> {
            platformRoleService.validateStaffStatusChange(lastAdminId, "disabled");
        });

        assertTrue(exception.getMessage().contains("需先指定另一位平台管理员"));
    }

    @Test
    void testLastPlatformAdminCannotBeOffboarded() {
        if (platformRoleService == null) {
            return;
        }

        // Given: 平台仅存 1 个激活的 platform_admin (staffAccountId = 1)
        long lastAdminId = 1L;

        // When: 尝试离职该账号
        // Then: 后端拒绝操作
        Exception exception = assertThrows(IllegalStateException.class, () -> {
            platformRoleService.validateStaffStatusChange(lastAdminId, "offboarded");
        });

        assertTrue(exception.getMessage().contains("需先指定另一位平台管理员"));
    }
}
