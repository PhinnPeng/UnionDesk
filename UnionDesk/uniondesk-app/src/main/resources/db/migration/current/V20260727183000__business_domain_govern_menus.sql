-- M0：业务域端「域治理」catalog / menu / button 种子 + domain.* 权限补齐 + domain_admin 授权

-- A) domain scope 权限（幂等插入）
INSERT INTO iam_permission (code, name, description, permission_scope, resource_code, action_code, http_method, path_pattern, status)
SELECT v.code, v.name, v.description, 'domain', v.resource_code, v.action_code, v.http_method, v.path_pattern, 1
FROM (
    SELECT 'domain.overview.read' AS code, '查看域概览' AS name, '查看业务域概览数据' AS description, 'domain.overview' AS resource_code, 'read' AS action_code, NULL AS http_method, NULL AS path_pattern
    UNION ALL SELECT 'domain.general.read', '查看通用设置', '查看业务域基础信息', 'domain.general', 'read', 'GET', '/api/v1/admin/domains/*'
    UNION ALL SELECT 'domain.general.update', '更新基础信息', '更新业务域基础信息', 'domain.general', 'update', 'PUT', '/api/v1/admin/domains/*'
    UNION ALL SELECT 'domain.general.update_status', '启停业务域', '更新业务域启用状态', 'domain.general', 'update_status', 'PUT', '/api/v1/admin/domains/*'
    UNION ALL SELECT 'domain.customer.read', '查看客户', '查看业务域客户列表', 'domain.customer', 'read', 'GET', '/api/v1/admin/domains/*/customers'
    UNION ALL SELECT 'domain.customer.create', '添加客户', '添加业务域客户', 'domain.customer', 'create', 'POST', '/api/v1/admin/domains/*/customers/**'
    UNION ALL SELECT 'domain.customer.update_status', '启停客户', '更新业务域客户状态', 'domain.customer', 'update_status', 'PATCH', '/api/v1/admin/domains/*/customers/*/status'
    UNION ALL SELECT 'domain.blocked_word.read', '查看屏蔽词', '查看业务域屏蔽词列表', 'blocked_word', 'read', 'GET', '/api/v1/admin/domains/*/blocked-words'
    UNION ALL SELECT 'domain.blocked_word.create', '添加屏蔽词', '为业务域新增屏蔽词', 'blocked_word', 'create', 'POST', '/api/v1/admin/domains/*/blocked-words'
    UNION ALL SELECT 'domain.blocked_word.delete', '删除屏蔽词', '删除业务域屏蔽词', 'blocked_word', 'delete', 'DELETE', '/api/v1/admin/domains/*/blocked-words/*'
    UNION ALL SELECT 'domain.ticket_attribute.read', '查看事项属性', '查看业务域事项属性', 'ticket_attribute', 'read', 'GET', '/api/v1/admin/domains/*/ticket-attributes'
    UNION ALL SELECT 'domain.ticket_attribute.create', '创建事项属性', '创建业务域事项属性', 'ticket_attribute', 'create', 'POST', '/api/v1/admin/domains/*/ticket-attributes'
    UNION ALL SELECT 'domain.ticket_attribute.update', '编辑事项属性', '更新业务域事项属性', 'ticket_attribute', 'update', 'PUT', '/api/v1/admin/domains/*/ticket-attributes/*'
    UNION ALL SELECT 'domain.ticket_attribute.delete', '删除事项属性', '删除业务域事项属性', 'ticket_attribute', 'delete', 'DELETE', '/api/v1/admin/domains/*/ticket-attributes/*'
    UNION ALL SELECT 'domain.ticket_status.read', '查看事项状态', '查看业务域事项状态', 'ticket_status', 'read', 'GET', '/api/v1/admin/domains/*/ticket-statuses'
    UNION ALL SELECT 'domain.ticket_status.create', '创建事项状态', '创建业务域事项状态', 'ticket_status', 'create', 'POST', '/api/v1/admin/domains/*/ticket-statuses'
    UNION ALL SELECT 'domain.ticket_status.update', '编辑事项状态', '更新业务域事项状态', 'ticket_status', 'update', 'PUT', '/api/v1/admin/domains/*/ticket-statuses/*'
    UNION ALL SELECT 'domain.ticket_status.delete', '删除事项状态', '删除业务域事项状态', 'ticket_status', 'delete', 'DELETE', '/api/v1/admin/domains/*/ticket-statuses/*'
    UNION ALL SELECT 'domain.audit_log.read', '查看操作日志', '查看业务域操作日志', 'audit_log', 'read', 'GET', '/api/v1/admin/domains/*/audit-logs'
    UNION ALL SELECT 'domain.login_log.read', '查看登录日志', '查看业务域登录日志', 'login_log', 'read', 'GET', '/api/v1/admin/domains/*/login-logs'
    UNION ALL SELECT 'domain.member.read', '查看成员', '查看业务域成员列表', 'domain.member', 'domain_member_read', 'GET', '/api/v1/admin/domains/*/members'
    UNION ALL SELECT 'domain.member.create', '添加成员', '添加业务域成员', 'domain.member', 'domain_member_create', 'POST', '/api/v1/admin/domains/*/members/**'
    UNION ALL SELECT 'domain.member.update_roles', '编辑成员角色', '更新业务域成员角色', 'domain.member', 'domain_member_update_roles', 'PUT', '/api/v1/admin/domains/*/members/*/roles'
    UNION ALL SELECT 'domain.member.update_status', '启停成员', '禁用或启用业务域成员', 'domain.member', 'domain_member_update_status', 'PUT', '/api/v1/admin/domains/*/members/*/status'
    UNION ALL SELECT 'domain.member.delete', '移除成员', '软删除业务域成员', 'domain.member', 'domain_member_delete', 'DELETE', '/api/v1/admin/domains/*/members/*'
    UNION ALL SELECT 'domain.role.read', '查看域角色', '查看业务域角色', 'domain.role', 'read', 'GET', '/api/v1/iam/roles'
    UNION ALL SELECT 'domain.role.create', '创建域角色', '创建业务域角色', 'domain.role', 'create', 'POST', '/api/v1/iam/roles'
    UNION ALL SELECT 'domain.role.update', '编辑域角色', '更新业务域角色', 'domain.role', 'update', 'PUT', '/api/v1/iam/roles/*'
    UNION ALL SELECT 'domain.role.delete', '删除域角色', '删除业务域角色', 'domain.role', 'delete', 'DELETE', '/api/v1/iam/roles/*'
    UNION ALL SELECT 'domain.role.permission.read', '查看域角色权限', '查看业务域角色权限', 'domain.role.permission', 'read', 'GET', '/api/v1/iam/roles/*/permissions'
    UNION ALL SELECT 'domain.role.permission.update', '编辑域角色权限', '更新业务域角色权限', 'domain.role.permission', 'update', 'PUT', '/api/v1/iam/roles/*/permissions'
    UNION ALL SELECT 'domain.invitation_code.read', '查看邀请码', '查看业务域邀请码', 'domain.invitation_code', 'read', 'GET', '/api/v1/admin/domains/*/invitation-codes'
    UNION ALL SELECT 'domain.invitation_code.create', '创建邀请码', '创建业务域邀请码', 'domain.invitation_code', 'create', 'POST', '/api/v1/admin/domains/*/invitation-codes'
    UNION ALL SELECT 'domain.invitation_code.delete', '删除邀请码', '删除业务域邀请码', 'domain.invitation_code', 'delete', 'DELETE', '/api/v1/admin/domains/*/invitation-codes/*'
    UNION ALL SELECT 'domain.config.read', '查看域配置', '查看业务域参数配置', 'domain_config', 'read', 'GET', '/api/v1/admin/domains/*/config'
    UNION ALL SELECT 'domain.config.update', '编辑域配置', '更新业务域参数配置', 'domain_config', 'update', 'PUT', '/api/v1/admin/domains/*/config'
    UNION ALL SELECT 'domain.ticket_type.read', '查看事项类型', '查看业务域事项类型', 'ticket_type', 'read', 'GET', '/api/v1/admin/domains/*/ticket-types'
    UNION ALL SELECT 'domain.ticket_type.create', '创建事项类型', '创建业务域事项类型', 'ticket_type', 'create', 'POST', '/api/v1/admin/domains/*/ticket-types'
    UNION ALL SELECT 'domain.ticket_type.update', '编辑事项类型', '更新业务域事项类型', 'ticket_type', 'update', 'PUT', '/api/v1/admin/domains/*/ticket-types/*'
    UNION ALL SELECT 'domain.ticket_type.delete', '删除事项类型', '删除业务域事项类型', 'ticket_type', 'delete', 'DELETE', '/api/v1/admin/domains/*/ticket-types/*'
    UNION ALL SELECT 'domain.notification_template.read', '查看通知模板', '查看业务域通知模板', 'notification_template', 'read', 'GET', '/api/v1/admin/domains/*/notification-templates'
    UNION ALL SELECT 'domain.notification_template.update', '编辑通知模板', '更新业务域通知模板', 'notification_template', 'update', 'PUT', '/api/v1/admin/domains/*/notification-templates/*'
) v
WHERE NOT EXISTS (SELECT 1 FROM iam_permission p WHERE p.code = v.code);

-- B) 域治理 catalog + menu
INSERT INTO iam_admin_menu (code, node_type, scope, name, route_path, component_key, permission_code, parent_id, order_no, icon, hidden, status, required)
SELECT 'BUSINESS-DOMAIN-GOVERN-CATALOG', 'catalog', 'business', '域治理', NULL, NULL, NULL, NULL, 20, 'SettingOutlined', 0, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-GOVERN-CATALOG');

UPDATE iam_admin_menu
SET name = '域治理',
    scope = 'business',
    node_type = 'catalog',
    order_no = 20,
    icon = 'SettingOutlined',
    hidden = 0,
    status = 1,
    updated_at = CURRENT_TIMESTAMP(3)
WHERE code = 'BUSINESS-DOMAIN-GOVERN-CATALOG';

INSERT INTO iam_admin_menu (code, node_type, scope, name, route_path, component_key, permission_code, parent_id, order_no, icon, hidden, status, required)
SELECT 'BUSINESS-DOMAIN-OVERVIEW', 'menu', 'business', '概览', '/domain/overview', './domain/overview', NULL,
       (SELECT id FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-GOVERN-CATALOG' LIMIT 1),
       1, NULL, 0, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-OVERVIEW');

INSERT INTO iam_admin_menu (code, node_type, scope, name, route_path, component_key, permission_code, parent_id, order_no, icon, hidden, status, required)
SELECT 'BUSINESS-DOMAIN-BASIC', 'menu', 'business', '通用设置', '/domain/basic', './domain/basic', NULL,
       (SELECT id FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-GOVERN-CATALOG' LIMIT 1),
       2, NULL, 0, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-BASIC');

INSERT INTO iam_admin_menu (code, node_type, scope, name, route_path, component_key, permission_code, parent_id, order_no, icon, hidden, status, required)
SELECT 'BUSINESS-DOMAIN-MEMBERS', 'menu', 'business', '人员管理', '/domain/members', './domain/members', NULL,
       (SELECT id FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-GOVERN-CATALOG' LIMIT 1),
       3, NULL, 0, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-MEMBERS');

INSERT INTO iam_admin_menu (code, node_type, scope, name, route_path, component_key, permission_code, parent_id, order_no, icon, hidden, status, required)
SELECT 'BUSINESS-DOMAIN-ROLES', 'menu', 'business', '角色管理', '/domain/roles', './domain/roles', NULL,
       (SELECT id FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-GOVERN-CATALOG' LIMIT 1),
       4, NULL, 0, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-ROLES');

INSERT INTO iam_admin_menu (code, node_type, scope, name, route_path, component_key, permission_code, parent_id, order_no, icon, hidden, status, required)
SELECT 'BUSINESS-DOMAIN-CUSTOMERS', 'menu', 'business', '客户管理', '/domain/customers', './domain/customers', NULL,
       (SELECT id FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-GOVERN-CATALOG' LIMIT 1),
       5, NULL, 0, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-CUSTOMERS');

INSERT INTO iam_admin_menu (code, node_type, scope, name, route_path, component_key, permission_code, parent_id, order_no, icon, hidden, status, required)
SELECT 'BUSINESS-DOMAIN-ONBOARDING', 'menu', 'business', '入域管理', '/domain/onboarding', './domain/onboarding', NULL,
       (SELECT id FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-GOVERN-CATALOG' LIMIT 1),
       6, NULL, 0, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-ONBOARDING');

INSERT INTO iam_admin_menu (code, node_type, scope, name, route_path, component_key, permission_code, parent_id, order_no, icon, hidden, status, required)
SELECT 'BUSINESS-DOMAIN-TICKET-CONFIG', 'menu', 'business', '事项配置', '/domain/ticket-config', './domain/ticket-config', NULL,
       (SELECT id FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-GOVERN-CATALOG' LIMIT 1),
       7, NULL, 0, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-TICKET-CONFIG');

INSERT INTO iam_admin_menu (code, node_type, scope, name, route_path, component_key, permission_code, parent_id, order_no, icon, hidden, status, required)
SELECT 'BUSINESS-DOMAIN-BLOCKWORDS', 'menu', 'business', '屏蔽词库', '/domain/blockwords', './domain/blockwords', NULL,
       (SELECT id FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-GOVERN-CATALOG' LIMIT 1),
       8, NULL, 0, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-BLOCKWORDS');

INSERT INTO iam_admin_menu (code, node_type, scope, name, route_path, component_key, permission_code, parent_id, order_no, icon, hidden, status, required)
SELECT 'BUSINESS-DOMAIN-NOTIFICATIONS', 'menu', 'business', '通知配置', '/domain/notifications', './domain/notifications', NULL,
       (SELECT id FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-GOVERN-CATALOG' LIMIT 1),
       9, NULL, 0, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-NOTIFICATIONS');

INSERT INTO iam_admin_menu (code, node_type, scope, name, route_path, component_key, permission_code, parent_id, order_no, icon, hidden, status, required)
SELECT 'BUSINESS-DOMAIN-CONFIG', 'menu', 'business', '参数配置', '/domain/config', './domain/config', NULL,
       (SELECT id FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-GOVERN-CATALOG' LIMIT 1),
       10, NULL, 0, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-CONFIG');

INSERT INTO iam_admin_menu (code, node_type, scope, name, route_path, component_key, permission_code, parent_id, order_no, icon, hidden, status, required)
SELECT 'BUSINESS-DOMAIN-AUDIT-LOGS', 'menu', 'business', '操作日志', '/domain/audit-logs', './domain/audit-logs', NULL,
       (SELECT id FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-GOVERN-CATALOG' LIMIT 1),
       11, NULL, 0, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-AUDIT-LOGS');

INSERT INTO iam_admin_menu (code, node_type, scope, name, route_path, component_key, permission_code, parent_id, order_no, icon, hidden, status, required)
SELECT 'BUSINESS-DOMAIN-LOGIN-LOGS', 'menu', 'business', '登录日志', '/domain/login-logs', './domain/login-logs', NULL,
       (SELECT id FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-GOVERN-CATALOG' LIMIT 1),
       12, NULL, 0, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-LOGIN-LOGS');

-- C) 按钮（design.md §3）
INSERT INTO iam_admin_menu (code, node_type, scope, name, permission_code, parent_id, order_no, status, required)
SELECT 'BUSINESS-DOMAIN-OVERVIEW-READ', 'button', 'business', '查看概览', 'domain.overview.read',
       (SELECT id FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-OVERVIEW' LIMIT 1), 1, 1, 1
WHERE NOT EXISTS (SELECT 1 FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-OVERVIEW-READ');

INSERT INTO iam_admin_menu (code, node_type, scope, name, permission_code, parent_id, order_no, status, required)
SELECT 'BUSINESS-DOMAIN-BASIC-READ', 'button', 'business', '查看通用设置', 'domain.general.read',
       (SELECT id FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-BASIC' LIMIT 1), 1, 1, 1
WHERE NOT EXISTS (SELECT 1 FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-BASIC-READ');

INSERT INTO iam_admin_menu (code, node_type, scope, name, permission_code, parent_id, order_no, status, required)
SELECT 'BUSINESS-DOMAIN-BASIC-UPDATE', 'button', 'business', '更新基础信息', 'domain.general.update',
       (SELECT id FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-BASIC' LIMIT 1), 2, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-BASIC-UPDATE');

INSERT INTO iam_admin_menu (code, node_type, scope, name, permission_code, parent_id, order_no, status, required)
SELECT 'BUSINESS-DOMAIN-BASIC-STATUS', 'button', 'business', '启停业务域', 'domain.general.update_status',
       (SELECT id FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-BASIC' LIMIT 1), 3, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-BASIC-STATUS');

INSERT INTO iam_admin_menu (code, node_type, scope, name, permission_code, parent_id, order_no, status, required)
SELECT 'BUSINESS-DOMAIN-MEMBERS-READ', 'button', 'business', '查看成员', 'domain.member.read',
       (SELECT id FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-MEMBERS' LIMIT 1), 1, 1, 1
WHERE NOT EXISTS (SELECT 1 FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-MEMBERS-READ');

INSERT INTO iam_admin_menu (code, node_type, scope, name, permission_code, parent_id, order_no, status, required)
SELECT 'BUSINESS-DOMAIN-MEMBERS-CREATE', 'button', 'business', '添加成员', 'domain.member.create',
       (SELECT id FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-MEMBERS' LIMIT 1), 2, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-MEMBERS-CREATE');

INSERT INTO iam_admin_menu (code, node_type, scope, name, permission_code, parent_id, order_no, status, required)
SELECT 'BUSINESS-DOMAIN-MEMBERS-UPDATE-ROLES', 'button', 'business', '编辑成员角色', 'domain.member.update_roles',
       (SELECT id FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-MEMBERS' LIMIT 1), 3, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-MEMBERS-UPDATE-ROLES');

INSERT INTO iam_admin_menu (code, node_type, scope, name, permission_code, parent_id, order_no, status, required)
SELECT 'BUSINESS-DOMAIN-MEMBERS-UPDATE-STATUS', 'button', 'business', '启停成员', 'domain.member.update_status',
       (SELECT id FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-MEMBERS' LIMIT 1), 4, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-MEMBERS-UPDATE-STATUS');

INSERT INTO iam_admin_menu (code, node_type, scope, name, permission_code, parent_id, order_no, status, required)
SELECT 'BUSINESS-DOMAIN-MEMBERS-DELETE', 'button', 'business', '移除成员', 'domain.member.delete',
       (SELECT id FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-MEMBERS' LIMIT 1), 5, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-MEMBERS-DELETE');

INSERT INTO iam_admin_menu (code, node_type, scope, name, permission_code, parent_id, order_no, status, required)
SELECT 'BUSINESS-DOMAIN-ROLES-READ', 'button', 'business', '查看域角色', 'domain.role.read',
       (SELECT id FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-ROLES' LIMIT 1), 1, 1, 1
WHERE NOT EXISTS (SELECT 1 FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-ROLES-READ');

INSERT INTO iam_admin_menu (code, node_type, scope, name, permission_code, parent_id, order_no, status, required)
SELECT 'BUSINESS-DOMAIN-ROLES-CREATE', 'button', 'business', '创建域角色', 'domain.role.create',
       (SELECT id FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-ROLES' LIMIT 1), 2, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-ROLES-CREATE');

INSERT INTO iam_admin_menu (code, node_type, scope, name, permission_code, parent_id, order_no, status, required)
SELECT 'BUSINESS-DOMAIN-ROLES-UPDATE', 'button', 'business', '编辑域角色', 'domain.role.update',
       (SELECT id FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-ROLES' LIMIT 1), 3, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-ROLES-UPDATE');

INSERT INTO iam_admin_menu (code, node_type, scope, name, permission_code, parent_id, order_no, status, required)
SELECT 'BUSINESS-DOMAIN-ROLES-DELETE', 'button', 'business', '删除域角色', 'domain.role.delete',
       (SELECT id FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-ROLES' LIMIT 1), 4, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-ROLES-DELETE');

INSERT INTO iam_admin_menu (code, node_type, scope, name, permission_code, parent_id, order_no, status, required)
SELECT 'BUSINESS-DOMAIN-ROLES-PERM-READ', 'button', 'business', '查看角色权限', 'domain.role.permission.read',
       (SELECT id FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-ROLES' LIMIT 1), 5, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-ROLES-PERM-READ');

INSERT INTO iam_admin_menu (code, node_type, scope, name, permission_code, parent_id, order_no, status, required)
SELECT 'BUSINESS-DOMAIN-ROLES-PERM-UPDATE', 'button', 'business', '编辑角色权限', 'domain.role.permission.update',
       (SELECT id FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-ROLES' LIMIT 1), 6, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-ROLES-PERM-UPDATE');

INSERT INTO iam_admin_menu (code, node_type, scope, name, permission_code, parent_id, order_no, status, required)
SELECT 'BUSINESS-DOMAIN-CUSTOMERS-READ', 'button', 'business', '查看客户', 'domain.customer.read',
       (SELECT id FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-CUSTOMERS' LIMIT 1), 1, 1, 1
WHERE NOT EXISTS (SELECT 1 FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-CUSTOMERS-READ');

INSERT INTO iam_admin_menu (code, node_type, scope, name, permission_code, parent_id, order_no, status, required)
SELECT 'BUSINESS-DOMAIN-CUSTOMERS-CREATE', 'button', 'business', '添加客户', 'domain.customer.create',
       (SELECT id FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-CUSTOMERS' LIMIT 1), 2, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-CUSTOMERS-CREATE');

INSERT INTO iam_admin_menu (code, node_type, scope, name, permission_code, parent_id, order_no, status, required)
SELECT 'BUSINESS-DOMAIN-CUSTOMERS-UPDATE-STATUS', 'button', 'business', '启停客户', 'domain.customer.update_status',
       (SELECT id FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-CUSTOMERS' LIMIT 1), 3, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-CUSTOMERS-UPDATE-STATUS');

INSERT INTO iam_admin_menu (code, node_type, scope, name, permission_code, parent_id, order_no, status, required)
SELECT 'BUSINESS-DOMAIN-ONBOARDING-READ', 'button', 'business', '查看入域配置', 'domain.invitation_code.read',
       (SELECT id FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-ONBOARDING' LIMIT 1), 1, 1, 1
WHERE NOT EXISTS (SELECT 1 FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-ONBOARDING-READ');

INSERT INTO iam_admin_menu (code, node_type, scope, name, permission_code, parent_id, order_no, status, required)
SELECT 'BUSINESS-DOMAIN-ONBOARDING-CREATE', 'button', 'business', '创建邀请码', 'domain.invitation_code.create',
       (SELECT id FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-ONBOARDING' LIMIT 1), 2, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-ONBOARDING-CREATE');

INSERT INTO iam_admin_menu (code, node_type, scope, name, permission_code, parent_id, order_no, status, required)
SELECT 'BUSINESS-DOMAIN-ONBOARDING-DELETE', 'button', 'business', '删除邀请码', 'domain.invitation_code.delete',
       (SELECT id FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-ONBOARDING' LIMIT 1), 3, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-ONBOARDING-DELETE');

-- 入域策略更新复用 domain.general.update（uk_iam_admin_menu_permission_code 唯一，不另建按钮）

INSERT INTO iam_admin_menu (code, node_type, scope, name, permission_code, parent_id, order_no, status, required)
SELECT 'BUSINESS-DOMAIN-TICKET-CONFIG-TYPE-READ', 'button', 'business', '查看事项类型', 'domain.ticket_type.read',
       (SELECT id FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-TICKET-CONFIG' LIMIT 1), 1, 1, 1
WHERE NOT EXISTS (SELECT 1 FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-TICKET-CONFIG-TYPE-READ');

INSERT INTO iam_admin_menu (code, node_type, scope, name, permission_code, parent_id, order_no, status, required)
SELECT 'BUSINESS-DOMAIN-TICKET-CONFIG-TYPE-CREATE', 'button', 'business', '创建事项类型', 'domain.ticket_type.create',
       (SELECT id FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-TICKET-CONFIG' LIMIT 1), 2, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-TICKET-CONFIG-TYPE-CREATE');

INSERT INTO iam_admin_menu (code, node_type, scope, name, permission_code, parent_id, order_no, status, required)
SELECT 'BUSINESS-DOMAIN-TICKET-CONFIG-TYPE-UPDATE', 'button', 'business', '编辑事项类型', 'domain.ticket_type.update',
       (SELECT id FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-TICKET-CONFIG' LIMIT 1), 3, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-TICKET-CONFIG-TYPE-UPDATE');

INSERT INTO iam_admin_menu (code, node_type, scope, name, permission_code, parent_id, order_no, status, required)
SELECT 'BUSINESS-DOMAIN-TICKET-CONFIG-TYPE-DELETE', 'button', 'business', '删除事项类型', 'domain.ticket_type.delete',
       (SELECT id FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-TICKET-CONFIG' LIMIT 1), 4, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-TICKET-CONFIG-TYPE-DELETE');

INSERT INTO iam_admin_menu (code, node_type, scope, name, permission_code, parent_id, order_no, status, required)
SELECT 'BUSINESS-DOMAIN-TICKET-CONFIG-ATTR-READ', 'button', 'business', '查看事项属性', 'domain.ticket_attribute.read',
       (SELECT id FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-TICKET-CONFIG' LIMIT 1), 5, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-TICKET-CONFIG-ATTR-READ');

INSERT INTO iam_admin_menu (code, node_type, scope, name, permission_code, parent_id, order_no, status, required)
SELECT 'BUSINESS-DOMAIN-TICKET-CONFIG-ATTR-CREATE', 'button', 'business', '创建事项属性', 'domain.ticket_attribute.create',
       (SELECT id FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-TICKET-CONFIG' LIMIT 1), 6, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-TICKET-CONFIG-ATTR-CREATE');

INSERT INTO iam_admin_menu (code, node_type, scope, name, permission_code, parent_id, order_no, status, required)
SELECT 'BUSINESS-DOMAIN-TICKET-CONFIG-ATTR-UPDATE', 'button', 'business', '编辑事项属性', 'domain.ticket_attribute.update',
       (SELECT id FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-TICKET-CONFIG' LIMIT 1), 7, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-TICKET-CONFIG-ATTR-UPDATE');

INSERT INTO iam_admin_menu (code, node_type, scope, name, permission_code, parent_id, order_no, status, required)
SELECT 'BUSINESS-DOMAIN-TICKET-CONFIG-ATTR-DELETE', 'button', 'business', '删除事项属性', 'domain.ticket_attribute.delete',
       (SELECT id FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-TICKET-CONFIG' LIMIT 1), 8, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-TICKET-CONFIG-ATTR-DELETE');

INSERT INTO iam_admin_menu (code, node_type, scope, name, permission_code, parent_id, order_no, status, required)
SELECT 'BUSINESS-DOMAIN-TICKET-CONFIG-STATUS-READ', 'button', 'business', '查看事项状态', 'domain.ticket_status.read',
       (SELECT id FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-TICKET-CONFIG' LIMIT 1), 9, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-TICKET-CONFIG-STATUS-READ');

INSERT INTO iam_admin_menu (code, node_type, scope, name, permission_code, parent_id, order_no, status, required)
SELECT 'BUSINESS-DOMAIN-TICKET-CONFIG-STATUS-CREATE', 'button', 'business', '创建事项状态', 'domain.ticket_status.create',
       (SELECT id FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-TICKET-CONFIG' LIMIT 1), 10, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-TICKET-CONFIG-STATUS-CREATE');

INSERT INTO iam_admin_menu (code, node_type, scope, name, permission_code, parent_id, order_no, status, required)
SELECT 'BUSINESS-DOMAIN-TICKET-CONFIG-STATUS-UPDATE', 'button', 'business', '编辑事项状态', 'domain.ticket_status.update',
       (SELECT id FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-TICKET-CONFIG' LIMIT 1), 11, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-TICKET-CONFIG-STATUS-UPDATE');

INSERT INTO iam_admin_menu (code, node_type, scope, name, permission_code, parent_id, order_no, status, required)
SELECT 'BUSINESS-DOMAIN-TICKET-CONFIG-STATUS-DELETE', 'button', 'business', '删除事项状态', 'domain.ticket_status.delete',
       (SELECT id FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-TICKET-CONFIG' LIMIT 1), 12, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-TICKET-CONFIG-STATUS-DELETE');

INSERT INTO iam_admin_menu (code, node_type, scope, name, permission_code, parent_id, order_no, status, required)
SELECT 'BUSINESS-DOMAIN-BLOCKWORDS-READ', 'button', 'business', '查看屏蔽词', 'domain.blocked_word.read',
       (SELECT id FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-BLOCKWORDS' LIMIT 1), 1, 1, 1
WHERE NOT EXISTS (SELECT 1 FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-BLOCKWORDS-READ');

INSERT INTO iam_admin_menu (code, node_type, scope, name, permission_code, parent_id, order_no, status, required)
SELECT 'BUSINESS-DOMAIN-BLOCKWORDS-CREATE', 'button', 'business', '添加屏蔽词', 'domain.blocked_word.create',
       (SELECT id FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-BLOCKWORDS' LIMIT 1), 2, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-BLOCKWORDS-CREATE');

INSERT INTO iam_admin_menu (code, node_type, scope, name, permission_code, parent_id, order_no, status, required)
SELECT 'BUSINESS-DOMAIN-BLOCKWORDS-DELETE', 'button', 'business', '删除屏蔽词', 'domain.blocked_word.delete',
       (SELECT id FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-BLOCKWORDS' LIMIT 1), 3, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-BLOCKWORDS-DELETE');

INSERT INTO iam_admin_menu (code, node_type, scope, name, permission_code, parent_id, order_no, status, required)
SELECT 'BUSINESS-DOMAIN-NOTIFICATIONS-READ', 'button', 'business', '查看通知配置', 'domain.notification_template.read',
       (SELECT id FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-NOTIFICATIONS' LIMIT 1), 1, 1, 1
WHERE NOT EXISTS (SELECT 1 FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-NOTIFICATIONS-READ');

INSERT INTO iam_admin_menu (code, node_type, scope, name, permission_code, parent_id, order_no, status, required)
SELECT 'BUSINESS-DOMAIN-NOTIFICATIONS-UPDATE', 'button', 'business', '编辑通知配置', 'domain.notification_template.update',
       (SELECT id FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-NOTIFICATIONS' LIMIT 1), 2, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-NOTIFICATIONS-UPDATE');

INSERT INTO iam_admin_menu (code, node_type, scope, name, permission_code, parent_id, order_no, status, required)
SELECT 'BUSINESS-DOMAIN-CONFIG-READ', 'button', 'business', '查看参数配置', 'domain.config.read',
       (SELECT id FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-CONFIG' LIMIT 1), 1, 1, 1
WHERE NOT EXISTS (SELECT 1 FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-CONFIG-READ');

INSERT INTO iam_admin_menu (code, node_type, scope, name, permission_code, parent_id, order_no, status, required)
SELECT 'BUSINESS-DOMAIN-CONFIG-UPDATE', 'button', 'business', '编辑参数配置', 'domain.config.update',
       (SELECT id FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-CONFIG' LIMIT 1), 2, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-CONFIG-UPDATE');

INSERT INTO iam_admin_menu (code, node_type, scope, name, permission_code, parent_id, order_no, status, required)
SELECT 'BUSINESS-DOMAIN-AUDIT-LOGS-READ', 'button', 'business', '查看操作日志', 'domain.audit_log.read',
       (SELECT id FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-AUDIT-LOGS' LIMIT 1), 1, 1, 1
WHERE NOT EXISTS (SELECT 1 FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-AUDIT-LOGS-READ');

INSERT INTO iam_admin_menu (code, node_type, scope, name, permission_code, parent_id, order_no, status, required)
SELECT 'BUSINESS-DOMAIN-LOGIN-LOGS-READ', 'button', 'business', '查看登录日志', 'domain.login_log.read',
       (SELECT id FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-LOGIN-LOGS' LIMIT 1), 1, 1, 1
WHERE NOT EXISTS (SELECT 1 FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-LOGIN-LOGS-READ');

-- D) domain_admin 菜单绑定
INSERT IGNORE INTO iam_admin_role_menu_relation (role_id, menu_id, created_at)
SELECT r.id, m.id, CURRENT_TIMESTAMP(3)
FROM role r
JOIN iam_admin_menu m ON m.status = 1 AND m.scope = 'business'
WHERE r.code = 'domain_admin'
  AND (
      m.code = 'BUSINESS-DOMAIN-GOVERN-CATALOG'
      OR m.code LIKE 'BUSINESS-DOMAIN-%'
  );

-- E) domain_admin 权限绑定
INSERT INTO iam_role_permission (role_id, permission_id, created_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP(3)
FROM role r
JOIN iam_permission p ON p.status = 1
WHERE r.code = 'domain_admin'
  AND p.code IN (
      'domain.overview.read',
      'domain.general.read', 'domain.general.update', 'domain.general.update_status',
      'domain.customer.read', 'domain.customer.create', 'domain.customer.update_status',
      'domain.blocked_word.read', 'domain.blocked_word.create', 'domain.blocked_word.delete',
      'domain.ticket_attribute.read', 'domain.ticket_attribute.create', 'domain.ticket_attribute.update', 'domain.ticket_attribute.delete',
      'domain.ticket_status.read', 'domain.ticket_status.create', 'domain.ticket_status.update', 'domain.ticket_status.delete',
      'domain.audit_log.read', 'domain.login_log.read',
      'domain.member.read', 'domain.member.create', 'domain.member.update_roles', 'domain.member.update_status', 'domain.member.delete',
      'domain.role.read', 'domain.role.create', 'domain.role.update', 'domain.role.delete',
      'domain.role.permission.read', 'domain.role.permission.update',
      'domain.invitation_code.read', 'domain.invitation_code.create', 'domain.invitation_code.delete',
      'domain.config.read', 'domain.config.update',
      'domain.ticket_type.read', 'domain.ticket_type.create', 'domain.ticket_type.update', 'domain.ticket_type.delete',
      'domain.notification_template.read', 'domain.notification_template.update'
  )
  AND NOT EXISTS (
      SELECT 1 FROM iam_role_permission existing
      WHERE existing.role_id = r.id AND existing.permission_id = p.id
  );
