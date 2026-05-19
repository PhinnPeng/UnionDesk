UPDATE role
SET name = CASE code
    WHEN 'super_admin' THEN '超级管理员'
    WHEN 'domain_admin' THEN '业务域管理员'
    WHEN 'agent' THEN '客服专员'
    WHEN 'customer' THEN '客户用户'
    ELSE name
END
WHERE code IN ('super_admin', 'domain_admin', 'agent', 'customer');

INSERT INTO role (code, name, scope, is_system)
VALUES
    ('platform_admin', '平台管理员', 'global', 1),
    ('security_auditor', '安全审计员', 'global', 1)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    scope = VALUES(scope),
    is_system = VALUES(is_system),
    updated_at = CURRENT_TIMESTAMP(3);

CREATE TABLE IF NOT EXISTS iam_permission (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    code VARCHAR(128) NOT NULL,
    name VARCHAR(128) NOT NULL,
    description VARCHAR(255) NOT NULL,
    permission_scope VARCHAR(16) NOT NULL,
    resource_code VARCHAR(128) NOT NULL,
    action_code VARCHAR(64) NOT NULL,
    http_method VARCHAR(16) DEFAULT NULL,
    path_pattern VARCHAR(255) DEFAULT NULL,
    status TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_iam_permission_code (code),
    KEY idx_iam_permission_scope_status (permission_scope, status),
    KEY idx_iam_permission_route (http_method, path_pattern),
    CONSTRAINT chk_iam_permission_scope CHECK (permission_scope IN ('platform', 'domain')),
    CONSTRAINT chk_iam_permission_route CHECK (
        (http_method IS NULL AND path_pattern IS NULL)
        OR (http_method IS NOT NULL AND path_pattern IS NOT NULL)
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限元数据';

CREATE TABLE IF NOT EXISTS iam_role_permission (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    role_id INT UNSIGNED NOT NULL,
    permission_id BIGINT UNSIGNED NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_iam_role_permission (role_id, permission_id),
    KEY idx_iam_role_permission_permission (permission_id),
    CONSTRAINT fk_iam_role_permission_role FOREIGN KEY (role_id) REFERENCES role(id),
    CONSTRAINT fk_iam_role_permission_permission FOREIGN KEY (permission_id) REFERENCES iam_permission(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色权限关系';

CREATE TABLE IF NOT EXISTS iam_role_binding (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    role_id INT UNSIGNED NOT NULL,
    binding_scope VARCHAR(16) NOT NULL,
    business_domain_id BIGINT UNSIGNED DEFAULT NULL,
    business_domain_key BIGINT UNSIGNED GENERATED ALWAYS AS (COALESCE(business_domain_id, 0)) STORED,
    granted_by BIGINT UNSIGNED DEFAULT NULL,
    effective_from DATETIME(3) DEFAULT NULL,
    effective_to DATETIME(3) DEFAULT NULL,
    status TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_iam_role_binding_scope (user_id, role_id, binding_scope, business_domain_key),
    KEY idx_iam_role_binding_user_status (user_id, status),
    KEY idx_iam_role_binding_role (role_id),
    KEY idx_iam_role_binding_domain (business_domain_id),
    CONSTRAINT fk_iam_role_binding_user FOREIGN KEY (user_id) REFERENCES user_account(id),
    CONSTRAINT fk_iam_role_binding_role FOREIGN KEY (role_id) REFERENCES role(id),
    CONSTRAINT fk_iam_role_binding_domain FOREIGN KEY (business_domain_id) REFERENCES business_domain(id),
    CONSTRAINT fk_iam_role_binding_granted_by FOREIGN KEY (granted_by) REFERENCES user_account(id),
    CONSTRAINT chk_iam_role_binding_scope CHECK (binding_scope IN ('global', 'domain')),
    CONSTRAINT chk_iam_role_binding_domain CHECK (
        (binding_scope = 'global' AND business_domain_id IS NULL)
        OR (binding_scope = 'domain' AND business_domain_id IS NOT NULL)
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='统一角色绑定';

INSERT INTO iam_permission (
    code, name, description, permission_scope, resource_code, action_code, http_method, path_pattern, status
)
VALUES
    ('platform.menu.read', '查看菜单', '查看管理端菜单树', 'platform', 'platform.menu', 'read', 'GET', '/api/v1/iam/menus/tree', 1),
    ('platform.menu.create', '创建菜单', '创建管理端菜单或按钮', 'platform', 'platform.menu', 'create', 'POST', '/api/v1/iam/menus', 1),
    ('platform.menu.update', '更新菜单', '更新管理端菜单或按钮', 'platform', 'platform.menu', 'update', 'PUT', '/api/v1/iam/menus/*', 1),
    ('platform.menu.delete', '删除菜单', '删除管理端菜单或按钮', 'platform', 'platform.menu', 'delete', 'DELETE', '/api/v1/iam/menus/*', 1),
    ('platform.role.read', '查看角色', '查看平台角色列表', 'platform', 'platform.role', 'read', 'GET', '/api/v1/iam/roles', 1),
    ('platform.role.create', '创建角色', '创建平台或业务域角色', 'platform', 'platform.role', 'create', 'POST', '/api/v1/iam/roles', 1),
    ('platform.role.update', '更新角色', '更新角色基础信息', 'platform', 'platform.role', 'update', 'PUT', '/api/v1/iam/roles/*', 1),
    ('platform.role.delete', '删除角色', '删除非系统角色', 'platform', 'platform.role', 'delete', 'DELETE', '/api/v1/iam/roles/*', 1),
    ('platform.role_permission.read', '查看角色授权', '查看角色拥有的菜单与按钮权限', 'platform', 'platform.role_permission', 'read', 'GET', '/api/v1/iam/roles/*/permissions', 1),
    ('platform.role_permission.update', '更新角色授权', '更新角色拥有的菜单与按钮权限', 'platform', 'platform.role_permission', 'update', 'PUT', '/api/v1/iam/roles/*/permissions', 1),
    ('platform.role.bind', '绑定平台角色', '为用户绑定平台级角色', 'platform', 'platform.role', 'bind', NULL, NULL, 1),
    ('platform.user.read', '查看平台用户', '查看平台用户与系统用户', 'platform', 'platform.user', 'read', 'GET', '/api/v1/iam/users', 1),
    ('platform.user.create', '创建平台用户', '创建带有平台级角色的用户', 'platform', 'platform.user', 'create', 'POST', '/api/v1/iam/users', 1),
    ('platform.user.update', '更新平台用户', '更新平台用户资料与状态', 'platform', 'platform.user', 'update', 'PUT', '/api/v1/iam/users/*', 1),
    ('platform.user.disable', '停用平台用户', '办理平台用户离职或停用', 'platform', 'platform.user', 'disable', 'POST', '/api/v1/iam/users/*/offboard', 1),
    ('platform.user.restore', '恢复平台用户', '恢复已离职或停用的平台用户', 'platform', 'platform.user', 'restore', 'POST', '/api/v1/iam/users/*/restore', 1),
    ('platform.user.offboard_pool.read', '查看离职池', '查看离职用户池', 'platform', 'platform.user', 'read_offboard_pool', 'GET', '/api/v1/iam/users/offboard-pool', 1),
    ('platform.user.delete', '删除平台用户', '彻底删除离职用户', 'platform', 'platform.user', 'delete', 'DELETE', '/api/v1/iam/users/*', 1),
    ('platform.permission.manage', '管理权限目录', '维护权限目录与授权基础数据', 'platform', 'platform.permission', 'manage', NULL, NULL, 1),
    ('domain.user.read', '查看域成员', '查看业务域成员', 'domain', 'domain.user', 'read', 'GET', '/api/v1/iam/users', 1),
    ('domain.user.create', '创建域用户', '在业务域内创建或邀请成员', 'domain', 'domain.user', 'create', 'POST', '/api/v1/iam/users', 1),
    ('domain.user.update', '更新域成员', '更新业务域成员资料与域内角色', 'domain', 'domain.user', 'update', NULL, NULL, 1),
    ('domain.user.remove', '移除域成员', '从业务域内移除成员', 'domain', 'domain.user', 'remove', NULL, NULL, 1),
    ('domain.sla.update', '更新业务域 SLA', '更新业务域 SLA 规则', 'domain', 'domain.sla', 'update', NULL, NULL, 1),
    ('domain.notification_template.update', '更新通知模板', '更新业务域通知模板', 'domain', 'domain.notification_template', 'update', NULL, NULL, 1),
    ('ticket.read', '查看工单', '查看业务域内工单', 'domain', 'ticket', 'read', 'GET', '/api/v1/tickets', 1),
    ('ticket.assign', '分配工单', '分配业务域内工单', 'domain', 'ticket', 'assign', NULL, NULL, 1),
    ('ticket.close', '关闭工单', '关闭业务域内工单', 'domain', 'ticket', 'close', NULL, NULL, 1),
    ('consultation.reply', '回复咨询', '回复业务域内咨询会话', 'domain', 'consultation', 'reply', 'POST', '/api/v1/consultations/*/messages', 1)
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

INSERT INTO iam_role_binding (user_id, role_id, binding_scope, business_domain_id, status)
SELECT user_id, role_id, 'global', NULL, 1
FROM user_global_role
ON DUPLICATE KEY UPDATE
    status = VALUES(status),
    updated_at = CURRENT_TIMESTAMP(3);

INSERT INTO iam_role_binding (user_id, role_id, binding_scope, business_domain_id, status)
SELECT user_id, role_id, 'domain', business_domain_id, 1
FROM user_domain_role
ON DUPLICATE KEY UPDATE
    status = VALUES(status),
    updated_at = CURRENT_TIMESTAMP(3);

INSERT INTO iam_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
JOIN iam_permission p
WHERE r.code = 'super_admin'
ON DUPLICATE KEY UPDATE
    role_id = VALUES(role_id),
    permission_id = VALUES(permission_id);

INSERT INTO iam_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
JOIN iam_permission p ON p.permission_scope = 'platform'
WHERE r.code = 'platform_admin'
ON DUPLICATE KEY UPDATE
    role_id = VALUES(role_id),
    permission_id = VALUES(permission_id);

INSERT INTO iam_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
JOIN iam_permission p ON p.code IN (
    'domain.user.read',
    'domain.user.create',
    'domain.user.update',
    'domain.user.remove'
)
WHERE r.code = 'platform_admin'
ON DUPLICATE KEY UPDATE
    role_id = VALUES(role_id),
    permission_id = VALUES(permission_id);

INSERT INTO iam_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
JOIN iam_permission p ON p.code IN (
    'platform.menu.read',
    'platform.role.read',
    'platform.role_permission.read',
    'platform.user.read',
    'platform.user.offboard_pool.read'
)
WHERE r.code = 'security_auditor'
ON DUPLICATE KEY UPDATE
    role_id = VALUES(role_id),
    permission_id = VALUES(permission_id);

INSERT INTO iam_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
JOIN iam_permission p ON p.code IN (
    'domain.user.read',
    'domain.user.create',
    'domain.user.update',
    'domain.user.remove',
    'domain.sla.update',
    'domain.notification_template.update',
    'ticket.read',
    'ticket.assign',
    'ticket.close',
    'consultation.reply'
)
WHERE r.code = 'domain_admin'
ON DUPLICATE KEY UPDATE
    role_id = VALUES(role_id),
    permission_id = VALUES(permission_id);

INSERT INTO iam_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
JOIN iam_permission p ON p.code IN (
    'ticket.read',
    'ticket.assign',
    'consultation.reply'
)
WHERE r.code = 'agent'
ON DUPLICATE KEY UPDATE
    role_id = VALUES(role_id),
    permission_id = VALUES(permission_id);

UPDATE iam_admin_menu
SET name = CASE route_path
    WHEN '/system/menus' THEN '菜单管理'
    WHEN '/system/roles' THEN '角色管理'
    WHEN '/system/users' THEN '用户管理'
    WHEN '/system/users/offboard-pool' THEN '离职池'
    ELSE name
END
WHERE route_path IN ('/system/menus', '/system/roles', '/system/users', '/system/users/offboard-pool');

UPDATE iam_admin_menu
SET permission_code = CASE permission_code
    WHEN 'action.iam.menus.read' THEN 'platform.menu.read'
    WHEN 'action.iam.menus.create' THEN 'platform.menu.create'
    WHEN 'action.iam.menus.update' THEN 'platform.menu.update'
    WHEN 'action.iam.menus.delete' THEN 'platform.menu.delete'
    WHEN 'action.iam.roles.read' THEN 'platform.role.read'
    WHEN 'action.iam.roles.create' THEN 'platform.role.create'
    WHEN 'action.iam.roles.update' THEN 'platform.role.update'
    WHEN 'action.iam.roles.delete' THEN 'platform.role.delete'
    WHEN 'action.iam.role_permissions.read' THEN 'platform.role_permission.read'
    WHEN 'action.iam.role_permissions.update' THEN 'platform.role_permission.update'
    WHEN 'action.iam.users.read' THEN 'platform.user.read'
    WHEN 'action.iam.users.create' THEN 'domain.user.create'
    WHEN 'action.iam.users.update' THEN 'platform.user.update'
    WHEN 'action.iam.users.offboard' THEN 'platform.user.disable'
    WHEN 'action.iam.users.restore' THEN 'platform.user.restore'
    WHEN 'action.iam.users.offboard_pool.read' THEN 'platform.user.offboard_pool.read'
    WHEN 'action.iam.users.delete' THEN 'platform.user.delete'
    ELSE permission_code
END
WHERE permission_code IN (
    'action.iam.menus.read',
    'action.iam.menus.create',
    'action.iam.menus.update',
    'action.iam.menus.delete',
    'action.iam.roles.read',
    'action.iam.roles.create',
    'action.iam.roles.update',
    'action.iam.roles.delete',
    'action.iam.role_permissions.read',
    'action.iam.role_permissions.update',
    'action.iam.users.read',
    'action.iam.users.create',
    'action.iam.users.update',
    'action.iam.users.offboard',
    'action.iam.users.restore',
    'action.iam.users.offboard_pool.read',
    'action.iam.users.delete'
);

UPDATE iam_admin_menu
SET name = CASE permission_code
    WHEN 'platform.menu.read' THEN '查看菜单'
    WHEN 'platform.menu.create' THEN '创建菜单'
    WHEN 'platform.menu.update' THEN '更新菜单'
    WHEN 'platform.menu.delete' THEN '删除菜单'
    WHEN 'platform.role.read' THEN '查看角色'
    WHEN 'platform.role.create' THEN '创建角色'
    WHEN 'platform.role.update' THEN '更新角色'
    WHEN 'platform.role.delete' THEN '删除角色'
    WHEN 'platform.role_permission.read' THEN '查看角色授权'
    WHEN 'platform.role_permission.update' THEN '更新角色授权'
    WHEN 'platform.user.read' THEN '查看平台用户'
    WHEN 'domain.user.create' THEN '创建域用户'
    WHEN 'platform.user.update' THEN '更新平台用户'
    WHEN 'platform.user.disable' THEN '停用平台用户'
    WHEN 'platform.user.restore' THEN '恢复平台用户'
    WHEN 'platform.user.offboard_pool.read' THEN '查看离职池'
    WHEN 'platform.user.delete' THEN '删除平台用户'
    ELSE name
END
WHERE permission_code IN (
    'platform.menu.read',
    'platform.menu.create',
    'platform.menu.update',
    'platform.menu.delete',
    'platform.role.read',
    'platform.role.create',
    'platform.role.update',
    'platform.role.delete',
    'platform.role_permission.read',
    'platform.role_permission.update',
    'platform.user.read',
    'domain.user.create',
    'platform.user.update',
    'platform.user.disable',
    'platform.user.restore',
    'platform.user.offboard_pool.read',
    'platform.user.delete'
);

UPDATE iam_resource
SET resource_name = CASE resource_code
    WHEN 'menu.admin.system' THEN '系统管理'
    WHEN 'menu.admin.system.menus' THEN '菜单管理'
    WHEN 'menu.admin.system.roles' THEN '角色管理'
    WHEN 'menu.admin.system.users' THEN '用户管理'
    WHEN 'menu.admin.system.users.offboard_pool' THEN '离职池'
    WHEN 'action.iam.users.create' THEN '创建域用户'
    ELSE resource_name
END
WHERE resource_code IN (
    'menu.admin.system',
    'menu.admin.system.menus',
    'menu.admin.system.roles',
    'menu.admin.system.users',
    'menu.admin.system.users.offboard_pool',
    'action.iam.users.create'
);

