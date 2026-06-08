INSERT INTO iam_permission (
    code, name, description, permission_scope, resource_code, action_code, http_method, path_pattern, status
)
VALUES
    ('platform.audit_log.read', '查看平台审计日志', '查看平台级审计日志列表', 'platform', 'audit_log', 'read', 'GET', '/api/v1/admin/audit-logs', 1),
    ('domain.audit_log.read', '查看域审计日志', '查看业务域内审计日志列表', 'domain', 'audit_log', 'read', 'GET', '/api/v1/admin/domains/*/audit-logs', 1),
    ('domain.login_log.read', '查看域登录日志', '查看业务域内登录日志列表', 'domain', 'login_log', 'read', 'GET', '/api/v1/admin/domains/*/login-logs', 1)
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
JOIN iam_permission p ON p.code IN (
    'platform.audit_log.read'
)
WHERE r.code = 'platform_admin'
ON DUPLICATE KEY UPDATE
    role_id = VALUES(role_id),
    permission_id = VALUES(permission_id);

INSERT INTO iam_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
JOIN iam_permission p ON p.code IN (
    'platform.audit_log.read'
)
WHERE r.code = 'super_admin'
ON DUPLICATE KEY UPDATE
    role_id = VALUES(role_id),
    permission_id = VALUES(permission_id);

INSERT INTO iam_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
JOIN iam_permission p ON p.code IN (
    'domain.audit_log.read',
    'domain.login_log.read'
)
WHERE r.code = 'domain_admin'
ON DUPLICATE KEY UPDATE
    role_id = VALUES(role_id),
    permission_id = VALUES(permission_id);

INSERT INTO iam_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
JOIN iam_permission p ON p.code IN (
    'domain.audit_log.read',
    'domain.login_log.read'
)
WHERE r.code = 'super_admin'
ON DUPLICATE KEY UPDATE
    role_id = VALUES(role_id),
    permission_id = VALUES(permission_id);
