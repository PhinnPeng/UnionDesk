-- 业务域控制台首页菜单 /home（手工脚本，非 Flyway）
-- 幂等：可重复执行

-- 1) 权限目录
INSERT INTO iam_permission (
    code, name, description, permission_scope, resource_code, action_code, http_method, path_pattern, status
)
SELECT 'domain.home.read', '查看业务域首页', '访问业务域控制台首页', 'domain', 'domain.home', 'read', NULL, NULL, 1
WHERE NOT EXISTS (SELECT 1 FROM iam_permission WHERE code = 'domain.home.read');

-- 2) 目录 + 菜单
INSERT INTO iam_admin_menu (code, node_type, scope, name, route_path, component_key, permission_code, parent_id, order_no, icon, hidden, status, required)
SELECT 'BUSINESS-HOME-CATALOG', 'catalog', 'business', '工作台', NULL, NULL, NULL, NULL, 1, 'HomeOutlined', 0, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM iam_admin_menu WHERE code = 'BUSINESS-HOME-CATALOG');

INSERT INTO iam_admin_menu (code, node_type, scope, name, route_path, component_key, permission_code, parent_id, order_no, icon, hidden, status, required)
SELECT 'BUSINESS-HOME-MENU', 'menu', 'business', '首页概览', '/home', './home', NULL,
       (SELECT id FROM iam_admin_menu WHERE code = 'BUSINESS-HOME-CATALOG' LIMIT 1),
       1, 'HomeOutlined', 0, 1, 1
WHERE NOT EXISTS (
    SELECT 1 FROM iam_admin_menu WHERE route_path = '/home' AND node_type = 'menu' AND scope = 'business'
);

UPDATE iam_admin_menu
SET parent_id = (SELECT id FROM (SELECT id FROM iam_admin_menu WHERE code = 'BUSINESS-HOME-CATALOG' LIMIT 1) AS c),
    scope = 'business',
    name = '首页概览',
    component_key = './home',
    order_no = 1,
    icon = 'HomeOutlined',
    hidden = 0,
    status = 1,
    required = 1,
    updated_at = CURRENT_TIMESTAMP(3)
WHERE route_path = '/home'
  AND node_type = 'menu'
  AND scope = 'business';

-- 3) 必需按钮
INSERT INTO iam_admin_menu (code, node_type, scope, name, permission_code, parent_id, order_no, status, required)
SELECT 'BUSINESS-HOME-BTN-READ', 'button', 'business', '查看业务域首页', 'domain.home.read',
       (SELECT id FROM iam_admin_menu WHERE route_path = '/home' AND node_type = 'menu' AND scope = 'business' LIMIT 1),
       0, 1, 1
WHERE NOT EXISTS (
    SELECT 1 FROM iam_admin_menu WHERE node_type = 'button' AND permission_code = 'domain.home.read'
);

-- 4) 业务域内置角色：菜单 + 按钮授权
INSERT IGNORE INTO iam_admin_role_menu_relation (role_id, menu_id, created_at)
SELECT r.id, m.id, CURRENT_TIMESTAMP(3)
FROM role r
JOIN iam_admin_menu m ON m.status = 1
WHERE r.scope = 'domain'
  AND r.code IN ('domain_admin', 'agent')
  AND (
      m.code = 'BUSINESS-HOME-CATALOG'
      OR (m.route_path = '/home' AND m.node_type = 'menu' AND m.scope = 'business')
      OR (m.node_type = 'button' AND m.permission_code = 'domain.home.read')
  );

INSERT INTO iam_role_permission (role_id, permission_id, created_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP(3)
FROM role r
JOIN iam_permission p ON p.code = 'domain.home.read' AND p.status = 1
WHERE r.scope = 'domain'
  AND r.code IN ('domain_admin', 'agent')
  AND NOT EXISTS (
      SELECT 1 FROM iam_role_permission existing
      WHERE existing.role_id = r.id AND existing.permission_id = p.id
  );
