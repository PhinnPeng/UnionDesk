package com.uniondesk.iam.admin;

import static org.assertj.core.api.Assertions.assertThat;

import com.uniondesk.iam.admin.AdminPermissionCatalog.PermissionDefinition;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
class PlatformButtonPermissionAlignmentProbeTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void platformButtonsMustMatchGlobalRoleCatalogRules() {
        List<String> violations = new ArrayList<>();
        jdbcTemplate.query(
                """
                SELECT id, name, permission_code
                FROM iam_admin_menu
                WHERE node_type = 'button' AND scope = 'platform' AND status = 1
                ORDER BY id
                """,
                (row, index) -> {
                    long id = row.getLong("id");
                    String name = row.getString("name");
                    String permissionCode = row.getString("permission_code");
                    if (permissionCode == null || permissionCode.isBlank()) {
                        violations.add(id + " " + name + " -> missing permission_code");
                        return null;
                    }
                    var definition = AdminPermissionCatalog.findByCode(permissionCode);
                    if (definition.isEmpty()) {
                        violations.add(id + " " + name + " -> not in catalog: " + permissionCode);
                        return null;
                    }
                    if (!matchesGlobalBinding(definition.get())) {
                        violations.add(
                                id + " " + name + " -> scope mismatch: " + permissionCode
                                        + " (" + definition.get().permissionScope() + ")");
                    }
                    return null;
                });
        assertThat(violations).as("platform button permission violations").isEmpty();
    }

    private static boolean matchesGlobalBinding(PermissionDefinition definition) {
        return "platform".equalsIgnoreCase(definition.permissionScope())
                && definition.code().startsWith("platform.");
    }
}
