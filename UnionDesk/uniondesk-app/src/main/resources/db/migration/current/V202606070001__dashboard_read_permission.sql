-- Dashboard API 读权限（@RequirePermission platform.dashboard.read）

INSERT INTO iam_permission (
    code, name, description, permission_scope, resource_code, action_code, http_method, path_pattern, status
)
SELECT 'platform.dashboard.read', '查看业务看板', '查看业务看板统计数据',
       'platform', 'dashboard', 'platform_dashboard_read', 'GET', '/api/v1/dashboard', 1
WHERE NOT EXISTS (
    SELECT 1 FROM iam_permission WHERE code = 'platform.dashboard.read'
);

INSERT IGNORE INTO iam_role_permission (role_id, permission_id, created_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP(3)
FROM role r
JOIN iam_permission p ON p.code = 'platform.dashboard.read' AND p.status = 1
WHERE r.code = 'super_admin';
