INSERT IGNORE INTO iam_permission (
    code, name, description, permission_scope, resource_code, action_code, http_method, path_pattern, status
) VALUES (
    'platform.user.reset_password',
    '重置平台用户密码',
    '重置平台用户登录密码',
    'platform',
    'platform.user',
    'reset_password',
    NULL,
    NULL,
    1
);

INSERT INTO iam_admin_menu (
    code, node_type, scope, name, route_path, component_key, permission_code,
    parent_id, order_no, icon, hidden, status, required
)
SELECT
    'TMP-BUTTON-PLATFORM-USER-RESET-PASSWORD',
    'button',
    'platform',
    '重置密码',
    NULL,
    NULL,
    'platform.user.reset_password',
    menu.id,
    45,
    NULL,
    0,
    1,
    0
FROM iam_admin_menu menu
WHERE menu.scope = 'platform'
  AND menu.node_type = 'menu'
  AND menu.route_path = '/platform/user'
  AND NOT EXISTS (
      SELECT 1
      FROM iam_admin_menu existing
      WHERE existing.permission_code = 'platform.user.reset_password'
  );
