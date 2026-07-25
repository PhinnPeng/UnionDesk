-- 事项配置：事项类型菜单项

INSERT INTO iam_admin_menu (
    code, node_type, scope, name, route_path, component_key, permission_code,
    parent_id, order_no, icon, hidden, status, required
)
SELECT
    'PLATFORM-TICKET-CONFIG-TYPES',
    'menu',
    'platform',
    '事项类型',
    '/platform/ticket-config/types',
    'platform/ticket-config/types',
    NULL,
    parent.id,
    20,
    'AppstoreOutlined',
    0,
    1,
    0
FROM iam_admin_menu parent
WHERE parent.route_path = '/platform/ticket-config'
  AND parent.node_type = 'menu'
  AND parent.status = 1
  AND NOT EXISTS (
      SELECT 1 FROM iam_admin_menu m WHERE m.route_path = '/platform/ticket-config/types'
  );

INSERT IGNORE INTO iam_admin_role_menu_relation (role_id, menu_id, created_at)
SELECT r.id, m.id, CURRENT_TIMESTAMP(3)
FROM role r
JOIN iam_admin_menu m ON m.route_path = '/platform/ticket-config/types' AND m.status = 1
WHERE r.code IN ('super_admin', 'platform_admin');
