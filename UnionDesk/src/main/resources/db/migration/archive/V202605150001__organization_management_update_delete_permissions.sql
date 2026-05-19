-- ============================================================
-- 补齐平台组织管理的新增、编辑、删除权限与按钮
-- 说明：
-- 1. 组织管理页面新增 update / delete 动作权限
-- 2. 同步修正 create 权限的接口口径，保持与后端路由一致
-- 3. 将 super_admin / platform_admin 的组织权限种子补全
-- ============================================================

INSERT INTO iam_permission (
    code, name, description, permission_scope, resource_code, action_code, http_method, path_pattern, status
)
VALUES
    ('platform.organization.read', '查看组织架构', '查看平台组织架构', 'platform', 'platform.organization', 'read', 'GET', '/api/v1/iam/organizations', 1),
    ('platform.organization.create', '新增组织', '创建平台组织节点', 'platform', 'platform.organization', 'create', 'POST', '/api/v1/iam/organizations', 1),
    ('platform.organization.update', '编辑组织', '编辑平台组织节点', 'platform', 'platform.organization', 'update', 'PUT', '/api/v1/iam/organizations/*', 1),
    ('platform.organization.delete', '删除组织', '删除平台组织节点', 'platform', 'platform.organization', 'delete', 'DELETE', '/api/v1/iam/organizations/*', 1)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    description = VALUES(description),
    permission_scope = VALUES(permission_scope),
    resource_code = VALUES(resource_code),
    action_code = VALUES(action_code),
    http_method = VALUES(http_method),
    path_pattern = VALUES(path_pattern),
    status = VALUES(status);

UPDATE iam_admin_menu
SET
    scope = 'platform',
    name = '查看组织架构',
    route_path = NULL,
    component_key = NULL,
    permission_code = 'platform.organization.read',
    parent_id = (SELECT id FROM (SELECT id FROM iam_admin_menu WHERE route_path = '/platform/dept' LIMIT 1) AS platform_dept_menu),
    order_no = 0,
    icon = NULL,
    hidden = 0,
    status = 1,
    required = 1
WHERE node_type = 'button'
  AND permission_code = 'platform.organization.read';

INSERT INTO iam_admin_menu (
    code, node_type, scope, name, route_path, component_key, permission_code, parent_id, order_no, icon, hidden, status, required
)
SELECT
    'TMP-BUTTON-PLATFORM-ORG-READ', 'button', 'platform', '查看组织架构', NULL, NULL, 'platform.organization.read',
    (SELECT id FROM iam_admin_menu WHERE route_path = '/platform/dept' LIMIT 1),
    0, NULL, 0, 1, 1
FROM dual
WHERE NOT EXISTS (
    SELECT 1
    FROM iam_admin_menu
    WHERE node_type = 'button'
      AND permission_code = 'platform.organization.read'
);

UPDATE iam_admin_menu
SET
    scope = 'platform',
    name = '新增组织',
    route_path = NULL,
    component_key = NULL,
    permission_code = 'platform.organization.create',
    parent_id = (SELECT id FROM (SELECT id FROM iam_admin_menu WHERE route_path = '/platform/dept' LIMIT 1) AS platform_dept_menu),
    order_no = 1,
    icon = NULL,
    hidden = 0,
    status = 1,
    required = 0
WHERE node_type = 'button'
  AND permission_code = 'platform.organization.create';

INSERT INTO iam_admin_menu (
    code, node_type, scope, name, route_path, component_key, permission_code, parent_id, order_no, icon, hidden, status, required
)
SELECT
    'TMP-BUTTON-PLATFORM-ORG-CREATE', 'button', 'platform', '新增组织', NULL, NULL, 'platform.organization.create',
    (SELECT id FROM iam_admin_menu WHERE route_path = '/platform/dept' LIMIT 1),
    1, NULL, 0, 1, 0
FROM dual
WHERE NOT EXISTS (
    SELECT 1
    FROM iam_admin_menu
    WHERE node_type = 'button'
      AND permission_code = 'platform.organization.create'
);

UPDATE iam_admin_menu
SET
    scope = 'platform',
    name = '编辑组织',
    route_path = NULL,
    component_key = NULL,
    permission_code = 'platform.organization.update',
    parent_id = (SELECT id FROM (SELECT id FROM iam_admin_menu WHERE route_path = '/platform/dept' LIMIT 1) AS platform_dept_menu),
    order_no = 2,
    icon = NULL,
    hidden = 0,
    status = 1,
    required = 0
WHERE node_type = 'button'
  AND permission_code = 'platform.organization.update';

INSERT INTO iam_admin_menu (
    code, node_type, scope, name, route_path, component_key, permission_code, parent_id, order_no, icon, hidden, status, required
)
SELECT
    'TMP-BUTTON-PLATFORM-ORG-UPDATE', 'button', 'platform', '编辑组织', NULL, NULL, 'platform.organization.update',
    (SELECT id FROM iam_admin_menu WHERE route_path = '/platform/dept' LIMIT 1),
    2, NULL, 0, 1, 0
FROM dual
WHERE NOT EXISTS (
    SELECT 1
    FROM iam_admin_menu
    WHERE node_type = 'button'
      AND permission_code = 'platform.organization.update'
);

UPDATE iam_admin_menu
SET
    scope = 'platform',
    name = '删除组织',
    route_path = NULL,
    component_key = NULL,
    permission_code = 'platform.organization.delete',
    parent_id = (SELECT id FROM (SELECT id FROM iam_admin_menu WHERE route_path = '/platform/dept' LIMIT 1) AS platform_dept_menu),
    order_no = 3,
    icon = NULL,
    hidden = 0,
    status = 1,
    required = 0
WHERE node_type = 'button'
  AND permission_code = 'platform.organization.delete';

INSERT INTO iam_admin_menu (
    code, node_type, scope, name, route_path, component_key, permission_code, parent_id, order_no, icon, hidden, status, required
)
SELECT
    'TMP-BUTTON-PLATFORM-ORG-DELETE', 'button', 'platform', '删除组织', NULL, NULL, 'platform.organization.delete',
    (SELECT id FROM iam_admin_menu WHERE route_path = '/platform/dept' LIMIT 1),
    3, NULL, 0, 1, 0
FROM dual
WHERE NOT EXISTS (
    SELECT 1
    FROM iam_admin_menu
    WHERE node_type = 'button'
      AND permission_code = 'platform.organization.delete'
);

INSERT IGNORE INTO iam_admin_role_menu_relation (role_id, menu_id, created_at)
SELECT r.id, btn.id, CURRENT_TIMESTAMP(3)
FROM role r
JOIN iam_admin_menu btn ON btn.node_type = 'button'
JOIN iam_admin_menu parent ON parent.id = btn.parent_id
WHERE r.code IN ('super_admin', 'platform_admin')
  AND parent.route_path = '/platform/dept'
  AND btn.permission_code IN (
      'platform.organization.read',
      'platform.organization.create',
      'platform.organization.update',
      'platform.organization.delete'
  );

INSERT INTO iam_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
JOIN iam_permission p ON p.code IN (
    'platform.organization.read',
    'platform.organization.create',
    'platform.organization.update',
    'platform.organization.delete',
    'platform.user.import',
    'platform.user.offboard_pool.export',
    'platform.user.offboard_pool.batch_restore'
)
WHERE r.code IN ('super_admin', 'platform_admin')
ON DUPLICATE KEY UPDATE
    role_id = VALUES(role_id),
    permission_id = VALUES(permission_id);
