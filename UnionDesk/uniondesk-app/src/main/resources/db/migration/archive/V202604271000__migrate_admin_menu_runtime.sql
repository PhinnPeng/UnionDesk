CREATE TABLE IF NOT EXISTS iam_admin_menu (
    id bigint unsigned NOT NULL AUTO_INCREMENT,
    code varchar(64) NOT NULL,
    node_type varchar(16) NOT NULL,
    name varchar(128) NOT NULL,
    route_path varchar(255) DEFAULT NULL,
    component_key varchar(255) DEFAULT NULL,
    permission_code varchar(128) DEFAULT NULL,
    parent_id bigint unsigned DEFAULT NULL,
    order_no int NOT NULL DEFAULT 0,
    icon varchar(64) DEFAULT NULL,
    hidden tinyint NOT NULL DEFAULT 0,
    status tinyint NOT NULL DEFAULT 1,
    required tinyint NOT NULL DEFAULT 0,
    created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_iam_admin_menu_code (code),
    UNIQUE KEY uk_iam_admin_menu_route_path (route_path),
    UNIQUE KEY uk_iam_admin_menu_permission_code (permission_code),
    KEY idx_iam_admin_menu_parent_order (parent_id, order_no, id),
    KEY idx_iam_admin_menu_type_status (node_type, status),
    CONSTRAINT fk_iam_admin_menu_parent FOREIGN KEY (parent_id) REFERENCES iam_admin_menu(id),
    CONSTRAINT chk_iam_admin_menu_node_type CHECK (node_type IN ('catalog', 'menu', 'button')),
    CONSTRAINT chk_iam_admin_menu_catalog_fields CHECK (
        (node_type <> 'catalog')
        OR (route_path IS NULL AND component_key IS NULL AND permission_code IS NULL AND required = 0)
    ),
    CONSTRAINT chk_iam_admin_menu_menu_fields CHECK (
        (node_type <> 'menu')
        OR (route_path IS NOT NULL AND component_key IS NOT NULL AND permission_code IS NULL)
    ),
    CONSTRAINT chk_iam_admin_menu_button_fields CHECK (
        (node_type <> 'button')
        OR (route_path IS NULL AND component_key IS NULL AND permission_code IS NOT NULL)
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Admin 菜单树';

CREATE TABLE IF NOT EXISTS iam_admin_role_menu_relation (
    id bigint unsigned NOT NULL AUTO_INCREMENT,
    role_id int unsigned NOT NULL,
    menu_id bigint unsigned NOT NULL,
    created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_iam_admin_role_menu (role_id, menu_id),
    KEY idx_iam_admin_role_menu_menu_id (menu_id),
    CONSTRAINT fk_iam_admin_role_menu_role FOREIGN KEY (role_id) REFERENCES role(id),
    CONSTRAINT fk_iam_admin_role_menu_menu FOREIGN KEY (menu_id) REFERENCES iam_admin_menu(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Admin 角色菜单授权关系';

INSERT INTO iam_admin_menu (
    code, node_type, name, route_path, component_key, permission_code, parent_id, order_no, icon, hidden, status, required
)
SELECT
    'TMP-SYSTEM-CATALOG', 'catalog', '系统管理', NULL, NULL, NULL, NULL, 100, 'SettingOutlined', 0, 1, 0
FROM dual
WHERE NOT EXISTS (
    SELECT 1
    FROM iam_admin_menu
    WHERE node_type = 'catalog'
      AND name = '系统管理'
      AND parent_id IS NULL
);

INSERT INTO iam_admin_menu (
    code, node_type, name, route_path, component_key, permission_code, parent_id, order_no, icon, hidden, status, required
)
SELECT
    'TMP-MENUS-MENU', 'menu', '菜单管理', '/system/menus', './system/menus', NULL,
    (SELECT id FROM iam_admin_menu WHERE node_type = 'catalog' AND name = '系统管理' AND parent_id IS NULL LIMIT 1),
    101, 'MenuOutlined', 0, 1, 0
FROM dual
WHERE NOT EXISTS (
    SELECT 1
    FROM iam_admin_menu
    WHERE route_path = '/system/menus'
);

INSERT INTO iam_admin_menu (
    code, node_type, name, route_path, component_key, permission_code, parent_id, order_no, icon, hidden, status, required
)
SELECT
    'TMP-ROLES-MENU', 'menu', '角色管理', '/system/roles', './system/roles', NULL,
    (SELECT id FROM iam_admin_menu WHERE node_type = 'catalog' AND name = '系统管理' AND parent_id IS NULL LIMIT 1),
    102, 'TeamOutlined', 0, 1, 0
FROM dual
WHERE NOT EXISTS (
    SELECT 1
    FROM iam_admin_menu
    WHERE route_path = '/system/roles'
);

INSERT INTO iam_admin_menu (
    code, node_type, name, route_path, component_key, permission_code, parent_id, order_no, icon, hidden, status, required
)
SELECT
    'TMP-USERS-MENU', 'menu', '用户管理', '/system/users', './system/users', NULL,
    (SELECT id FROM iam_admin_menu WHERE node_type = 'catalog' AND name = '系统管理' AND parent_id IS NULL LIMIT 1),
    103, 'UserOutlined', 0, 1, 0
FROM dual
WHERE NOT EXISTS (
    SELECT 1
    FROM iam_admin_menu
    WHERE route_path = '/system/users'
);

INSERT INTO iam_admin_menu (
    code, node_type, name, route_path, component_key, permission_code, parent_id, order_no, icon, hidden, status, required
)
SELECT
    'TMP-OFFBOARD-MENU', 'menu', '离职池', '/system/users/offboard-pool', './system/users/offboard-pool', NULL,
    (SELECT id FROM iam_admin_menu WHERE route_path = '/system/users' LIMIT 1),
    104, 'DeleteOutlined', 0, 1, 0
FROM dual
WHERE NOT EXISTS (
    SELECT 1
    FROM iam_admin_menu
    WHERE route_path = '/system/users/offboard-pool'
);

INSERT INTO iam_admin_menu (
    code, node_type, name, route_path, component_key, permission_code, parent_id, order_no, icon, hidden, status, required
)
SELECT
    code_seed, 'button', name, NULL, NULL, permission_code,
    (SELECT id FROM iam_admin_menu WHERE route_path = parent_route LIMIT 1),
    0, NULL, 0, 1, required_flag
FROM (
    SELECT 'TMP-BUTTON-MENUS-READ' AS code_seed, '查询菜单' AS name, 'action.iam.menus.read' AS permission_code, '/system/menus' AS parent_route, 1 AS required_flag
    UNION ALL SELECT 'TMP-BUTTON-MENUS-CREATE', '新增菜单', 'action.iam.menus.create', '/system/menus', 0
    UNION ALL SELECT 'TMP-BUTTON-MENUS-UPDATE', '修改菜单', 'action.iam.menus.update', '/system/menus', 0
    UNION ALL SELECT 'TMP-BUTTON-MENUS-DELETE', '删除菜单', 'action.iam.menus.delete', '/system/menus', 0
    UNION ALL SELECT 'TMP-BUTTON-ROLES-READ', '查询角色', 'action.iam.roles.read', '/system/roles', 1
    UNION ALL SELECT 'TMP-BUTTON-ROLES-CREATE', '新增角色', 'action.iam.roles.create', '/system/roles', 0
    UNION ALL SELECT 'TMP-BUTTON-ROLES-UPDATE', '修改角色', 'action.iam.roles.update', '/system/roles', 0
    UNION ALL SELECT 'TMP-BUTTON-ROLES-DELETE', '删除角色', 'action.iam.roles.delete', '/system/roles', 0
    UNION ALL SELECT 'TMP-BUTTON-ROLE-PERMISSIONS-READ', '查询角色授权', 'action.iam.role_permissions.read', '/system/roles', 0
    UNION ALL SELECT 'TMP-BUTTON-ROLE-PERMISSIONS-UPDATE', '修改角色授权', 'action.iam.role_permissions.update', '/system/roles', 0
    UNION ALL SELECT 'TMP-BUTTON-USERS-READ', '查询用户', 'action.iam.users.read', '/system/users', 1
    UNION ALL SELECT 'TMP-BUTTON-USERS-CREATE', '新增用户', 'action.iam.users.create', '/system/users', 0
    UNION ALL SELECT 'TMP-BUTTON-USERS-UPDATE', '修改用户', 'action.iam.users.update', '/system/users', 0
    UNION ALL SELECT 'TMP-BUTTON-USERS-OFFBOARD', '办理离职', 'action.iam.users.offboard', '/system/users', 0
    UNION ALL SELECT 'TMP-BUTTON-OFFBOARD-READ', '查询离职池', 'action.iam.users.offboard_pool.read', '/system/users/offboard-pool', 1
    UNION ALL SELECT 'TMP-BUTTON-OFFBOARD-RESTORE', '恢复用户', 'action.iam.users.restore', '/system/users/offboard-pool', 0
    UNION ALL SELECT 'TMP-BUTTON-OFFBOARD-DELETE', '彻底删除用户', 'action.iam.users.delete', '/system/users/offboard-pool', 0
) seeded
WHERE NOT EXISTS (
    SELECT 1
    FROM iam_admin_menu existing
    WHERE existing.permission_code = seeded.permission_code
);

UPDATE iam_admin_menu
SET code = CONCAT('ADM', LPAD(id, 10, '0'))
WHERE code LIKE 'TMP-%';

INSERT IGNORE INTO iam_admin_role_menu_relation (role_id, menu_id, created_at)
SELECT DISTINCT
    relation.role_id,
    target_menu.id,
    CURRENT_TIMESTAMP(3)
FROM iam_role_resource relation
JOIN iam_resource resource ON resource.id = relation.resource_id
JOIN iam_admin_menu target_menu ON target_menu.route_path = CASE resource.resource_code
    WHEN 'menu.admin.system.menus' THEN '/system/menus'
    WHEN 'menu.admin.system.roles' THEN '/system/roles'
    WHEN 'menu.admin.system.users' THEN '/system/users'
    WHEN 'menu.admin.system.users.offboard_pool' THEN '/system/users/offboard-pool'
    ELSE NULL
END
WHERE resource.resource_code IN (
    'menu.admin.system.menus',
    'menu.admin.system.roles',
    'menu.admin.system.users',
    'menu.admin.system.users.offboard_pool'
);

INSERT IGNORE INTO iam_admin_role_menu_relation (role_id, menu_id, created_at)
SELECT DISTINCT
    relation.role_id,
    target_button.id,
    CURRENT_TIMESTAMP(3)
FROM iam_role_resource relation
JOIN iam_resource resource ON resource.id = relation.resource_id
JOIN iam_admin_menu target_button ON target_button.permission_code = resource.resource_code
WHERE resource.resource_code IN (
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
    'action.iam.users.offboard_pool.read',
    'action.iam.users.restore',
    'action.iam.users.delete'
);

INSERT IGNORE INTO iam_admin_role_menu_relation (role_id, menu_id, created_at)
SELECT
    granted_menu.role_id,
    required_button.id,
    CURRENT_TIMESTAMP(3)
FROM iam_admin_role_menu_relation granted_menu
JOIN iam_admin_menu menu_node ON menu_node.id = granted_menu.menu_id
JOIN iam_admin_menu required_button ON required_button.parent_id = menu_node.id
    AND required_button.node_type = 'button'
    AND required_button.required = 1
WHERE menu_node.node_type = 'menu';
