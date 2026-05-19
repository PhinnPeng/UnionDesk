-- ============================================================
-- 恢复平台侧组织管理菜单树、按钮权限与基础权限码
--
-- 说明：
-- 1. 组织管理作为平台侧顶层目录，包含 用户管理 / 组织架构 / 离职池 / 组织配置
-- 2. 现有平台用户与离职池按钮迁移到新父级，补齐组织相关占位按钮码
-- 3. 同步补回 super_admin / platform_admin 的权限种子
-- ============================================================

INSERT INTO iam_permission (
    code, name, description, permission_scope, resource_code, action_code, http_method, path_pattern, status
)
VALUES
    ('platform.organization.read', '查看组织架构', '查看平台组织架构', 'platform', 'platform.organization', 'read', 'GET', '/api/v1/iam/organizations', 1),
    ('platform.organization.create', '新增组织', '创建平台组织节点', 'platform', 'platform.organization', 'create', NULL, NULL, 1),
    ('platform.user.import', '导入用户', '导入平台用户', 'platform', 'platform.user', 'import', NULL, NULL, 1),
    ('platform.user.offboard_pool.export', '导出离职池', '导出离职池数据', 'platform', 'platform.user.offboard_pool', 'export', NULL, NULL, 1),
    ('platform.user.offboard_pool.batch_restore', '批量恢复', '批量恢复离职池用户', 'platform', 'platform.user.offboard_pool', 'batch_restore', NULL, NULL, 1)
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
    name = '组织管理',
    route_path = NULL,
    component_key = NULL,
    permission_code = NULL,
    parent_id = NULL,
    order_no = 2,
    icon = 'ApartmentOutlined',
    hidden = 0,
    status = 1
WHERE node_type = 'catalog'
  AND parent_id IS NULL
  AND name IN ('平台管理', '组织管理');

INSERT INTO iam_admin_menu (
    code, node_type, scope, name, route_path, component_key, permission_code, parent_id, order_no, icon, hidden, status, required
)
SELECT
    'TMP-PLATFORM-ORG-CATALOG', 'catalog', 'platform', '组织管理', NULL, NULL, NULL, NULL,
    2, 'ApartmentOutlined', 0, 1, 0
FROM dual
WHERE NOT EXISTS (
    SELECT 1
    FROM iam_admin_menu
    WHERE node_type = 'catalog'
      AND name = '组织管理'
      AND parent_id IS NULL
);

UPDATE iam_admin_menu
SET
    scope = 'platform',
    name = '用户管理',
    route_path = '/platform/user',
    component_key = './platform/user',
    permission_code = NULL,
    parent_id = (SELECT id FROM (SELECT id FROM iam_admin_menu WHERE node_type = 'catalog' AND name = '组织管理' AND parent_id IS NULL LIMIT 1) AS org_catalog),
    order_no = 1,
    icon = 'UserOutlined',
    hidden = 0,
    status = 1
WHERE node_type = 'menu'
  AND route_path = '/platform/user';

INSERT INTO iam_admin_menu (
    code, node_type, scope, name, route_path, component_key, permission_code, parent_id, order_no, icon, hidden, status, required
)
SELECT
    'TMP-PLATFORM-USER', 'menu', 'platform', '用户管理', '/platform/user', './platform/user', NULL,
    (SELECT id FROM iam_admin_menu WHERE node_type = 'catalog' AND name = '组织管理' AND parent_id IS NULL LIMIT 1),
    1, 'UserOutlined', 0, 1, 0
FROM dual
WHERE NOT EXISTS (
    SELECT 1
    FROM iam_admin_menu
    WHERE route_path = '/platform/user'
);

UPDATE iam_admin_menu
SET
    scope = 'platform',
    name = '组织架构',
    route_path = '/platform/dept',
    component_key = './platform/dept',
    permission_code = NULL,
    parent_id = (SELECT id FROM (SELECT id FROM iam_admin_menu WHERE node_type = 'catalog' AND name = '组织管理' AND parent_id IS NULL LIMIT 1) AS org_catalog),
    order_no = 2,
    icon = 'ApartmentOutlined',
    hidden = 0,
    status = 1
WHERE node_type = 'menu'
  AND route_path = '/platform/dept';

INSERT INTO iam_admin_menu (
    code, node_type, scope, name, route_path, component_key, permission_code, parent_id, order_no, icon, hidden, status, required
)
SELECT
    'TMP-PLATFORM-DEPT', 'menu', 'platform', '组织架构', '/platform/dept', './platform/dept', NULL,
    (SELECT id FROM iam_admin_menu WHERE node_type = 'catalog' AND name = '组织管理' AND parent_id IS NULL LIMIT 1),
    2, 'ApartmentOutlined', 0, 1, 0
FROM dual
WHERE NOT EXISTS (
    SELECT 1
    FROM iam_admin_menu
    WHERE route_path = '/platform/dept'
);

UPDATE iam_admin_menu
SET
    scope = 'platform',
    name = '离职池',
    route_path = '/platform/offboard-pool',
    component_key = './platform/offboard-pool',
    permission_code = NULL,
    parent_id = (SELECT id FROM (SELECT id FROM iam_admin_menu WHERE node_type = 'catalog' AND name = '组织管理' AND parent_id IS NULL LIMIT 1) AS org_catalog),
    order_no = 3,
    icon = 'UserDeleteOutlined',
    hidden = 0,
    status = 1
WHERE node_type = 'menu'
  AND route_path = '/platform/offboard-pool';

INSERT INTO iam_admin_menu (
    code, node_type, scope, name, route_path, component_key, permission_code, parent_id, order_no, icon, hidden, status, required
)
SELECT
    'TMP-PLATFORM-OFFBOARD-POOL', 'menu', 'platform', '离职池', '/platform/offboard-pool', './platform/offboard-pool', NULL,
    (SELECT id FROM iam_admin_menu WHERE node_type = 'catalog' AND name = '组织管理' AND parent_id IS NULL LIMIT 1),
    3, 'UserDeleteOutlined', 0, 1, 0
FROM dual
WHERE NOT EXISTS (
    SELECT 1
    FROM iam_admin_menu
    WHERE route_path = '/platform/offboard-pool'
);

UPDATE iam_admin_menu
SET
    scope = 'platform',
    name = '组织配置',
    route_path = '/platform/org-config',
    component_key = './platform/org-config',
    permission_code = NULL,
    parent_id = (SELECT id FROM (SELECT id FROM iam_admin_menu WHERE node_type = 'catalog' AND name = '组织管理' AND parent_id IS NULL LIMIT 1) AS org_catalog),
    order_no = 4,
    icon = 'SettingOutlined',
    hidden = 0,
    status = 1
WHERE node_type = 'menu'
  AND route_path = '/platform/org-config';

INSERT INTO iam_admin_menu (
    code, node_type, scope, name, route_path, component_key, permission_code, parent_id, order_no, icon, hidden, status, required
)
SELECT
    'TMP-PLATFORM-ORG-CONFIG', 'menu', 'platform', '组织配置', '/platform/org-config', './platform/org-config', NULL,
    (SELECT id FROM iam_admin_menu WHERE node_type = 'catalog' AND name = '组织管理' AND parent_id IS NULL LIMIT 1),
    4, 'SettingOutlined', 0, 1, 0
FROM dual
WHERE NOT EXISTS (
    SELECT 1
    FROM iam_admin_menu
    WHERE route_path = '/platform/org-config'
);

UPDATE iam_admin_menu
SET
    scope = 'platform',
    name = '查询平台用户',
    route_path = NULL,
    component_key = NULL,
    permission_code = 'platform.user.read',
    parent_id = (SELECT id FROM (SELECT id FROM iam_admin_menu WHERE route_path = '/platform/user' LIMIT 1) AS platform_user_menu),
    order_no = 0,
    icon = NULL,
    hidden = 0,
    status = 1,
    required = 1
WHERE node_type = 'button'
  AND permission_code = 'platform.user.read';

INSERT INTO iam_admin_menu (
    code, node_type, scope, name, route_path, component_key, permission_code, parent_id, order_no, icon, hidden, status, required
)
SELECT
    'TMP-BUTTON-PLATFORM-USER-READ', 'button', 'platform', '查询平台用户', NULL, NULL, 'platform.user.read',
    (SELECT id FROM iam_admin_menu WHERE route_path = '/platform/user' LIMIT 1),
    0, NULL, 0, 1, 1
FROM dual
WHERE NOT EXISTS (
    SELECT 1
    FROM iam_admin_menu
    WHERE node_type = 'button'
      AND permission_code = 'platform.user.read'
);

UPDATE iam_admin_menu
SET
    scope = 'platform',
    name = '新增用户',
    route_path = NULL,
    component_key = NULL,
    permission_code = 'platform.user.create',
    parent_id = (SELECT id FROM (SELECT id FROM iam_admin_menu WHERE route_path = '/platform/user' LIMIT 1) AS platform_user_menu),
    order_no = 1,
    icon = NULL,
    hidden = 0,
    status = 1,
    required = 0
WHERE node_type = 'button'
  AND permission_code = 'platform.user.create';

INSERT INTO iam_admin_menu (
    code, node_type, scope, name, route_path, component_key, permission_code, parent_id, order_no, icon, hidden, status, required
)
SELECT
    'TMP-BUTTON-PLATFORM-USER-CREATE', 'button', 'platform', '新增用户', NULL, NULL, 'platform.user.create',
    (SELECT id FROM iam_admin_menu WHERE route_path = '/platform/user' LIMIT 1),
    1, NULL, 0, 1, 0
FROM dual
WHERE NOT EXISTS (
    SELECT 1
    FROM iam_admin_menu
    WHERE node_type = 'button'
      AND permission_code = 'platform.user.create'
);

UPDATE iam_admin_menu
SET
    scope = 'platform',
    name = '导入用户',
    route_path = NULL,
    component_key = NULL,
    permission_code = 'platform.user.import',
    parent_id = (SELECT id FROM (SELECT id FROM iam_admin_menu WHERE route_path = '/platform/user' LIMIT 1) AS platform_user_menu),
    order_no = 2,
    icon = NULL,
    hidden = 0,
    status = 1,
    required = 0
WHERE node_type = 'button'
  AND permission_code = 'platform.user.import';

INSERT INTO iam_admin_menu (
    code, node_type, scope, name, route_path, component_key, permission_code, parent_id, order_no, icon, hidden, status, required
)
SELECT
    'TMP-BUTTON-PLATFORM-USER-IMPORT', 'button', 'platform', '导入用户', NULL, NULL, 'platform.user.import',
    (SELECT id FROM iam_admin_menu WHERE route_path = '/platform/user' LIMIT 1),
    2, NULL, 0, 1, 0
FROM dual
WHERE NOT EXISTS (
    SELECT 1
    FROM iam_admin_menu
    WHERE node_type = 'button'
      AND permission_code = 'platform.user.import'
);

UPDATE iam_admin_menu
SET
    scope = 'platform',
    name = '编辑用户',
    route_path = NULL,
    component_key = NULL,
    permission_code = 'platform.user.update',
    parent_id = (SELECT id FROM (SELECT id FROM iam_admin_menu WHERE route_path = '/platform/user' LIMIT 1) AS platform_user_menu),
    order_no = 3,
    icon = NULL,
    hidden = 0,
    status = 1,
    required = 0
WHERE node_type = 'button'
  AND permission_code = 'platform.user.update';

INSERT INTO iam_admin_menu (
    code, node_type, scope, name, route_path, component_key, permission_code, parent_id, order_no, icon, hidden, status, required
)
SELECT
    'TMP-BUTTON-PLATFORM-USER-UPDATE', 'button', 'platform', '编辑用户', NULL, NULL, 'platform.user.update',
    (SELECT id FROM iam_admin_menu WHERE route_path = '/platform/user' LIMIT 1),
    3, NULL, 0, 1, 0
FROM dual
WHERE NOT EXISTS (
    SELECT 1
    FROM iam_admin_menu
    WHERE node_type = 'button'
      AND permission_code = 'platform.user.update'
);

UPDATE iam_admin_menu
SET
    scope = 'platform',
    name = '离职用户',
    route_path = NULL,
    component_key = NULL,
    permission_code = 'platform.user.disable',
    parent_id = (SELECT id FROM (SELECT id FROM iam_admin_menu WHERE route_path = '/platform/user' LIMIT 1) AS platform_user_menu),
    order_no = 4,
    icon = NULL,
    hidden = 0,
    status = 1,
    required = 0
WHERE node_type = 'button'
  AND permission_code = 'platform.user.disable';

INSERT INTO iam_admin_menu (
    code, node_type, scope, name, route_path, component_key, permission_code, parent_id, order_no, icon, hidden, status, required
)
SELECT
    'TMP-BUTTON-PLATFORM-USER-DISABLE', 'button', 'platform', '离职用户', NULL, NULL, 'platform.user.disable',
    (SELECT id FROM iam_admin_menu WHERE route_path = '/platform/user' LIMIT 1),
    4, NULL, 0, 1, 0
FROM dual
WHERE NOT EXISTS (
    SELECT 1
    FROM iam_admin_menu
    WHERE node_type = 'button'
      AND permission_code = 'platform.user.disable'
);

UPDATE iam_admin_menu
SET
    scope = 'platform',
    name = '查询组织架构',
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
    'TMP-BUTTON-PLATFORM-ORG-READ', 'button', 'platform', '查询组织架构', NULL, NULL, 'platform.organization.read',
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
    name = '查询离职池',
    route_path = NULL,
    component_key = NULL,
    permission_code = 'platform.user.offboard_pool.read',
    parent_id = (SELECT id FROM (SELECT id FROM iam_admin_menu WHERE route_path = '/platform/offboard-pool' LIMIT 1) AS platform_offboard_menu),
    order_no = 0,
    icon = NULL,
    hidden = 0,
    status = 1,
    required = 1
WHERE node_type = 'button'
  AND permission_code = 'platform.user.offboard_pool.read';

INSERT INTO iam_admin_menu (
    code, node_type, scope, name, route_path, component_key, permission_code, parent_id, order_no, icon, hidden, status, required
)
SELECT
    'TMP-BUTTON-PLATFORM-OFFBOARD-READ', 'button', 'platform', '查询离职池', NULL, NULL, 'platform.user.offboard_pool.read',
    (SELECT id FROM iam_admin_menu WHERE route_path = '/platform/offboard-pool' LIMIT 1),
    0, NULL, 0, 1, 1
FROM dual
WHERE NOT EXISTS (
    SELECT 1
    FROM iam_admin_menu
    WHERE node_type = 'button'
      AND permission_code = 'platform.user.offboard_pool.read'
);

UPDATE iam_admin_menu
SET
    scope = 'platform',
    name = '导出离职池',
    route_path = NULL,
    component_key = NULL,
    permission_code = 'platform.user.offboard_pool.export',
    parent_id = (SELECT id FROM (SELECT id FROM iam_admin_menu WHERE route_path = '/platform/offboard-pool' LIMIT 1) AS platform_offboard_menu),
    order_no = 1,
    icon = NULL,
    hidden = 0,
    status = 1,
    required = 0
WHERE node_type = 'button'
  AND permission_code = 'platform.user.offboard_pool.export';

INSERT INTO iam_admin_menu (
    code, node_type, scope, name, route_path, component_key, permission_code, parent_id, order_no, icon, hidden, status, required
)
SELECT
    'TMP-BUTTON-PLATFORM-OFFBOARD-EXPORT', 'button', 'platform', '导出离职池', NULL, NULL, 'platform.user.offboard_pool.export',
    (SELECT id FROM iam_admin_menu WHERE route_path = '/platform/offboard-pool' LIMIT 1),
    1, NULL, 0, 1, 0
FROM dual
WHERE NOT EXISTS (
    SELECT 1
    FROM iam_admin_menu
    WHERE node_type = 'button'
      AND permission_code = 'platform.user.offboard_pool.export'
);

UPDATE iam_admin_menu
SET
    scope = 'platform',
    name = '批量恢复',
    route_path = NULL,
    component_key = NULL,
    permission_code = 'platform.user.offboard_pool.batch_restore',
    parent_id = (SELECT id FROM (SELECT id FROM iam_admin_menu WHERE route_path = '/platform/offboard-pool' LIMIT 1) AS platform_offboard_menu),
    order_no = 2,
    icon = NULL,
    hidden = 0,
    status = 1,
    required = 0
WHERE node_type = 'button'
  AND permission_code = 'platform.user.offboard_pool.batch_restore';

INSERT INTO iam_admin_menu (
    code, node_type, scope, name, route_path, component_key, permission_code, parent_id, order_no, icon, hidden, status, required
)
SELECT
    'TMP-BUTTON-PLATFORM-OFFBOARD-BATCH-RESTORE', 'button', 'platform', '批量恢复', NULL, NULL, 'platform.user.offboard_pool.batch_restore',
    (SELECT id FROM iam_admin_menu WHERE route_path = '/platform/offboard-pool' LIMIT 1),
    2, NULL, 0, 1, 0
FROM dual
WHERE NOT EXISTS (
    SELECT 1
    FROM iam_admin_menu
    WHERE node_type = 'button'
      AND permission_code = 'platform.user.offboard_pool.batch_restore'
);

UPDATE iam_admin_menu
SET
    scope = 'platform',
    name = '恢复用户',
    route_path = NULL,
    component_key = NULL,
    permission_code = 'platform.user.restore',
    parent_id = (SELECT id FROM (SELECT id FROM iam_admin_menu WHERE route_path = '/platform/offboard-pool' LIMIT 1) AS platform_offboard_menu),
    order_no = 3,
    icon = NULL,
    hidden = 0,
    status = 1,
    required = 0
WHERE node_type = 'button'
  AND permission_code = 'platform.user.restore';

INSERT INTO iam_admin_menu (
    code, node_type, scope, name, route_path, component_key, permission_code, parent_id, order_no, icon, hidden, status, required
)
SELECT
    'TMP-BUTTON-PLATFORM-OFFBOARD-RESTORE', 'button', 'platform', '恢复用户', NULL, NULL, 'platform.user.restore',
    (SELECT id FROM iam_admin_menu WHERE route_path = '/platform/offboard-pool' LIMIT 1),
    3, NULL, 0, 1, 0
FROM dual
WHERE NOT EXISTS (
    SELECT 1
    FROM iam_admin_menu
    WHERE node_type = 'button'
      AND permission_code = 'platform.user.restore'
);

UPDATE iam_admin_menu
SET
    scope = 'platform',
    name = '删除用户',
    route_path = NULL,
    component_key = NULL,
    permission_code = 'platform.user.delete',
    parent_id = (SELECT id FROM (SELECT id FROM iam_admin_menu WHERE route_path = '/platform/offboard-pool' LIMIT 1) AS platform_offboard_menu),
    order_no = 4,
    icon = NULL,
    hidden = 0,
    status = 1,
    required = 0
WHERE node_type = 'button'
  AND permission_code = 'platform.user.delete';

INSERT INTO iam_admin_menu (
    code, node_type, scope, name, route_path, component_key, permission_code, parent_id, order_no, icon, hidden, status, required
)
SELECT
    'TMP-BUTTON-PLATFORM-OFFBOARD-DELETE', 'button', 'platform', '删除用户', NULL, NULL, 'platform.user.delete',
    (SELECT id FROM iam_admin_menu WHERE route_path = '/platform/offboard-pool' LIMIT 1),
    4, NULL, 0, 1, 0
FROM dual
WHERE NOT EXISTS (
    SELECT 1
    FROM iam_admin_menu
    WHERE node_type = 'button'
      AND permission_code = 'platform.user.delete'
);

UPDATE iam_admin_menu
SET code = CONCAT('ADM', LPAD(id, 10, '0'))
WHERE code LIKE 'TMP-%';

INSERT IGNORE INTO iam_admin_role_menu_relation (role_id, menu_id, created_at)
SELECT r.id, m.id, CURRENT_TIMESTAMP(3)
FROM role r
JOIN iam_admin_menu m ON m.node_type = 'menu'
WHERE r.code = 'super_admin'
  AND m.scope = 'platform'
  AND m.route_path IN (
      '/platform/user',
      '/platform/dept',
      '/platform/offboard-pool',
      '/platform/org-config'
  );

INSERT IGNORE INTO iam_admin_role_menu_relation (role_id, menu_id, created_at)
SELECT r.id, btn.id, CURRENT_TIMESTAMP(3)
FROM role r
JOIN iam_admin_menu btn ON btn.node_type = 'button'
JOIN iam_admin_menu parent ON parent.id = btn.parent_id
WHERE r.code = 'super_admin'
  AND parent.route_path IN (
      '/platform/user',
      '/platform/dept',
      '/platform/offboard-pool'
  );

INSERT INTO iam_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
JOIN iam_permission p ON p.code IN (
    'platform.organization.read',
    'platform.organization.create',
    'platform.user.import',
    'platform.user.offboard_pool.export',
    'platform.user.offboard_pool.batch_restore'
)
WHERE r.code IN ('super_admin', 'platform_admin')
ON DUPLICATE KEY UPDATE
    role_id = VALUES(role_id),
    permission_id = VALUES(permission_id);
