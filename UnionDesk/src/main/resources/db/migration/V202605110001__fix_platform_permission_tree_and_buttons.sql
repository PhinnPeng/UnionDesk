-- ============================================================
-- 修复平台权限管理菜单树结构 + 补全缺失的 CRUD 按钮节点
--
-- 问题：
-- 1. 角色管理/菜单管理的 parent_id 可能未正确指向权限管理
-- 2. 角色管理/菜单管理缺少 CRUD 按钮节点（仅有 read 按钮）
-- ============================================================

-- 1. 修复 角色管理 和 菜单管理 的 parent_id → 权限管理
UPDATE iam_admin_menu
SET parent_id = (SELECT id FROM (SELECT id FROM iam_admin_menu WHERE route_path = '/platform/permission' AND status = 1 LIMIT 1) AS perm)
WHERE route_path = '/platform/role'
  AND (parent_id IS NULL OR parent_id != (SELECT id FROM (SELECT id FROM iam_admin_menu WHERE route_path = '/platform/permission' AND status = 1 LIMIT 1) AS perm2));

UPDATE iam_admin_menu
SET parent_id = (SELECT id FROM (SELECT id FROM iam_admin_menu WHERE route_path = '/platform/permission' AND status = 1 LIMIT 1) AS perm)
WHERE route_path = '/platform/menu'
  AND (parent_id IS NULL OR parent_id != (SELECT id FROM (SELECT id FROM iam_admin_menu WHERE route_path = '/platform/permission' AND status = 1 LIMIT 1) AS perm2));

-- 2. 补全 菜单管理 下的 CRUD 按钮节点
INSERT INTO iam_admin_menu (code, node_type, scope, name, route_path, component_key, permission_code, parent_id, order_no, icon, hidden, status, required)
SELECT 'TMP-BTN-MENU-CREATE', 'button', 'platform', '新增菜单', NULL, NULL, 'platform.menu.create',
       (SELECT id FROM iam_admin_menu WHERE route_path = '/platform/menu' LIMIT 1), 1, NULL, 0, 1, 0
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM iam_admin_menu WHERE permission_code = 'platform.menu.create' AND node_type = 'button');

INSERT INTO iam_admin_menu (code, node_type, scope, name, route_path, component_key, permission_code, parent_id, order_no, icon, hidden, status, required)
SELECT 'TMP-BTN-MENU-UPDATE', 'button', 'platform', '编辑菜单', NULL, NULL, 'platform.menu.update',
       (SELECT id FROM iam_admin_menu WHERE route_path = '/platform/menu' LIMIT 1), 2, NULL, 0, 1, 0
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM iam_admin_menu WHERE permission_code = 'platform.menu.update' AND node_type = 'button');

INSERT INTO iam_admin_menu (code, node_type, scope, name, route_path, component_key, permission_code, parent_id, order_no, icon, hidden, status, required)
SELECT 'TMP-BTN-MENU-DELETE', 'button', 'platform', '删除菜单', NULL, NULL, 'platform.menu.delete',
       (SELECT id FROM iam_admin_menu WHERE route_path = '/platform/menu' LIMIT 1), 3, NULL, 0, 1, 0
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM iam_admin_menu WHERE permission_code = 'platform.menu.delete' AND node_type = 'button');

-- 3. 补全 角色管理 下的 CRUD 按钮节点
INSERT INTO iam_admin_menu (code, node_type, scope, name, route_path, component_key, permission_code, parent_id, order_no, icon, hidden, status, required)
SELECT 'TMP-BTN-ROLE-CREATE', 'button', 'platform', '新增角色', NULL, NULL, 'platform.role.create',
       (SELECT id FROM iam_admin_menu WHERE route_path = '/platform/role' LIMIT 1), 1, NULL, 0, 1, 0
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM iam_admin_menu WHERE permission_code = 'platform.role.create' AND node_type = 'button');

INSERT INTO iam_admin_menu (code, node_type, scope, name, route_path, component_key, permission_code, parent_id, order_no, icon, hidden, status, required)
SELECT 'TMP-BTN-ROLE-UPDATE', 'button', 'platform', '编辑角色', NULL, NULL, 'platform.role.update',
       (SELECT id FROM iam_admin_menu WHERE route_path = '/platform/role' LIMIT 1), 2, NULL, 0, 1, 0
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM iam_admin_menu WHERE permission_code = 'platform.role.update' AND node_type = 'button');

INSERT INTO iam_admin_menu (code, node_type, scope, name, route_path, component_key, permission_code, parent_id, order_no, icon, hidden, status, required)
SELECT 'TMP-BTN-ROLE-DELETE', 'button', 'platform', '删除角色', NULL, NULL, 'platform.role.delete',
       (SELECT id FROM iam_admin_menu WHERE route_path = '/platform/role' LIMIT 1), 3, NULL, 0, 1, 0
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM iam_admin_menu WHERE permission_code = 'platform.role.delete' AND node_type = 'button');

INSERT INTO iam_admin_menu (code, node_type, scope, name, route_path, component_key, permission_code, parent_id, order_no, icon, hidden, status, required)
SELECT 'TMP-BTN-ROLE-PERM-READ', 'button', 'platform', '查看角色权限', NULL, NULL, 'platform.role_permission.read',
       (SELECT id FROM iam_admin_menu WHERE route_path = '/platform/role' LIMIT 1), 4, NULL, 0, 1, 0
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM iam_admin_menu WHERE permission_code = 'platform.role_permission.read' AND node_type = 'button');

INSERT INTO iam_admin_menu (code, node_type, scope, name, route_path, component_key, permission_code, parent_id, order_no, icon, hidden, status, required)
SELECT 'TMP-BTN-ROLE-PERM-UPDATE', 'button', 'platform', '修改角色权限', NULL, NULL, 'platform.role_permission.update',
       (SELECT id FROM iam_admin_menu WHERE route_path = '/platform/role' LIMIT 1), 5, NULL, 0, 1, 0
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM iam_admin_menu WHERE permission_code = 'platform.role_permission.update' AND node_type = 'button');

INSERT INTO iam_admin_menu (code, node_type, scope, name, route_path, component_key, permission_code, parent_id, order_no, icon, hidden, status, required)
SELECT 'TMP-BTN-ROLE-BIND', 'button', 'platform', '绑定平台角色', NULL, NULL, 'platform.role.bind',
       (SELECT id FROM iam_admin_menu WHERE route_path = '/platform/role' LIMIT 1), 6, NULL, 0, 1, 0
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM iam_admin_menu WHERE permission_code = 'platform.role.bind' AND node_type = 'button');

-- 4. 规范化临时 code
UPDATE iam_admin_menu
SET code = CONCAT('ADM', LPAD(id, 10, '0'))
WHERE code LIKE 'TMP-%';

-- 5. 授权 super_admin 拥有所有新增的平台按钮节点
INSERT IGNORE INTO iam_admin_role_menu_relation (role_id, menu_id, created_at)
SELECT r.id, btn.id, CURRENT_TIMESTAMP(3)
FROM role r
JOIN iam_admin_menu btn ON btn.node_type = 'button'
WHERE r.code = 'super_admin'
  AND btn.scope = 'platform'
  AND btn.status = 1
  AND btn.permission_code IN (
      'platform.menu.create',
      'platform.menu.update',
      'platform.menu.delete',
      'platform.role.create',
      'platform.role.update',
      'platform.role.delete',
      'platform.role_permission.read',
      'platform.role_permission.update',
      'platform.role.bind'
  );
