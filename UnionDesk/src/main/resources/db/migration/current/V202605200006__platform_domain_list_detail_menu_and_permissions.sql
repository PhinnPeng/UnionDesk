-- 业务域列表/详情菜单拆分 + domain.admin.list.read / domain.admin.detail.read 权限

INSERT INTO iam_permission (
    code, name, description, permission_scope, resource_code, action_code, http_method, path_pattern, status
)
VALUES
    ('domain.admin.list.read', '查看业务域列表', '查看后台业务域分页列表', 'platform', 'domain', 'admin_list_read', 'GET', '/api/v1/admin/domains', 1),
    ('domain.admin.detail.read', '查看业务域详情', '查看单个后台业务域详情', 'platform', 'domain', 'admin_detail_read', 'GET', '/api/v1/admin/domains/*', 1)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    description = VALUES(description),
    permission_scope = VALUES(permission_scope),
    resource_code = VALUES(resource_code),
    action_code = VALUES(action_code),
    http_method = VALUES(http_method),
    path_pattern = VALUES(path_pattern),
    status = VALUES(status),
    updated_at = CURRENT_TIMESTAMP(3);

UPDATE iam_permission
SET status = 0,
    updated_at = CURRENT_TIMESTAMP(3)
WHERE code = 'domain.admin.read';

INSERT IGNORE INTO iam_role_permission (role_id, permission_id, created_at)
SELECT rp.role_id, p_new.id, CURRENT_TIMESTAMP(3)
FROM iam_role_permission rp
JOIN iam_permission p_old ON p_old.id = rp.permission_id AND p_old.code = 'domain.admin.read'
JOIN iam_permission p_new ON p_new.code IN ('domain.admin.list.read', 'domain.admin.detail.read') AND p_new.status = 1;

INSERT IGNORE INTO iam_role_permission (role_id, permission_id, created_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP(3)
FROM role r
JOIN iam_permission p ON p.code IN ('domain.admin.list.read', 'domain.admin.detail.read') AND p.status = 1
WHERE r.code = 'super_admin';

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

INSERT INTO iam_admin_menu (
    code, node_type, scope, name, route_path, component_key, permission_code, parent_id, order_no, icon, hidden, status, required
)
SELECT
    'PLATFORM-DOMAIN-DETAIL', 'menu', 'platform', '业务域详情', '/platform/domains/detail', './platform/domains/detail',
    NULL,
    (SELECT id FROM iam_admin_menu WHERE code = 'PLATFORM-DOMAIN-CATALOG' LIMIT 1),
    2, 'ProfileOutlined', 1, 1, 0
WHERE NOT EXISTS (
    SELECT 1 FROM iam_admin_menu WHERE route_path = '/platform/domains/detail' AND node_type = 'menu'
);

UPDATE iam_admin_menu
SET permission_code = NULL,
    parent_id = (SELECT id FROM (SELECT id FROM iam_admin_menu WHERE code = 'PLATFORM-DOMAIN-CATALOG' LIMIT 1) AS catalog),
    component_key = './platform/domains/detail',
    hidden = 1,
    scope = 'platform',
    status = 1
WHERE route_path = '/platform/domains/detail' AND node_type = 'menu';

UPDATE iam_admin_menu
SET name = '查询业务域列表',
    permission_code = 'domain.admin.list.read',
    scope = 'platform',
    parent_id = (SELECT id FROM (SELECT id FROM iam_admin_menu WHERE route_path = '/platform/domains' AND node_type = 'menu' LIMIT 1) AS list_menu),
    order_no = 0
WHERE code = 'ADM0000000058'
   OR (node_type = 'button' AND name = '查询业务域列表' AND parent_id = (
       SELECT id FROM (SELECT id FROM iam_admin_menu WHERE route_path = '/platform/domains' AND node_type = 'menu' LIMIT 1) AS list_menu
   ));

UPDATE iam_admin_menu btn
JOIN iam_admin_menu detail_menu ON detail_menu.route_path = '/platform/domains/detail' AND detail_menu.node_type = 'menu'
SET btn.name = '查看业务域详情',
    btn.permission_code = 'domain.admin.detail.read',
    btn.parent_id = detail_menu.id,
    btn.scope = 'platform',
    btn.order_no = 0
WHERE btn.node_type = 'button'
  AND (btn.name = '查看业务域详情' OR btn.permission_code IN ('domain.admin.read', 'domain.admin.detail.read'));

INSERT INTO iam_admin_menu (code, node_type, scope, name, route_path, component_key, permission_code, parent_id, order_no, icon, hidden, status, required)
SELECT 'TMP-BTN-DOMAIN-DETAIL-V6', 'button', 'platform', '查看业务域详情', NULL, NULL, 'domain.admin.detail.read',
       (SELECT id FROM iam_admin_menu WHERE route_path = '/platform/domains/detail' AND node_type = 'menu' LIMIT 1),
       0, NULL, 0, 1, 0
WHERE NOT EXISTS (
    SELECT 1 FROM iam_admin_menu
    WHERE node_type = 'button' AND name = '查看业务域详情'
      AND parent_id = (SELECT id FROM iam_admin_menu WHERE route_path = '/platform/domains/detail' AND node_type = 'menu' LIMIT 1)
);

UPDATE iam_admin_menu
SET code = CONCAT('ADM', LPAD(id, 10, '0'))
WHERE code = 'TMP-BTN-DOMAIN-DETAIL-V6';

INSERT IGNORE INTO iam_admin_role_menu_relation (role_id, menu_id, created_at)
SELECT r.id, catalog.id, CURRENT_TIMESTAMP(3)
FROM role r
JOIN iam_admin_menu catalog ON catalog.code = 'PLATFORM-DOMAIN-CATALOG' AND catalog.status = 1
WHERE r.code = 'super_admin';

INSERT IGNORE INTO iam_admin_role_menu_relation (role_id, menu_id, created_at)
SELECT r.id, list_menu.id, CURRENT_TIMESTAMP(3)
FROM role r
JOIN iam_admin_menu list_menu ON list_menu.route_path = '/platform/domains' AND list_menu.node_type = 'menu' AND list_menu.status = 1
WHERE r.code = 'super_admin';

INSERT IGNORE INTO iam_admin_role_menu_relation (role_id, menu_id, created_at)
SELECT r.id, detail_menu.id, CURRENT_TIMESTAMP(3)
FROM role r
JOIN iam_admin_menu detail_menu ON detail_menu.route_path = '/platform/domains/detail' AND detail_menu.node_type = 'menu' AND detail_menu.status = 1
WHERE r.code = 'super_admin';

INSERT IGNORE INTO iam_admin_role_menu_relation (role_id, menu_id, created_at)
SELECT r.id, btn.id, CURRENT_TIMESTAMP(3)
FROM role r
JOIN iam_admin_menu btn ON btn.node_type = 'button' AND btn.scope = 'platform' AND btn.status = 1
WHERE r.code = 'super_admin'
  AND btn.permission_code IN (
      'domain.admin.list.read',
      'domain.admin.detail.read',
      'domain.admin.create',
      'domain.admin.update',
      'domain.admin.delete'
  )
  AND (
      btn.parent_id = (SELECT id FROM iam_admin_menu WHERE route_path = '/platform/domains' AND node_type = 'menu' LIMIT 1)
      OR btn.parent_id = (SELECT id FROM iam_admin_menu WHERE route_path = '/platform/domains/detail' AND node_type = 'menu' LIMIT 1)
  );
