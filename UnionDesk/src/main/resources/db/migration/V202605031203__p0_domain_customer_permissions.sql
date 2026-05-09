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
    ('domain.customer.read', '查看域客户', '查看域客户列表', 'domain', 'domain.customer', 'read', 'GET', '/api/v1/admin/domains/*/customers', 1),
    ('domain.customer.create', '创建域客户', '创建域客户入域关系', 'domain', 'domain.customer', 'create', 'POST', '/api/v1/admin/domains/*/customers', 1),
    ('domain.customer.update_status', '更新域客户状态', '更新域客户状态', 'domain', 'domain.customer', 'update_status', 'PATCH', '/api/v1/admin/domains/*/customers/*/status', 1),
    ('domain.invitation_code.read', '查看邀请码', '查看邀请码列表', 'domain', 'domain.invitation_code', 'read', 'GET', '/api/v1/admin/domains/*/invitation-codes', 1),
    ('domain.invitation_code.create', '创建邀请码', '创建邀请码', 'domain', 'domain.invitation_code', 'create', 'POST', '/api/v1/admin/domains/*/invitation-codes', 1),
    ('domain.invitation_code.delete', '删除邀请码', '删除邀请码', 'domain', 'domain.invitation_code', 'delete', 'DELETE', '/api/v1/admin/domains/*/invitation-codes/*', 1)
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
    'domain.customer.read',
    'domain.customer.create',
    'domain.customer.update_status',
    'domain.invitation_code.read',
    'domain.invitation_code.create',
    'domain.invitation_code.delete'
)
WHERE r.code IN ('domain_admin', 'super_admin')
ON DUPLICATE KEY UPDATE
    role_id = VALUES(role_id),
    permission_id = VALUES(permission_id);

INSERT INTO iam_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
JOIN iam_permission p ON p.code = 'domain.customer.read'
WHERE r.code = 'agent'
ON DUPLICATE KEY UPDATE
    role_id = VALUES(role_id),
    permission_id = VALUES(permission_id);
