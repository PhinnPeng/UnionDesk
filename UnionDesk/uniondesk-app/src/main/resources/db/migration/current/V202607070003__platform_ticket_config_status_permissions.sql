-- 事项状态权限 + 菜单

INSERT INTO iam_permission (code, name, description, permission_scope, resource_code, action_code, http_method, path_pattern, status)
SELECT v.code, v.name, v.name, 'platform', v.code, v.code, v.http_method, v.path_pattern, 1
FROM (
    SELECT 'platform.ticket_config.status.read' AS code, '全局事项状态-查看' AS name, 'GET' AS http_method, '/api/v1/platform/ticket-statuses' AS path_pattern
    UNION ALL SELECT 'platform.ticket_config.status.create', '全局事项状态-创建', 'POST', '/api/v1/platform/ticket-statuses'
    UNION ALL SELECT 'platform.ticket_config.status.update', '全局事项状态-更新', 'PUT', '/api/v1/platform/ticket-statuses/**'
    UNION ALL SELECT 'platform.ticket_config.status.delete', '全局事项状态-删除', 'DELETE', '/api/v1/platform/ticket-statuses/*'
) v
WHERE NOT EXISTS (
    SELECT 1 FROM iam_permission p WHERE p.code = v.code
);

INSERT INTO iam_role_permission (role_id, permission_id, created_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP(3)
FROM role r
JOIN iam_permission p ON p.code IN (
    'platform.ticket_config.status.read',
    'platform.ticket_config.status.create',
    'platform.ticket_config.status.update',
    'platform.ticket_config.status.delete'
)
WHERE r.code IN ('super_admin', 'platform_admin')
  AND NOT EXISTS (
      SELECT 1 FROM iam_role_permission rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

INSERT INTO iam_admin_menu (
    code, node_type, scope, name, route_path, component_key, permission_code,
    parent_id, order_no, icon, hidden, status, required
)
SELECT
    'PLATFORM-TICKET-CONFIG-STATUSES',
    'menu',
    'platform',
    '事项状态',
    '/platform/ticket-config/statuses',
    'platform/ticket-config/statuses',
    NULL,
    parent.id,
    30,
    'FlagOutlined',
    0,
    1,
    0
FROM iam_admin_menu parent
WHERE parent.route_path = '/platform/ticket-config'
  AND parent.node_type = 'menu'
  AND parent.status = 1
  AND NOT EXISTS (
      SELECT 1 FROM iam_admin_menu m WHERE m.route_path = '/platform/ticket-config/statuses'
  );

INSERT IGNORE INTO iam_admin_role_menu_relation (role_id, menu_id, created_at)
SELECT r.id, m.id, CURRENT_TIMESTAMP(3)
FROM role r
JOIN iam_admin_menu m ON m.route_path = '/platform/ticket-config/statuses' AND m.status = 1
WHERE r.code IN ('super_admin', 'platform_admin');
