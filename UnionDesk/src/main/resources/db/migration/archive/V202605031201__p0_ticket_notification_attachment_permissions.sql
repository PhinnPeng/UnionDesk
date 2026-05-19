INSERT INTO iam_permission (
    code, name, description, permission_scope, resource_code, action_code, http_method, path_pattern, status
)
VALUES
    ('ticket.create', '创建工单', '客户提交新的工单', 'domain', 'ticket', 'create', 'POST', '/api/v1/domains/*/tickets', 1),
    ('ticket.view.self', '查看我的工单', '查看客户自己的工单列表与详情', 'domain', 'ticket', 'view_self', 'GET', '/api/v1/domains/*/tickets/my/**', 1),
    ('ticket.view.domain_all', '查看域内工单', '查看业务域内所有工单', 'domain', 'ticket', 'view_domain_all', 'GET', '/api/v1/admin/domains/*/tickets/**', 1),
    ('ticket.claim', '领取工单', '客服领取工单', 'domain', 'ticket', 'claim', 'POST', '/api/v1/admin/domains/*/tickets/*/claim', 1),
    ('ticket.assign', '分配工单', '客服分配工单', 'domain', 'ticket', 'assign', 'POST', '/api/v1/admin/domains/*/tickets/*/assign', 1),
    ('ticket.reply.self', '回复我的工单', '客户回复自己的工单', 'domain', 'ticket', 'reply_self', 'POST', '/api/v1/domains/*/tickets/my/**/replies', 1),
    ('ticket.reply', '回复工单', '客服回复工单', 'domain', 'ticket', 'reply', 'POST', '/api/v1/admin/domains/*/tickets/*/replies', 1),
    ('ticket.close', '关闭工单', '关闭业务域内工单', 'domain', 'ticket', 'close', 'PATCH', '/api/v1/admin/domains/*/tickets/*/status', 1),
    ('ticket.withdraw.self', '撤回我的工单', '客户撤回自己创建的工单', 'domain', 'ticket', 'withdraw_self', 'POST', '/api/v1/domains/*/tickets/my/**/withdraw', 1),
    ('ticket.merge', '合并工单', '将工单合并到目标工单', 'domain', 'ticket', 'merge', 'POST', '/api/v1/admin/domains/*/tickets/*/merge', 1),
    ('attachment.upload', '上传附件', '上传工单附件', 'domain', 'attachment', 'upload', 'POST', '/api/v1/attachments/upload', 1),
    ('attachment.download', '下载附件', '下载工单附件', 'domain', 'attachment', 'download', 'GET', '/api/v1/attachments/*/download', 1),
    ('inbox.read', '查看站内信', '查看站内信列表和未读数', 'domain', 'inbox', 'read', 'GET', '/api/v1/inbox/**', 1),
    ('inbox.mark_read', '标记站内信已读', '将站内信标记为已读', 'domain', 'inbox', 'mark_read', 'POST', '/api/v1/inbox/**', 1)
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
    'ticket.create',
    'ticket.view.self',
    'ticket.reply.self',
    'ticket.withdraw.self',
    'attachment.upload',
    'attachment.download',
    'inbox.read',
    'inbox.mark_read'
)
WHERE r.code = 'customer'
ON DUPLICATE KEY UPDATE
    role_id = VALUES(role_id),
    permission_id = VALUES(permission_id);

INSERT INTO iam_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
JOIN iam_permission p ON p.code IN (
    'ticket.view.domain_all',
    'ticket.claim',
    'ticket.assign',
    'ticket.reply',
    'ticket.close',
    'attachment.upload',
    'attachment.download',
    'inbox.read',
    'inbox.mark_read'
)
WHERE r.code = 'agent'
ON DUPLICATE KEY UPDATE
    role_id = VALUES(role_id),
    permission_id = VALUES(permission_id);

INSERT INTO iam_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
JOIN iam_permission p ON p.code IN (
    'ticket.view.domain_all',
    'ticket.claim',
    'ticket.assign',
    'ticket.reply',
    'ticket.close',
    'ticket.merge',
    'attachment.upload',
    'attachment.download',
    'inbox.read',
    'inbox.mark_read'
)
WHERE r.code = 'domain_admin'
ON DUPLICATE KEY UPDATE
    role_id = VALUES(role_id),
    permission_id = VALUES(permission_id);

INSERT INTO iam_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
JOIN iam_permission p
WHERE r.code = 'super_admin'
ON DUPLICATE KEY UPDATE
    role_id = VALUES(role_id),
    permission_id = VALUES(permission_id);
