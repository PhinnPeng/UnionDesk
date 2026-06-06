-- 员工管理：domain.member.* 权限 + 控制台 catalog/按钮

INSERT INTO iam_permission (
    code, name, description, permission_scope, resource_code, action_code, http_method, path_pattern, status
)
SELECT 'domain.member.read', '查看成员', '查看业务域成员列表', 'domain', 'domain.member', 'domain_member_read', 'GET', '/api/v1/admin/domains/*/members', 1
WHERE NOT EXISTS (SELECT 1 FROM iam_permission WHERE code = 'domain.member.read');

INSERT INTO iam_permission (
    code, name, description, permission_scope, resource_code, action_code, http_method, path_pattern, status
)
SELECT 'domain.member.create', '创建成员', '添加业务域成员（含新建员工）', 'domain', 'domain.member', 'domain_member_create', 'POST', '/api/v1/admin/domains/*/members/**', 1
WHERE NOT EXISTS (SELECT 1 FROM iam_permission WHERE code = 'domain.member.create');

INSERT INTO iam_permission (
    code, name, description, permission_scope, resource_code, action_code, http_method, path_pattern, status
)
SELECT 'domain.member.update_roles', '编辑成员角色', '更新业务域成员角色', 'domain', 'domain.member', 'domain_member_update_roles', 'PUT', '/api/v1/admin/domains/*/members/*/roles', 1
WHERE NOT EXISTS (SELECT 1 FROM iam_permission WHERE code = 'domain.member.update_roles');

INSERT INTO iam_permission (
    code, name, description, permission_scope, resource_code, action_code, http_method, path_pattern, status
)
SELECT 'domain.member.update_status', '启停成员', '禁用或启用业务域成员', 'domain', 'domain.member', 'domain_member_update_status', 'PUT', '/api/v1/admin/domains/*/members/*/status', 1
WHERE NOT EXISTS (SELECT 1 FROM iam_permission WHERE code = 'domain.member.update_status');

INSERT INTO iam_permission (
    code, name, description, permission_scope, resource_code, action_code, http_method, path_pattern, status
)
SELECT 'domain.member.delete', '删除成员', '软删除业务域成员', 'domain', 'domain.member', 'domain_member_delete', 'DELETE', '/api/v1/admin/domains/*/members/*', 1
WHERE NOT EXISTS (SELECT 1 FROM iam_permission WHERE code = 'domain.member.delete');

INSERT INTO iam_admin_menu (code, node_type, scope, name, route_path, component_key, permission_code, parent_id, order_no, icon, hidden, status, required)
SELECT 'PLATFORM-DOMAIN-MEMBERS', 'catalog', 'domain', '员工管理', NULL, NULL, NULL,
       (SELECT id FROM iam_admin_menu WHERE code = 'PLATFORM-DOMAIN-DETAIL' LIMIT 1),
       12, NULL, 1, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM iam_admin_menu WHERE code = 'PLATFORM-DOMAIN-MEMBERS');

INSERT INTO iam_admin_menu (code, node_type, scope, name, permission_code, parent_id, order_no, status, required)
SELECT 'TMP-BTN-DOMAIN-MEMBER-READ', 'button', 'domain', '查看成员', 'domain.member.read',
       (SELECT id FROM iam_admin_menu WHERE code = 'PLATFORM-DOMAIN-MEMBERS' LIMIT 1),
       0, 1, 0
WHERE NOT EXISTS (
    SELECT 1 FROM iam_admin_menu WHERE permission_code = 'domain.member.read' AND node_type = 'button'
);

INSERT INTO iam_admin_menu (code, node_type, scope, name, permission_code, parent_id, order_no, status, required)
SELECT 'TMP-BTN-DOMAIN-MEMBER-CREATE', 'button', 'domain', '添加成员', 'domain.member.create',
       (SELECT id FROM iam_admin_menu WHERE code = 'PLATFORM-DOMAIN-MEMBERS' LIMIT 1),
       1, 1, 0
WHERE NOT EXISTS (
    SELECT 1 FROM iam_admin_menu WHERE permission_code = 'domain.member.create' AND node_type = 'button'
);

INSERT INTO iam_admin_menu (code, node_type, scope, name, permission_code, parent_id, order_no, status, required)
SELECT 'TMP-BTN-DOMAIN-MEMBER-UPDATE-ROLES', 'button', 'domain', '编辑成员角色', 'domain.member.update_roles',
       (SELECT id FROM iam_admin_menu WHERE code = 'PLATFORM-DOMAIN-MEMBERS' LIMIT 1),
       2, 1, 0
WHERE NOT EXISTS (
    SELECT 1 FROM iam_admin_menu WHERE permission_code = 'domain.member.update_roles' AND node_type = 'button'
);

INSERT INTO iam_admin_menu (code, node_type, scope, name, permission_code, parent_id, order_no, status, required)
SELECT 'TMP-BTN-DOMAIN-MEMBER-UPDATE-STATUS', 'button', 'domain', '启停成员', 'domain.member.update_status',
       (SELECT id FROM iam_admin_menu WHERE code = 'PLATFORM-DOMAIN-MEMBERS' LIMIT 1),
       3, 1, 0
WHERE NOT EXISTS (
    SELECT 1 FROM iam_admin_menu WHERE permission_code = 'domain.member.update_status' AND node_type = 'button'
);

INSERT INTO iam_admin_menu (code, node_type, scope, name, permission_code, parent_id, order_no, status, required)
SELECT 'TMP-BTN-DOMAIN-MEMBER-DELETE', 'button', 'domain', '删除成员', 'domain.member.delete',
       (SELECT id FROM iam_admin_menu WHERE code = 'PLATFORM-DOMAIN-MEMBERS' LIMIT 1),
       4, 1, 0
WHERE NOT EXISTS (
    SELECT 1 FROM iam_admin_menu WHERE permission_code = 'domain.member.delete' AND node_type = 'button'
);

UPDATE iam_admin_menu
SET code = CONCAT('ADM', LPAD(id, 10, '0'))
WHERE code IN (
    'TMP-BTN-DOMAIN-MEMBER-READ',
    'TMP-BTN-DOMAIN-MEMBER-CREATE',
    'TMP-BTN-DOMAIN-MEMBER-UPDATE-ROLES',
    'TMP-BTN-DOMAIN-MEMBER-UPDATE-STATUS',
    'TMP-BTN-DOMAIN-MEMBER-DELETE'
);

INSERT IGNORE INTO iam_role_permission (role_id, permission_id, created_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP(3)
FROM role r
JOIN iam_permission p ON p.code IN (
    'domain.member.read',
    'domain.member.create',
    'domain.member.update_roles',
    'domain.member.update_status',
    'domain.member.delete'
) AND p.status = 1
WHERE r.code = 'super_admin';

INSERT IGNORE INTO iam_admin_role_menu_relation (role_id, menu_id, created_at)
SELECT r.id, m.id, CURRENT_TIMESTAMP(3)
FROM role r
JOIN iam_admin_menu m ON m.code = 'PLATFORM-DOMAIN-MEMBERS' AND m.status = 1
WHERE r.code = 'super_admin';

INSERT IGNORE INTO iam_admin_role_menu_relation (role_id, menu_id, created_at)
SELECT r.id, btn.id, CURRENT_TIMESTAMP(3)
FROM role r
JOIN iam_admin_menu btn ON btn.node_type = 'button'
    AND btn.permission_code IN (
        'domain.member.read',
        'domain.member.create',
        'domain.member.update_roles',
        'domain.member.update_status',
        'domain.member.delete'
    )
    AND btn.status = 1
WHERE r.code = 'super_admin';

INSERT IGNORE INTO iam_admin_role_menu_relation (role_id, menu_id, created_at)
SELECT DISTINCT r.id, m.id, CURRENT_TIMESTAMP(3)
FROM role r
JOIN iam_admin_role_menu_relation rel ON rel.role_id = r.id
JOIN iam_admin_menu detail ON detail.id = rel.menu_id
    AND detail.route_path = '/platform/domains/detail'
    AND detail.node_type = 'menu'
    AND detail.status = 1
JOIN iam_admin_menu m ON (
    m.code = 'PLATFORM-DOMAIN-MEMBERS'
    OR m.permission_code IN (
        'domain.member.read',
        'domain.member.create',
        'domain.member.update_roles',
        'domain.member.update_status',
        'domain.member.delete'
    )
)
WHERE m.status = 1;

INSERT IGNORE INTO iam_role_permission (role_id, permission_id, created_at)
SELECT DISTINCT r.id, p.id, CURRENT_TIMESTAMP(3)
FROM role r
JOIN iam_admin_role_menu_relation rel ON rel.role_id = r.id
JOIN iam_admin_menu detail ON detail.id = rel.menu_id
    AND detail.route_path = '/platform/domains/detail'
    AND detail.node_type = 'menu'
    AND detail.status = 1
JOIN iam_permission p ON p.code IN (
    'domain.member.read',
    'domain.member.create',
    'domain.member.update_roles',
    'domain.member.update_status',
    'domain.member.delete'
) AND p.status = 1;
