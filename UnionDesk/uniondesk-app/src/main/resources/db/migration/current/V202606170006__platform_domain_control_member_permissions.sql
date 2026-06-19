-- 员工管理：platform.domain.control.member.*（新增，保留 domain.member.* 供 business 角色）

INSERT INTO iam_permission (
    code, name, description, permission_scope, resource_code, action_code, http_method, path_pattern, status
)
SELECT 'platform.domain.control.member.read', '查看成员', '查看业务域成员列表', 'platform',
       'domain.member', 'platform_domain_control_member_read', 'GET', '/api/v1/admin/domains/*/members', 1
WHERE NOT EXISTS (SELECT 1 FROM iam_permission WHERE code = 'platform.domain.control.member.read');

INSERT INTO iam_permission (
    code, name, description, permission_scope, resource_code, action_code, http_method, path_pattern, status
)
SELECT 'platform.domain.control.member.create', '添加成员', '添加业务域成员（含新建员工）', 'platform',
       'domain.member', 'platform_domain_control_member_create', 'POST', '/api/v1/admin/domains/*/members/**', 1
WHERE NOT EXISTS (SELECT 1 FROM iam_permission WHERE code = 'platform.domain.control.member.create');

INSERT INTO iam_permission (
    code, name, description, permission_scope, resource_code, action_code, http_method, path_pattern, status
)
SELECT 'platform.domain.control.member.update_roles', '编辑成员角色', '更新业务域成员角色', 'platform',
       'domain.member', 'platform_domain_control_member_update_roles', 'PUT', '/api/v1/admin/domains/*/members/*/roles', 1
WHERE NOT EXISTS (SELECT 1 FROM iam_permission WHERE code = 'platform.domain.control.member.update_roles');

INSERT INTO iam_permission (
    code, name, description, permission_scope, resource_code, action_code, http_method, path_pattern, status
)
SELECT 'platform.domain.control.member.update_status', '启停成员', '启用或禁用业务域成员', 'platform',
       'domain.member', 'platform_domain_control_member_update_status', 'PUT', '/api/v1/admin/domains/*/members/*/status', 1
WHERE NOT EXISTS (SELECT 1 FROM iam_permission WHERE code = 'platform.domain.control.member.update_status');

INSERT INTO iam_permission (
    code, name, description, permission_scope, resource_code, action_code, http_method, path_pattern, status
)
SELECT 'platform.domain.control.member.delete', '移除成员', '软删除业务域成员', 'platform',
       'domain.member', 'platform_domain_control_member_delete', 'DELETE', '/api/v1/admin/domains/*/members/*', 1
WHERE NOT EXISTS (SELECT 1 FROM iam_permission WHERE code = 'platform.domain.control.member.delete');

UPDATE iam_admin_menu
SET scope = 'platform',
    updated_at = CURRENT_TIMESTAMP(3)
WHERE code = 'PLATFORM-DOMAIN-MEMBERS';

UPDATE iam_admin_menu
SET permission_code = 'platform.domain.control.member.read',
    scope = 'platform',
    updated_at = CURRENT_TIMESTAMP(3)
WHERE node_type = 'button' AND permission_code = 'domain.member.read';

UPDATE iam_admin_menu
SET permission_code = 'platform.domain.control.member.create',
    scope = 'platform',
    updated_at = CURRENT_TIMESTAMP(3)
WHERE node_type = 'button' AND permission_code = 'domain.member.create';

UPDATE iam_admin_menu
SET permission_code = 'platform.domain.control.member.update_roles',
    scope = 'platform',
    updated_at = CURRENT_TIMESTAMP(3)
WHERE node_type = 'button' AND permission_code = 'domain.member.update_roles';

UPDATE iam_admin_menu
SET permission_code = 'platform.domain.control.member.update_status',
    scope = 'platform',
    updated_at = CURRENT_TIMESTAMP(3)
WHERE node_type = 'button' AND permission_code = 'domain.member.update_status';

UPDATE iam_admin_menu
SET permission_code = 'platform.domain.control.member.delete',
    scope = 'platform',
    updated_at = CURRENT_TIMESTAMP(3)
WHERE node_type = 'button' AND permission_code = 'domain.member.delete';

INSERT IGNORE INTO iam_role_permission (role_id, permission_id, created_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP(3)
FROM role r
JOIN iam_permission p ON p.code IN (
    'platform.domain.control.member.read',
    'platform.domain.control.member.create',
    'platform.domain.control.member.update_roles',
    'platform.domain.control.member.update_status',
    'platform.domain.control.member.delete'
) AND p.status = 1
WHERE r.code IN ('super_admin', 'platform_admin');
