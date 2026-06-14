package com.uniondesk.common.audit;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class AuditDetailTextBuilderTests {

    @Test
    void buildDomainStatusDetail_formatsStatusChange() {
        String detail = AuditDetailTextBuilder.buildDomainStatusDetail("演示域", "demo", 0, 1);

        assertThat(detail).contains("业务域：演示域（demo）");
        assertThat(detail).contains("状态：禁用 → 启用");
    }

    @Test
    void buildRolePermissionDetail_listsAddedAndRemovedPaths() {
        String detail = AuditDetailTextBuilder.buildRolePermissionDetail(
                "平台管理员",
                "platform_admin",
                List.of("系统 / 角色管理"),
                List.of("系统 / 菜单管理"));

        assertThat(detail).contains("角色：平台管理员（platform_admin）");
        assertThat(detail).contains("新增菜单权限：");
        assertThat(detail).contains("- 系统 / 角色管理");
        assertThat(detail).contains("移除菜单权限：");
        assertThat(detail).contains("- 系统 / 菜单管理");
    }

    @Test
    void buildDomainUpdateDetail_omitsUnchangedFields() {
        String detail = AuditDetailTextBuilder.buildDomainUpdateDetail(
                "演示域",
                "demo",
                "演示域",
                "演示业务域",
                null,
                null,
                List.of("public"),
                List.of("public"),
                "allowed",
                "allowed",
                "allowed",
                "allowed");

        assertThat(detail).contains("名称：演示域 → 演示业务域");
        assertThat(detail).doesNotContain("可见策略：");
    }
}
