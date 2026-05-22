-- 业务域管理目录：业务域列表菜单 + CRUD/详情按钮权限

INSERT INTO iam_admin_menu (code, node_type, scope, name, route_path, order_no, icon, status, required)
SELECT 'PLATFORM-DOMAIN-CATALOG', 'catalog', 'platform', '业务域管理', NULL, 6, 'GlobalOutlined', 1, 0
WHERE NOT EXISTS (SELECT 1 FROM iam_admin_menu WHERE code = 'PLATFORM-DOMAIN-CATALOG');

UPDATE iam_admin_menu
SET name = '业务域列表',
    node_type = 'menu',
    scope = 'platform',
    route_path = '/platform/domains',
    component_key = './platform/domains',
    permission_code = NULL,
    parent_id = (SELECT id FROM (SELECT id FROM iam_admin_menu WHERE code = 'PLATFORM-DOMAIN-CATALOG' LIMIT 1) AS catalog),
    order_no = 1,
    icon = 'GlobalOutlined',
    hidden = 0,
    status = 1
WHERE code = 'ADM0000000043'
   OR (route_path = '/platform/domains' AND node_type = 'menu');

UPDATE iam_admin_menu
SET name = '查询业务域列表',
    permission_code = 'domain.admin.read',
    scope = 'platform',
    parent_id = (SELECT id FROM (SELECT id FROM iam_admin_menu WHERE route_path = '/platform/domains' AND node_type = 'menu' LIMIT 1) AS list_menu),
    order_no = 0
WHERE code = 'ADM0000000058'
   OR (node_type = 'button' AND permission_code = 'domain.read' AND parent_id = (
       SELECT id FROM (SELECT id FROM iam_admin_menu WHERE route_path = '/platform/domains' AND node_type = 'menu' LIMIT 1) AS list_menu
   ));

INSERT INTO iam_admin_menu (code, node_type, scope, name, route_path, component_key, permission_code, parent_id, order_no, icon, hidden, status, required)
SELECT 'TMP-BTN-DOMAIN-DETAIL', 'button', 'platform', '查看业务域详情', NULL, NULL, 'domain.admin.detail',
       (SELECT id FROM iam_admin_menu WHERE route_path = '/platform/domains' AND node_type = 'menu' LIMIT 1),
       1, NULL, 0, 1, 0
WHERE NOT EXISTS (
    SELECT 1 FROM iam_admin_menu
    WHERE node_type = 'button' AND permission_code = 'domain.admin.detail' AND name = '查看业务域详情'
);

INSERT INTO iam_admin_menu (code, node_type, scope, name, route_path, component_key, permission_code, parent_id, order_no, icon, hidden, status, required)
SELECT 'TMP-BTN-DOMAIN-CREATE', 'button', 'platform', '新建业务域', NULL, NULL, 'domain.admin.create',
       (SELECT id FROM iam_admin_menu WHERE route_path = '/platform/domains' AND node_type = 'menu' LIMIT 1),
       2, NULL, 0, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM iam_admin_menu WHERE permission_code = 'domain.admin.create' AND node_type = 'button');

INSERT INTO iam_admin_menu (code, node_type, scope, name, route_path, component_key, permission_code, parent_id, order_no, icon, hidden, status, required)
SELECT 'TMP-BTN-DOMAIN-UPDATE', 'button', 'platform', '编辑业务域', NULL, NULL, 'domain.admin.update',
       (SELECT id FROM iam_admin_menu WHERE route_path = '/platform/domains' AND node_type = 'menu' LIMIT 1),
       3, NULL, 0, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM iam_admin_menu WHERE permission_code = 'domain.admin.update' AND node_type = 'button');

INSERT INTO iam_admin_menu (code, node_type, scope, name, route_path, component_key, permission_code, parent_id, order_no, icon, hidden, status, required)
SELECT 'TMP-BTN-DOMAIN-DELETE', 'button', 'platform', '删除业务域', NULL, NULL, 'domain.admin.delete',
       (SELECT id FROM iam_admin_menu WHERE route_path = '/platform/domains' AND node_type = 'menu' LIMIT 1),
       4, NULL, 0, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM iam_admin_menu WHERE permission_code = 'domain.admin.delete' AND node_type = 'button');

UPDATE iam_admin_menu
SET code = CONCAT('ADM', LPAD(id, 10, '0'))
WHERE code LIKE 'TMP-BTN-DOMAIN-%';

INSERT IGNORE INTO iam_admin_role_menu_relation (role_id, menu_id, created_at)
SELECT r.id, btn.id, CURRENT_TIMESTAMP(3)
FROM role r
JOIN iam_admin_menu btn ON btn.node_type = 'button' AND btn.scope = 'platform' AND btn.status = 1
WHERE r.code = 'super_admin'
  AND btn.permission_code IN (
      'domain.admin.read',
      'domain.admin.detail',
      'domain.admin.create',
      'domain.admin.update',
      'domain.admin.delete'
  )
  AND btn.parent_id = (SELECT id FROM iam_admin_menu WHERE route_path = '/platform/domains' AND node_type = 'menu' LIMIT 1);

INSERT IGNORE INTO iam_admin_role_menu_relation (role_id, menu_id, created_at)
SELECT r.id, list_menu.id, CURRENT_TIMESTAMP(3)
FROM role r
JOIN iam_admin_menu list_menu ON list_menu.route_path = '/platform/domains' AND list_menu.node_type = 'menu' AND list_menu.status = 1
WHERE r.code = 'super_admin';

INSERT IGNORE INTO iam_admin_role_menu_relation (role_id, menu_id, created_at)
SELECT r.id, catalog.id, CURRENT_TIMESTAMP(3)
FROM role r
JOIN iam_admin_menu catalog ON catalog.code = 'PLATFORM-DOMAIN-CATALOG' AND catalog.status = 1
WHERE r.code = 'super_admin';
