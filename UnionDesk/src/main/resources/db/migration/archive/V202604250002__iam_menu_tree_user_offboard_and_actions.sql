ALTER TABLE iam_resource
    ADD COLUMN parent_id BIGINT UNSIGNED NULL AFTER path_pattern,
    ADD COLUMN order_no INT NOT NULL DEFAULT 0 AFTER parent_id,
    ADD COLUMN icon VARCHAR(64) DEFAULT NULL AFTER order_no,
    ADD COLUMN component VARCHAR(255) DEFAULT NULL AFTER icon,
    ADD COLUMN hidden TINYINT NOT NULL DEFAULT 0 AFTER component;

ALTER TABLE iam_resource
    ADD KEY idx_iam_resource_parent_order (parent_id, order_no, id),
    ADD CONSTRAINT fk_iam_resource_parent FOREIGN KEY (parent_id) REFERENCES iam_resource(id);

ALTER TABLE user_account
    ADD COLUMN employment_status VARCHAR(16) NOT NULL DEFAULT 'active' AFTER account_type,
    ADD COLUMN offboarded_at DATETIME(3) DEFAULT NULL AFTER employment_status,
    ADD COLUMN offboarded_by BIGINT UNSIGNED DEFAULT NULL AFTER offboarded_at,
    ADD COLUMN offboard_reason VARCHAR(255) DEFAULT NULL AFTER offboarded_by;

ALTER TABLE user_account
    ADD KEY idx_user_account_employment_status (employment_status, status),
    ADD CONSTRAINT fk_user_account_offboarded_by FOREIGN KEY (offboarded_by) REFERENCES user_account(id);

UPDATE user_account
SET employment_status = CASE
    WHEN status = 1 THEN 'active'
    ELSE 'offboarded'
END
WHERE employment_status IS NULL OR employment_status = '';

INSERT INTO iam_resource (resource_type, resource_code, resource_name, client_scope, http_method, path_pattern, parent_id, order_no, icon, component, hidden, status)
VALUES
    ('menu', 'menu.admin.system', 'System Management', 'ud-admin-web', NULL, '/system', NULL, 100, 'SettingOutlined', NULL, 0, 1),
    ('menu', 'menu.admin.system.menus', 'Menu Management', 'ud-admin-web', NULL, '/system/menus', NULL, 101, 'MenuOutlined', './system/menus', 0, 1),
    ('menu', 'menu.admin.system.roles', 'Role Management', 'ud-admin-web', NULL, '/system/roles', NULL, 102, 'TeamOutlined', './system/roles', 0, 1),
    ('menu', 'menu.admin.system.users', 'User Management', 'ud-admin-web', NULL, '/system/users', NULL, 103, 'UserOutlined', './system/users', 0, 1),
    ('menu', 'menu.admin.system.users.offboard_pool', 'Offboard Pool', 'ud-admin-web', NULL, '/system/users/offboard-pool', NULL, 104, 'DeleteOutlined', './system/users/offboard-pool', 0, 1)
ON DUPLICATE KEY UPDATE
    resource_name = VALUES(resource_name),
    client_scope = VALUES(client_scope),
    path_pattern = VALUES(path_pattern),
    order_no = VALUES(order_no),
    icon = VALUES(icon),
    component = VALUES(component),
    hidden = VALUES(hidden),
    status = VALUES(status),
    updated_at = CURRENT_TIMESTAMP(3);

UPDATE iam_resource child
JOIN iam_resource parent ON parent.resource_code = 'menu.admin.system'
SET child.parent_id = parent.id
WHERE child.resource_code IN (
    'menu.admin.system.menus',
    'menu.admin.system.roles',
    'menu.admin.system.users',
    'menu.admin.system.users.offboard_pool'
);

INSERT INTO iam_resource (resource_type, resource_code, resource_name, client_scope, http_method, path_pattern, status)
VALUES
    ('action', 'action.iam.menus.read', 'Read Menu Tree', 'ud-admin-web', 'GET', '/api/v1/iam/menus/tree', 1),
    ('action', 'action.iam.menus.create', 'Create Menu', 'ud-admin-web', 'POST', '/api/v1/iam/menus', 1),
    ('action', 'action.iam.menus.update', 'Update Menu', 'ud-admin-web', 'PUT', '/api/v1/iam/menus/*', 1),
    ('action', 'action.iam.menus.delete', 'Delete Menu', 'ud-admin-web', 'DELETE', '/api/v1/iam/menus/*', 1),
    ('action', 'action.iam.roles.read', 'Read Roles', 'ud-admin-web', 'GET', '/api/v1/iam/roles', 1),
    ('action', 'action.iam.roles.create', 'Create Role', 'ud-admin-web', 'POST', '/api/v1/iam/roles', 1),
    ('action', 'action.iam.roles.update', 'Update Role', 'ud-admin-web', 'PUT', '/api/v1/iam/roles/*', 1),
    ('action', 'action.iam.roles.delete', 'Delete Role', 'ud-admin-web', 'DELETE', '/api/v1/iam/roles/*', 1),
    ('action', 'action.iam.role_permissions.read', 'Read Role Permissions', 'ud-admin-web', 'GET', '/api/v1/iam/roles/*/permissions', 1),
    ('action', 'action.iam.role_permissions.update', 'Update Role Permissions', 'ud-admin-web', 'PUT', '/api/v1/iam/roles/*/permissions', 1),
    ('action', 'action.iam.users.read', 'Read Users', 'ud-admin-web', 'GET', '/api/v1/iam/users', 1),
    ('action', 'action.iam.users.create', 'Create User', 'ud-admin-web', 'POST', '/api/v1/iam/users', 1),
    ('action', 'action.iam.users.update', 'Update User', 'ud-admin-web', 'PUT', '/api/v1/iam/users/*', 1),
    ('action', 'action.iam.users.offboard', 'Offboard User', 'ud-admin-web', 'POST', '/api/v1/iam/users/*/offboard', 1),
    ('action', 'action.iam.users.restore', 'Restore User', 'ud-admin-web', 'POST', '/api/v1/iam/users/*/restore', 1),
    ('action', 'action.iam.users.offboard_pool.read', 'Read Offboard Pool', 'ud-admin-web', 'GET', '/api/v1/iam/users/offboard-pool', 1),
    ('action', 'action.iam.users.delete', 'Delete User Permanently', 'ud-admin-web', 'DELETE', '/api/v1/iam/users/*', 1)
ON DUPLICATE KEY UPDATE
    resource_name = VALUES(resource_name),
    client_scope = VALUES(client_scope),
    http_method = VALUES(http_method),
    path_pattern = VALUES(path_pattern),
    status = VALUES(status),
    updated_at = CURRENT_TIMESTAMP(3);

INSERT INTO iam_role_resource (role_id, resource_id)
SELECT r.id, ir.id
FROM role r
JOIN iam_resource ir ON ir.resource_code IN (
    'menu.admin.system',
    'menu.admin.system.menus',
    'menu.admin.system.roles',
    'menu.admin.system.users',
    'menu.admin.system.users.offboard_pool',
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
)
WHERE r.code = 'super_admin'
ON DUPLICATE KEY UPDATE
    role_id = VALUES(role_id),
    resource_id = VALUES(resource_id);
