-- 平台菜单图标统一 + 菜单精简后缺失的按钮权限补全（角色/菜单管理等）

-- ---------------------------------------------------------------------------
-- 1. 菜单图标（Ant Design Outlined 组件名，与前端 menu-icons 映射一致）
-- ---------------------------------------------------------------------------
UPDATE iam_admin_menu SET icon = 'DashboardOutlined', updated_at = CURRENT_TIMESTAMP(3)
WHERE code = 'PLATFORM-HOME-CATALOG' AND node_type = 'catalog';

UPDATE iam_admin_menu SET icon = 'HomeOutlined', updated_at = CURRENT_TIMESTAMP(3)
WHERE route_path = '/platform/home' AND node_type = 'menu';

UPDATE iam_admin_menu SET icon = 'ApartmentOutlined', updated_at = CURRENT_TIMESTAMP(3)
WHERE code = 'ADM0000000070' AND node_type = 'catalog';

UPDATE iam_admin_menu SET icon = 'UserOutlined', updated_at = CURRENT_TIMESTAMP(3)
WHERE route_path = '/platform/user' AND node_type = 'menu';

UPDATE iam_admin_menu SET icon = 'ApartmentOutlined', updated_at = CURRENT_TIMESTAMP(3)
WHERE route_path = '/platform/dept' AND node_type = 'menu';

UPDATE iam_admin_menu SET icon = 'UserDeleteOutlined', updated_at = CURRENT_TIMESTAMP(3)
WHERE route_path = '/platform/offboard-pool' AND node_type = 'menu';

UPDATE iam_admin_menu SET icon = 'SettingOutlined', updated_at = CURRENT_TIMESTAMP(3)
WHERE route_path = '/platform/org-config' AND node_type = 'menu';

UPDATE iam_admin_menu SET icon = 'GlobalOutlined', updated_at = CURRENT_TIMESTAMP(3)
WHERE code = 'PLATFORM-DOMAIN-CATALOG' AND node_type = 'catalog';

UPDATE iam_admin_menu SET icon = 'AppstoreOutlined', updated_at = CURRENT_TIMESTAMP(3)
WHERE route_path = '/platform/domains' AND node_type = 'menu';

UPDATE iam_admin_menu SET icon = 'ProfileOutlined', updated_at = CURRENT_TIMESTAMP(3)
WHERE route_path = '/platform/domains/detail' AND node_type = 'menu';

UPDATE iam_admin_menu SET icon = 'FileSearchOutlined', updated_at = CURRENT_TIMESTAMP(3)
WHERE code = 'PLATFORM-AUDIT-CATALOG' AND node_type = 'catalog';

UPDATE iam_admin_menu SET icon = 'LoginOutlined', updated_at = CURRENT_TIMESTAMP(3)
WHERE code = 'PLATFORM-LOGIN-LOG-MENU' AND node_type = 'menu';

UPDATE iam_admin_menu SET icon = 'FileTextOutlined', updated_at = CURRENT_TIMESTAMP(3)
WHERE code = 'PLATFORM-OP-LOG-MENU' AND node_type = 'menu';

UPDATE iam_admin_menu SET icon = 'SafetyCertificateOutlined', updated_at = CURRENT_TIMESTAMP(3)
WHERE route_path = '/platform/permission' AND node_type = 'menu';

UPDATE iam_admin_menu SET icon = 'TeamOutlined', updated_at = CURRENT_TIMESTAMP(3)
WHERE route_path = '/platform/role' AND node_type = 'menu';

UPDATE iam_admin_menu SET icon = 'MenuOutlined', updated_at = CURRENT_TIMESTAMP(3)
WHERE route_path = '/platform/menu' AND node_type = 'menu';

-- ---------------------------------------------------------------------------
-- 2. platform.home.query 权限目录 + 首页按钮文案
-- ---------------------------------------------------------------------------
INSERT INTO iam_permission (
    code, name, description, permission_scope, resource_code, action_code, http_method, path_pattern, status
)
SELECT 'platform.home.query', '查询平台首页', '访问平台首页概览与聚合统计', 'platform', 'platform.home', 'query', NULL, NULL, 1
WHERE NOT EXISTS (SELECT 1 FROM iam_permission WHERE code = 'platform.home.query');

UPDATE iam_admin_menu btn
JOIN (
    SELECT id FROM iam_admin_menu WHERE route_path = '/platform/home' AND node_type = 'menu' LIMIT 1
) home_menu ON btn.parent_id = home_menu.id
SET btn.name = '查询平台首页',
    btn.permission_code = 'platform.home.query',
    btn.updated_at = CURRENT_TIMESTAMP(3)
WHERE btn.node_type = 'button'
  AND btn.permission_code = 'platform.home.query';

INSERT IGNORE INTO iam_role_permission (role_id, permission_id, created_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP(3)
FROM role r
JOIN iam_permission p ON p.code = 'platform.home.query' AND p.status = 1
WHERE r.code = 'super_admin';

-- 说明：permission_code 在 iam_admin_menu 上全局唯一，不可在首页重复挂载与其它菜单相同的按钮权限。

-- ---------------------------------------------------------------------------
-- 3. 权限管理 / 角色 / 菜单：精简删除业务域侧按钮后，补回平台侧按钮
-- ---------------------------------------------------------------------------
INSERT INTO iam_admin_menu (code, node_type, scope, name, permission_code, parent_id, order_no, status, required)
SELECT 'TMP-BTN-PERM-MANAGE', 'button', 'platform', '管理权限目录', 'platform.permission.manage',
       (SELECT id FROM iam_admin_menu WHERE route_path = '/platform/permission' AND node_type = 'menu' LIMIT 1),
       0, 1, 1
WHERE NOT EXISTS (
    SELECT 1 FROM iam_admin_menu WHERE node_type = 'button' AND permission_code = 'platform.permission.manage'
);

INSERT INTO iam_admin_menu (code, node_type, scope, name, permission_code, parent_id, order_no, status, required)
SELECT 'TMP-BTN-ROLE-READ', 'button', 'platform', '查看角色', 'platform.role.read',
       (SELECT id FROM iam_admin_menu WHERE route_path = '/platform/role' AND node_type = 'menu' LIMIT 1),
       0, 1, 1
WHERE NOT EXISTS (
    SELECT 1 FROM iam_admin_menu WHERE node_type = 'button' AND permission_code = 'platform.role.read'
);

INSERT INTO iam_admin_menu (code, node_type, scope, name, permission_code, parent_id, order_no, status, required)
SELECT 'TMP-BTN-ROLE-CREATE', 'button', 'platform', '创建角色', 'platform.role.create',
       (SELECT id FROM iam_admin_menu WHERE route_path = '/platform/role' AND node_type = 'menu' LIMIT 1),
       1, 1, 0
WHERE NOT EXISTS (
    SELECT 1 FROM iam_admin_menu WHERE node_type = 'button' AND permission_code = 'platform.role.create'
);

INSERT INTO iam_admin_menu (code, node_type, scope, name, permission_code, parent_id, order_no, status, required)
SELECT 'TMP-BTN-ROLE-UPDATE', 'button', 'platform', '更新角色', 'platform.role.update',
       (SELECT id FROM iam_admin_menu WHERE route_path = '/platform/role' AND node_type = 'menu' LIMIT 1),
       2, 1, 0
WHERE NOT EXISTS (
    SELECT 1 FROM iam_admin_menu WHERE node_type = 'button' AND permission_code = 'platform.role.update'
);

INSERT INTO iam_admin_menu (code, node_type, scope, name, permission_code, parent_id, order_no, status, required)
SELECT 'TMP-BTN-ROLE-DELETE', 'button', 'platform', '删除角色', 'platform.role.delete',
       (SELECT id FROM iam_admin_menu WHERE route_path = '/platform/role' AND node_type = 'menu' LIMIT 1),
       3, 1, 0
WHERE NOT EXISTS (
    SELECT 1 FROM iam_admin_menu WHERE node_type = 'button' AND permission_code = 'platform.role.delete'
);

INSERT INTO iam_admin_menu (code, node_type, scope, name, permission_code, parent_id, order_no, status, required)
SELECT 'TMP-BTN-ROLE-BIND', 'button', 'platform', '绑定平台角色', 'platform.role.bind',
       (SELECT id FROM iam_admin_menu WHERE route_path = '/platform/role' AND node_type = 'menu' LIMIT 1),
       4, 1, 0
WHERE NOT EXISTS (
    SELECT 1 FROM iam_admin_menu WHERE node_type = 'button' AND permission_code = 'platform.role.bind'
);

INSERT INTO iam_admin_menu (code, node_type, scope, name, permission_code, parent_id, order_no, status, required)
SELECT 'TMP-BTN-ROLE-PERM-READ', 'button', 'platform', '查看角色授权', 'platform.role_permission.read',
       (SELECT id FROM iam_admin_menu WHERE route_path = '/platform/role' AND node_type = 'menu' LIMIT 1),
       5, 1, 0
WHERE NOT EXISTS (
    SELECT 1 FROM iam_admin_menu WHERE node_type = 'button' AND permission_code = 'platform.role_permission.read'
);

INSERT INTO iam_admin_menu (code, node_type, scope, name, permission_code, parent_id, order_no, status, required)
SELECT 'TMP-BTN-ROLE-PERM-UPDATE', 'button', 'platform', '更新角色授权', 'platform.role_permission.update',
       (SELECT id FROM iam_admin_menu WHERE route_path = '/platform/role' AND node_type = 'menu' LIMIT 1),
       6, 1, 0
WHERE NOT EXISTS (
    SELECT 1 FROM iam_admin_menu WHERE node_type = 'button' AND permission_code = 'platform.role_permission.update'
);

INSERT INTO iam_admin_menu (code, node_type, scope, name, permission_code, parent_id, order_no, status, required)
SELECT 'TMP-BTN-MENU-READ', 'button', 'platform', '查看菜单', 'platform.menu.read',
       (SELECT id FROM iam_admin_menu WHERE route_path = '/platform/menu' AND node_type = 'menu' LIMIT 1),
       0, 1, 1
WHERE NOT EXISTS (
    SELECT 1 FROM iam_admin_menu WHERE node_type = 'button' AND permission_code = 'platform.menu.read'
);

INSERT INTO iam_admin_menu (code, node_type, scope, name, permission_code, parent_id, order_no, status, required)
SELECT 'TMP-BTN-MENU-CREATE', 'button', 'platform', '创建菜单', 'platform.menu.create',
       (SELECT id FROM iam_admin_menu WHERE route_path = '/platform/menu' AND node_type = 'menu' LIMIT 1),
       1, 1, 0
WHERE NOT EXISTS (
    SELECT 1 FROM iam_admin_menu WHERE node_type = 'button' AND permission_code = 'platform.menu.create'
);

INSERT INTO iam_admin_menu (code, node_type, scope, name, permission_code, parent_id, order_no, status, required)
SELECT 'TMP-BTN-MENU-UPDATE', 'button', 'platform', '更新菜单', 'platform.menu.update',
       (SELECT id FROM iam_admin_menu WHERE route_path = '/platform/menu' AND node_type = 'menu' LIMIT 1),
       2, 1, 0
WHERE NOT EXISTS (
    SELECT 1 FROM iam_admin_menu WHERE node_type = 'button' AND permission_code = 'platform.menu.update'
);

INSERT INTO iam_admin_menu (code, node_type, scope, name, permission_code, parent_id, order_no, status, required)
SELECT 'TMP-BTN-MENU-DELETE', 'button', 'platform', '删除菜单', 'platform.menu.delete',
       (SELECT id FROM iam_admin_menu WHERE route_path = '/platform/menu' AND node_type = 'menu' LIMIT 1),
       3, 1, 0
WHERE NOT EXISTS (
    SELECT 1 FROM iam_admin_menu WHERE node_type = 'button' AND permission_code = 'platform.menu.delete'
);

-- ---------------------------------------------------------------------------
-- 5. 业务域列表 CRUD 按钮（若此前仅挂在已删除菜单上则重建）
-- ---------------------------------------------------------------------------
INSERT INTO iam_admin_menu (code, node_type, scope, name, permission_code, parent_id, order_no, status, required)
SELECT 'TMP-BTN-DOMAIN-CREATE-V22', 'button', 'platform', '新建业务域', 'domain.admin.create',
       (SELECT id FROM iam_admin_menu WHERE route_path = '/platform/domains' AND node_type = 'menu' LIMIT 1),
       2, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM iam_admin_menu WHERE permission_code = 'domain.admin.create' AND node_type = 'button');

INSERT INTO iam_admin_menu (code, node_type, scope, name, permission_code, parent_id, order_no, status, required)
SELECT 'TMP-BTN-DOMAIN-UPDATE-V22', 'button', 'platform', '编辑业务域', 'domain.admin.update',
       (SELECT id FROM iam_admin_menu WHERE route_path = '/platform/domains' AND node_type = 'menu' LIMIT 1),
       3, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM iam_admin_menu WHERE permission_code = 'domain.admin.update' AND node_type = 'button');

INSERT INTO iam_admin_menu (code, node_type, scope, name, permission_code, parent_id, order_no, status, required)
SELECT 'TMP-BTN-DOMAIN-DELETE-V22', 'button', 'platform', '删除业务域', 'domain.admin.delete',
       (SELECT id FROM iam_admin_menu WHERE route_path = '/platform/domains' AND node_type = 'menu' LIMIT 1),
       4, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM iam_admin_menu WHERE permission_code = 'domain.admin.delete' AND node_type = 'button');

-- 登录/操作日志按钮（菜单精简后若缺失则补）
INSERT INTO iam_admin_menu (code, node_type, scope, name, permission_code, parent_id, order_no, status, required)
SELECT 'TMP-BTN-LOGIN-LOG-V22', 'button', 'platform', '查询登录日志', 'platform.login_log.read',
       (SELECT id FROM iam_admin_menu WHERE code = 'PLATFORM-LOGIN-LOG-MENU' LIMIT 1),
       0, 1, 1
WHERE NOT EXISTS (
    SELECT 1 FROM iam_admin_menu WHERE node_type = 'button' AND permission_code = 'platform.login_log.read'
);

INSERT INTO iam_admin_menu (code, node_type, scope, name, permission_code, parent_id, order_no, status, required)
SELECT 'TMP-BTN-OP-LOG-V22', 'button', 'platform', '查询操作日志', 'platform.audit_log.read',
       (SELECT id FROM iam_admin_menu WHERE code = 'PLATFORM-OP-LOG-MENU' LIMIT 1),
       0, 1, 1
WHERE NOT EXISTS (
    SELECT 1 FROM iam_admin_menu WHERE node_type = 'button' AND permission_code = 'platform.audit_log.read'
);

UPDATE iam_admin_menu
SET code = CONCAT('ADM', LPAD(id, 10, '0'))
WHERE code LIKE 'TMP-BTN-%';

-- ---------------------------------------------------------------------------
-- 6. super_admin 绑定新增按钮
-- ---------------------------------------------------------------------------
INSERT IGNORE INTO iam_admin_role_menu_relation (role_id, menu_id, created_at)
SELECT r.id, btn.id, CURRENT_TIMESTAMP(3)
FROM role r
JOIN iam_admin_menu btn ON btn.node_type = 'button' AND btn.scope = 'platform' AND btn.status = 1
WHERE r.code = 'super_admin'
  AND btn.permission_code IN (
      'platform.home.query',
      'platform.permission.manage',
      'platform.role.read',
      'platform.role.create',
      'platform.role.update',
      'platform.role.delete',
      'platform.role.bind',
      'platform.role_permission.read',
      'platform.role_permission.update',
      'platform.menu.read',
      'platform.menu.create',
      'platform.menu.update',
      'platform.menu.delete',
      'domain.admin.create',
      'domain.admin.update',
      'domain.admin.delete',
      'platform.audit_log.read'
  );

INSERT IGNORE INTO iam_role_permission (role_id, permission_id, created_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP(3)
FROM role r
JOIN iam_permission p ON p.code = 'platform.home.query' AND p.status = 1
WHERE r.code = 'super_admin';
