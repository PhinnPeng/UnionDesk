INSERT INTO iam_permission (
    code, name, description, permission_scope, resource_code, action_code, http_method, path_pattern, status
)
VALUES
    ('domain.ticket_type.read', 'View ticket types', 'View ticket type configuration within a business domain', 'domain', 'ticket_type', 'read', 'GET', '/api/v1/admin/domains/*/ticket-types', 1),
    ('domain.ticket_type.create', 'Create ticket type', 'Create ticket type configuration within a business domain', 'domain', 'ticket_type', 'create', 'POST', '/api/v1/admin/domains/*/ticket-types', 1),
    ('domain.ticket_type.update', 'Update ticket type', 'Update ticket type configuration within a business domain', 'domain', 'ticket_type', 'update', 'PUT', '/api/v1/admin/domains/*/ticket-types/*', 1),
    ('domain.ticket_type.delete', 'Delete ticket type', 'Delete ticket type configuration within a business domain', 'domain', 'ticket_type', 'delete', 'DELETE', '/api/v1/admin/domains/*/ticket-types/*', 1),
    ('domain.ticket_template.read', 'View ticket templates', 'View ticket template configuration within a business domain', 'domain', 'ticket_template', 'read', 'GET', '/api/v1/admin/domains/*/ticket-templates', 1),
    ('domain.ticket_template.create', 'Create ticket template', 'Create ticket template configuration within a business domain', 'domain', 'ticket_template', 'create', 'POST', '/api/v1/admin/domains/*/ticket-templates', 1),
    ('domain.ticket_template.update', 'Update ticket template', 'Update ticket template configuration within a business domain', 'domain', 'ticket_template', 'update', 'PUT', '/api/v1/admin/domains/*/ticket-templates/*', 1),
    ('domain.ticket_template.delete', 'Delete ticket template', 'Delete ticket template configuration within a business domain', 'domain', 'ticket_template', 'delete', 'DELETE', '/api/v1/admin/domains/*/ticket-templates/*', 1),
    ('domain.quick_reply.read', 'View quick replies', 'View quick reply configuration within a business domain', 'domain', 'quick_reply', 'read', 'GET', '/api/v1/admin/domains/*/quick-reply*', 1),
    ('domain.quick_reply.create', 'Create quick reply', 'Create quick reply configuration within a business domain', 'domain', 'quick_reply', 'create', 'POST', '/api/v1/admin/domains/*/quick-reply*', 1),
    ('domain.quick_reply.update', 'Update quick reply', 'Update quick reply configuration within a business domain', 'domain', 'quick_reply', 'update', 'PUT', '/api/v1/admin/domains/*/quick-reply*/*', 1),
    ('domain.quick_reply.delete', 'Delete quick reply', 'Delete quick reply configuration within a business domain', 'domain', 'quick_reply', 'delete', 'DELETE', '/api/v1/admin/domains/*/quick-reply*/*', 1),
    ('domain.priority_level.read', 'View priority levels', 'View priority level configuration within a business domain', 'domain', 'priority_level', 'read', 'GET', '/api/v1/admin/domains/*/priority-levels', 1),
    ('domain.priority_level.create', 'Create priority level', 'Create priority level configuration within a business domain', 'domain', 'priority_level', 'create', 'POST', '/api/v1/admin/domains/*/priority-levels', 1),
    ('domain.priority_level.update', 'Update priority level', 'Update priority level configuration within a business domain', 'domain', 'priority_level', 'update', 'PUT', '/api/v1/admin/domains/*/priority-levels/*', 1),
    ('domain.priority_level.delete', 'Delete priority level', 'Delete priority level configuration within a business domain', 'domain', 'priority_level', 'delete', 'DELETE', '/api/v1/admin/domains/*/priority-levels/*', 1)
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
    'domain.ticket_type.read',
    'domain.ticket_type.create',
    'domain.ticket_type.update',
    'domain.ticket_type.delete',
    'domain.ticket_template.read',
    'domain.ticket_template.create',
    'domain.ticket_template.update',
    'domain.ticket_template.delete',
    'domain.quick_reply.read',
    'domain.quick_reply.create',
    'domain.quick_reply.update',
    'domain.quick_reply.delete',
    'domain.priority_level.read',
    'domain.priority_level.create',
    'domain.priority_level.update',
    'domain.priority_level.delete'
)
WHERE r.code = 'domain_admin'
ON DUPLICATE KEY UPDATE
    role_id = VALUES(role_id),
    permission_id = VALUES(permission_id);

INSERT INTO iam_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
JOIN iam_permission p ON p.code IN (
    'domain.ticket_type.read',
    'domain.ticket_type.create',
    'domain.ticket_type.update',
    'domain.ticket_type.delete',
    'domain.ticket_template.read',
    'domain.ticket_template.create',
    'domain.ticket_template.update',
    'domain.ticket_template.delete',
    'domain.quick_reply.read',
    'domain.quick_reply.create',
    'domain.quick_reply.update',
    'domain.quick_reply.delete',
    'domain.priority_level.read',
    'domain.priority_level.create',
    'domain.priority_level.update',
    'domain.priority_level.delete'
)
WHERE r.code = 'super_admin'
ON DUPLICATE KEY UPDATE
    role_id = VALUES(role_id),
    permission_id = VALUES(permission_id);
