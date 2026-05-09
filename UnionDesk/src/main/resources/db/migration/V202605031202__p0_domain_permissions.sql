INSERT INTO iam_permission (
    code, name, description, permission_scope, resource_code, action_code, http_method, path_pattern, status
)
VALUES
    ('domain.read', '查看业务域', '查看客户可见业务域列表和详情', 'domain', 'domain', 'read', 'GET', '/api/v1/domains/**', 1),
    ('domain.admin.read', '查看业务域管理', '查看后台业务域列表和详情', 'platform', 'domain', 'admin_read', 'GET', '/api/v1/admin/domains/**', 1),
    ('domain.admin.create', '创建业务域', '创建新的业务域', 'platform', 'domain', 'admin_create', 'POST', '/api/v1/admin/domains', 1),
    ('domain.admin.update', '更新业务域', '更新业务域信息', 'platform', 'domain', 'admin_update', 'PUT', '/api/v1/admin/domains/*', 1),
    ('domain.admin.delete', '删除业务域', '删除业务域', 'platform', 'domain', 'admin_delete', 'DELETE', '/api/v1/admin/domains/*', 1)
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

INSERT INTO iam_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
JOIN iam_permission p ON p.code = 'domain.read'
WHERE r.code = 'customer'
ON DUPLICATE KEY UPDATE
    role_id = VALUES(role_id),
    permission_id = VALUES(permission_id);

INSERT INTO iam_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
JOIN iam_permission p ON p.code IN (
    'domain.read',
    'domain.admin.read',
    'domain.admin.create',
    'domain.admin.update',
    'domain.admin.delete'
)
WHERE r.code = 'platform_admin'
ON DUPLICATE KEY UPDATE
    role_id = VALUES(role_id),
    permission_id = VALUES(permission_id);

INSERT INTO iam_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
JOIN iam_permission p ON p.code IN (
    'domain.read',
    'domain.admin.read',
    'domain.admin.create',
    'domain.admin.update',
    'domain.admin.delete'
)
WHERE r.code = 'super_admin'
ON DUPLICATE KEY UPDATE
    role_id = VALUES(role_id),
    permission_id = VALUES(permission_id);
