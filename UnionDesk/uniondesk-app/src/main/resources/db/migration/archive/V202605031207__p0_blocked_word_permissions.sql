INSERT INTO iam_permission (
    code, name, description, permission_scope, resource_code, action_code, http_method, path_pattern, status
)
VALUES
    ('domain.blocked_word.read', '查看屏蔽词', '查看业务域内屏蔽词列表', 'domain', 'blocked_word', 'read', 'GET', '/api/v1/admin/domains/*/blocked-words', 1),
    ('domain.blocked_word.create', '创建屏蔽词', '为业务域新增屏蔽词', 'domain', 'blocked_word', 'create', 'POST', '/api/v1/admin/domains/*/blocked-words', 1),
    ('domain.blocked_word.delete', '删除屏蔽词', '删除业务域内屏蔽词', 'domain', 'blocked_word', 'delete', 'DELETE', '/api/v1/admin/domains/*/blocked-words/*', 1)
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
    'domain.blocked_word.read',
    'domain.blocked_word.create',
    'domain.blocked_word.delete'
)
WHERE r.code = 'domain_admin'
ON DUPLICATE KEY UPDATE
    role_id = VALUES(role_id),
    permission_id = VALUES(permission_id);

INSERT INTO iam_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
JOIN iam_permission p ON p.code IN (
    'domain.blocked_word.read',
    'domain.blocked_word.create',
    'domain.blocked_word.delete'
)
WHERE r.code = 'super_admin'
ON DUPLICATE KEY UPDATE
    role_id = VALUES(role_id),
    permission_id = VALUES(permission_id);
