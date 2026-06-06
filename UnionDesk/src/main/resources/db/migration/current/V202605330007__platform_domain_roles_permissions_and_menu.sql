-- 角色管理（只读）：platform.domain.roles.* + 控制台 catalog/按钮

INSERT INTO iam_permission (
    code, name, description, permission_scope, resource_code, action_code, http_method, path_pattern, status
)
SELECT 'platform.domain.roles.read', '查看角色管理', '查看业务域控制台角色列表',
       'platform', 'domain', 'platform_domain_roles_read', 'GET', '/api/v1/admin/domains/*/platform-roles', 1
WHERE NOT EXISTS (SELECT 1 FROM iam_permission WHERE code = 'platform.domain.roles.read');

INSERT INTO iam_permission (
    code, name, description, permission_scope, resource_code, action_code, http_method, path_pattern, status
)
SELECT 'platform.domain.roles.permissions.read', '查看角色权限', '查看业务域角色权限明细',
       'platform', 'domain', 'platform_domain_roles_permissions_read', 'GET', '/api/v1/admin/domains/*/platform-roles/*/permissions', 1
WHERE NOT EXISTS (SELECT 1 FROM iam_permission WHERE code = 'platform.domain.roles.permissions.read');

INSERT INTO iam_admin_menu (code, node_type, scope, name, route_path, component_key, permission_code, parent_id, order_no, icon, hidden, status, required)
SELECT 'PLATFORM-DOMAIN-ROLES', 'catalog', 'platform', '角色管理', NULL, NULL, NULL,
       (SELECT id FROM iam_admin_menu WHERE code = 'PLATFORM-DOMAIN-DETAIL' LIMIT 1),
       11, NULL, 1, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM iam_admin_menu WHERE code = 'PLATFORM-DOMAIN-ROLES');

UPDATE iam_admin_menu btn
JOIN iam_admin_menu catalog ON catalog.code = 'PLATFORM-DOMAIN-ROLES'
SET btn.parent_id = catalog.id,
    btn.scope = 'platform',
    btn.hidden = 0,
    btn.updated_at = CURRENT_TIMESTAMP(3)
WHERE btn.node_type = 'button'
  AND btn.permission_code IN (
      'platform.domain.roles.read',
      'platform.domain.roles.permissions.read'
  );

INSERT INTO iam_admin_menu (code, node_type, scope, name, permission_code, parent_id, order_no, status, required)
SELECT 'TMP-BTN-DOMAIN-ROLES-READ', 'button', 'platform', '查看角色管理', 'platform.domain.roles.read',
       (SELECT id FROM iam_admin_menu WHERE code = 'PLATFORM-DOMAIN-ROLES' LIMIT 1),
       0, 1, 0
WHERE NOT EXISTS (
    SELECT 1 FROM iam_admin_menu WHERE permission_code = 'platform.domain.roles.read' AND node_type = 'button'
);

INSERT INTO iam_admin_menu (code, node_type, scope, name, permission_code, parent_id, order_no, status, required)
SELECT 'TMP-BTN-DOMAIN-ROLES-PERMISSIONS-READ', 'button', 'platform', '查看角色权限', 'platform.domain.roles.permissions.read',
       (SELECT id FROM iam_admin_menu WHERE code = 'PLATFORM-DOMAIN-ROLES' LIMIT 1),
       1, 1, 0
WHERE NOT EXISTS (
    SELECT 1 FROM iam_admin_menu WHERE permission_code = 'platform.domain.roles.permissions.read' AND node_type = 'button'
);

UPDATE iam_admin_menu
SET code = CONCAT('ADM', LPAD(id, 10, '0'))
WHERE code IN ('TMP-BTN-DOMAIN-ROLES-READ', 'TMP-BTN-DOMAIN-ROLES-PERMISSIONS-READ');

INSERT IGNORE INTO iam_role_permission (role_id, permission_id, created_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP(3)
FROM role r
JOIN iam_permission p ON p.code IN (
    'platform.domain.roles.read',
    'platform.domain.roles.permissions.read'
) AND p.status = 1
WHERE r.code = 'super_admin';

INSERT IGNORE INTO iam_admin_role_menu_relation (role_id, menu_id, created_at)
SELECT r.id, m.id, CURRENT_TIMESTAMP(3)
FROM role r
JOIN iam_admin_menu m ON m.code = 'PLATFORM-DOMAIN-ROLES' AND m.status = 1
WHERE r.code = 'super_admin';

INSERT IGNORE INTO iam_admin_role_menu_relation (role_id, menu_id, created_at)
SELECT r.id, btn.id, CURRENT_TIMESTAMP(3)
FROM role r
JOIN iam_admin_menu btn ON btn.node_type = 'button'
    AND btn.permission_code IN (
        'platform.domain.roles.read',
        'platform.domain.roles.permissions.read'
    )
    AND btn.status = 1
WHERE r.code = 'super_admin';
