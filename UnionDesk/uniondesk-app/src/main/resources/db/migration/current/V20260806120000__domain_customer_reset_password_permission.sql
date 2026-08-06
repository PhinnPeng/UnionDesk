-- 域客户重置密码权限（业务控制台客户列表行内操作）
-- 重置后客户首次登录需强制修改密码

INSERT INTO iam_permission (
    code,
    name,
    description,
    permission_scope,
    resource_code,
    action_code,
    http_method,
    path_pattern,
    status
)
VALUES
    ('domain.customer.reset_password', '重置域客户密码', '重置域客户密码，重置后客户首次登录需强制修改密码', 'domain', 'domain.customer', 'reset_password', 'PUT', '/api/v1/admin/domains/*/customers/*/password', 1)
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
JOIN iam_permission p ON p.code = 'domain.customer.reset_password'
WHERE r.code IN ('domain_admin', 'super_admin')
ON DUPLICATE KEY UPDATE
    role_id = VALUES(role_id),
    permission_id = VALUES(permission_id);
