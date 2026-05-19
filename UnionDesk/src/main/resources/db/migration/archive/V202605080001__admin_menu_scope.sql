ALTER TABLE iam_admin_menu
    ADD COLUMN scope VARCHAR(16) NOT NULL DEFAULT 'business' AFTER node_type;

ALTER TABLE iam_admin_menu
    ADD CONSTRAINT chk_iam_admin_menu_scope CHECK (scope IN ('platform', 'business'));

ALTER TABLE iam_admin_menu
    ADD KEY idx_iam_admin_menu_scope (scope);

UPDATE iam_admin_menu
SET scope = 'business'
WHERE scope <> 'platform';

INSERT INTO iam_admin_menu (
    code, node_type, scope, name, route_path, component_key, permission_code, parent_id, order_no, icon, hidden, status, required
)
SELECT
    'TMP-PLATFORM-CATALOG', 'catalog', 'platform', '平台管理', NULL, NULL, NULL, NULL, 1, 'AppstoreOutlined', 0, 1, 0
FROM dual
WHERE NOT EXISTS (
    SELECT 1
    FROM iam_admin_menu
    WHERE node_type = 'catalog'
      AND name = '平台管理'
      AND parent_id IS NULL
);

INSERT INTO iam_admin_menu (
    code, node_type, scope, name, route_path, component_key, permission_code, parent_id, order_no, icon, hidden, status, required
)
SELECT
    'TMP-PLATFORM-HOME', 'menu', 'platform', '平台首页', '/platform/home', './platform/home', NULL,
    (SELECT id FROM iam_admin_menu WHERE node_type = 'catalog' AND name = '平台管理' AND parent_id IS NULL LIMIT 1),
    1, 'DashboardOutlined', 0, 1, 0
FROM dual
WHERE NOT EXISTS (
    SELECT 1
    FROM iam_admin_menu
    WHERE route_path = '/platform/home'
);

INSERT INTO iam_admin_menu (
    code, node_type, scope, name, route_path, component_key, permission_code, parent_id, order_no, icon, hidden, status, required
)
SELECT
    'TMP-PLATFORM-USER', 'menu', 'platform', '平台用户', '/platform/user', './platform/user', NULL,
    (SELECT id FROM iam_admin_menu WHERE node_type = 'catalog' AND name = '平台管理' AND parent_id IS NULL LIMIT 1),
    2, 'UserOutlined', 0, 1, 0
FROM dual
WHERE NOT EXISTS (
    SELECT 1
    FROM iam_admin_menu
    WHERE route_path = '/platform/user'
);

INSERT INTO iam_admin_menu (
    code, node_type, scope, name, route_path, component_key, permission_code, parent_id, order_no, icon, hidden, status, required
)
SELECT
    'TMP-PLATFORM-DEPT', 'menu', 'platform', '平台组织', '/platform/dept', './platform/dept', NULL,
    (SELECT id FROM iam_admin_menu WHERE node_type = 'catalog' AND name = '平台管理' AND parent_id IS NULL LIMIT 1),
    3, 'ApartmentOutlined', 0, 1, 0
FROM dual
WHERE NOT EXISTS (
    SELECT 1
    FROM iam_admin_menu
    WHERE route_path = '/platform/dept'
);

INSERT INTO iam_admin_menu (
    code, node_type, scope, name, route_path, component_key, permission_code, parent_id, order_no, icon, hidden, status, required
)
SELECT
    'TMP-PLATFORM-OFFBOARD-POOL', 'menu', 'platform', '离职池', '/platform/offboard-pool', './platform/offboard-pool', NULL,
    (SELECT id FROM iam_admin_menu WHERE node_type = 'catalog' AND name = '平台管理' AND parent_id IS NULL LIMIT 1),
    4, 'UserDeleteOutlined', 0, 1, 0
FROM dual
WHERE NOT EXISTS (
    SELECT 1
    FROM iam_admin_menu
    WHERE route_path = '/platform/offboard-pool'
);

INSERT INTO iam_admin_menu (
    code, node_type, scope, name, route_path, component_key, permission_code, parent_id, order_no, icon, hidden, status, required
)
SELECT
    'TMP-PLATFORM-IMPORT-EXPORT', 'menu', 'platform', '导入导出', '/platform/import-export', './platform/import-export', NULL,
    (SELECT id FROM iam_admin_menu WHERE node_type = 'catalog' AND name = '平台管理' AND parent_id IS NULL LIMIT 1),
    5, 'ImportOutlined', 0, 1, 0
FROM dual
WHERE NOT EXISTS (
    SELECT 1
    FROM iam_admin_menu
    WHERE route_path = '/platform/import-export'
);

INSERT INTO iam_admin_menu (
    code, node_type, scope, name, route_path, component_key, permission_code, parent_id, order_no, icon, hidden, status, required
)
SELECT
    'TMP-PLATFORM-DOMAINS', 'menu', 'platform', '业务域管理', '/platform/domains', './platform/domains', NULL,
    (SELECT id FROM iam_admin_menu WHERE node_type = 'catalog' AND name = '平台管理' AND parent_id IS NULL LIMIT 1),
    6, 'GlobalOutlined', 0, 1, 0
FROM dual
WHERE NOT EXISTS (
    SELECT 1
    FROM iam_admin_menu
    WHERE route_path = '/platform/domains'
);

INSERT INTO iam_admin_menu (
    code, node_type, scope, name, route_path, component_key, permission_code, parent_id, order_no, icon, hidden, status, required
)
SELECT
    'TMP-PLATFORM-DOMAIN-ONBOARDING', 'menu', 'platform', '客户入域', '/platform/domain-onboarding', './platform/domain-onboarding', NULL,
    (SELECT id FROM iam_admin_menu WHERE node_type = 'catalog' AND name = '平台管理' AND parent_id IS NULL LIMIT 1),
    7, 'UserAddOutlined', 0, 1, 0
FROM dual
WHERE NOT EXISTS (
    SELECT 1
    FROM iam_admin_menu
    WHERE route_path = '/platform/domain-onboarding'
);

INSERT INTO iam_admin_menu (
    code, node_type, scope, name, route_path, component_key, permission_code, parent_id, order_no, icon, hidden, status, required
)
SELECT
    'TMP-PLATFORM-TICKET-POOL', 'menu', 'platform', '工单池', '/platform/ticket-pool', './platform/ticket-pool', NULL,
    (SELECT id FROM iam_admin_menu WHERE node_type = 'catalog' AND name = '平台管理' AND parent_id IS NULL LIMIT 1),
    8, 'SolutionOutlined', 0, 1, 0
FROM dual
WHERE NOT EXISTS (
    SELECT 1
    FROM iam_admin_menu
    WHERE route_path = '/platform/ticket-pool'
);

INSERT INTO iam_admin_menu (
    code, node_type, scope, name, route_path, component_key, permission_code, parent_id, order_no, icon, hidden, status, required
)
SELECT
    'TMP-PLATFORM-INBOX', 'menu', 'platform', '站内信', '/platform/inbox', './platform/inbox', NULL,
    (SELECT id FROM iam_admin_menu WHERE node_type = 'catalog' AND name = '平台管理' AND parent_id IS NULL LIMIT 1),
    9, 'MailOutlined', 0, 1, 0
FROM dual
WHERE NOT EXISTS (
    SELECT 1
    FROM iam_admin_menu
    WHERE route_path = '/platform/inbox'
);

INSERT INTO iam_admin_menu (
    code, node_type, scope, name, route_path, component_key, permission_code, parent_id, order_no, icon, hidden, status, required
)
SELECT
    'TMP-PLATFORM-ATTACHMENTS', 'menu', 'platform', '附件', '/platform/attachments', './platform/attachments', NULL,
    (SELECT id FROM iam_admin_menu WHERE node_type = 'catalog' AND name = '平台管理' AND parent_id IS NULL LIMIT 1),
    10, 'PaperClipOutlined', 0, 1, 0
FROM dual
WHERE NOT EXISTS (
    SELECT 1
    FROM iam_admin_menu
    WHERE route_path = '/platform/attachments'
);

INSERT INTO iam_admin_menu (
    code, node_type, scope, name, route_path, component_key, permission_code, parent_id, order_no, icon, hidden, status, required
)
SELECT
    'TMP-PLATFORM-PERMISSION', 'menu', 'platform', '权限管理', '/platform/permission', './platform/permission', NULL,
    (SELECT id FROM iam_admin_menu WHERE node_type = 'catalog' AND name = '平台管理' AND parent_id IS NULL LIMIT 1),
    11, 'SafetyCertificateOutlined', 0, 1, 0
FROM dual
WHERE NOT EXISTS (
    SELECT 1
    FROM iam_admin_menu
    WHERE route_path = '/platform/permission'
);

INSERT INTO iam_admin_menu (
    code, node_type, scope, name, route_path, component_key, permission_code, parent_id, order_no, icon, hidden, status, required
)
SELECT
    'TMP-PLATFORM-ROLE', 'menu', 'platform', '平台角色', '/platform/role', './system/role', NULL,
    (SELECT id FROM iam_admin_menu WHERE route_path = '/platform/permission' LIMIT 1),
    11, 'TeamOutlined', 0, 1, 0
FROM dual
WHERE NOT EXISTS (
    SELECT 1
    FROM iam_admin_menu
    WHERE route_path = '/platform/role'
);

INSERT INTO iam_admin_menu (
    code, node_type, scope, name, route_path, component_key, permission_code, parent_id, order_no, icon, hidden, status, required
)
SELECT
    'TMP-PLATFORM-MENU', 'menu', 'platform', '平台菜单', '/platform/menu', './system/menu', NULL,
    (SELECT id FROM iam_admin_menu WHERE route_path = '/platform/permission' LIMIT 1),
    12, 'MenuOutlined', 0, 1, 0
FROM dual
WHERE NOT EXISTS (
    SELECT 1
    FROM iam_admin_menu
    WHERE route_path = '/platform/menu'
);

INSERT INTO iam_admin_menu (
    code, node_type, scope, name, route_path, component_key, permission_code, parent_id, order_no, icon, hidden, status, required
)
SELECT
    'TMP-PLATFORM-AUDIT-LOGS', 'menu', 'platform', '审计日志', '/platform/audit-logs', './platform/audit-logs', NULL,
    (SELECT id FROM iam_admin_menu WHERE node_type = 'catalog' AND name = '平台管理' AND parent_id IS NULL LIMIT 1),
    13, 'FileSearchOutlined', 1, 1, 0
FROM dual
WHERE NOT EXISTS (
    SELECT 1
    FROM iam_admin_menu
    WHERE route_path = '/platform/audit-logs'
);

INSERT INTO iam_admin_menu (
    code, node_type, scope, name, route_path, component_key, permission_code, parent_id, order_no, icon, hidden, status, required
)
SELECT
    'TMP-PLATFORM-DOMAIN-CONFIG', 'menu', 'platform', '域配置', '/platform/domain-config', './platform/domain-config', NULL,
    (SELECT id FROM iam_admin_menu WHERE node_type = 'catalog' AND name = '平台管理' AND parent_id IS NULL LIMIT 1),
    14, 'SettingOutlined', 1, 1, 0
FROM dual
WHERE NOT EXISTS (
    SELECT 1
    FROM iam_admin_menu
    WHERE route_path = '/platform/domain-config'
);

INSERT INTO iam_admin_menu (
    code, node_type, scope, name, route_path, component_key, permission_code, parent_id, order_no, icon, hidden, status, required
)
SELECT
    'TMP-PLATFORM-SLA-MANAGEMENT', 'menu', 'platform', 'SLA管理', '/platform/sla-management', './platform/sla-management', NULL,
    (SELECT id FROM iam_admin_menu WHERE node_type = 'catalog' AND name = '平台管理' AND parent_id IS NULL LIMIT 1),
    15, 'ClockCircleOutlined', 1, 1, 0
FROM dual
WHERE NOT EXISTS (
    SELECT 1
    FROM iam_admin_menu
    WHERE route_path = '/platform/sla-management'
);

INSERT INTO iam_admin_menu (
    code, node_type, scope, name, route_path, component_key, permission_code, parent_id, order_no, icon, hidden, status, required
)
SELECT
    'TMP-PLATFORM-SYSTEM-SETTINGS', 'menu', 'platform', '系统设置', '/platform/system-settings', './platform/system-settings', NULL,
    (SELECT id FROM iam_admin_menu WHERE node_type = 'catalog' AND name = '平台管理' AND parent_id IS NULL LIMIT 1),
    16, 'ToolOutlined', 1, 1, 0
FROM dual
WHERE NOT EXISTS (
    SELECT 1
    FROM iam_admin_menu
    WHERE route_path = '/platform/system-settings'
);

INSERT INTO iam_admin_menu (
    code, node_type, scope, name, route_path, component_key, permission_code, parent_id, order_no, icon, hidden, status, required
)
SELECT
    'TMP-PLATFORM-TICKET-DETAIL', 'menu', 'platform', '工单详情', '/platform/ticket-detail', './platform/ticket-detail', NULL,
    (SELECT id FROM iam_admin_menu WHERE node_type = 'catalog' AND name = '平台管理' AND parent_id IS NULL LIMIT 1),
    17, 'ProfileOutlined', 1, 1, 0
FROM dual
WHERE NOT EXISTS (
    SELECT 1
    FROM iam_admin_menu
    WHERE route_path = '/platform/ticket-detail'
);

INSERT INTO iam_admin_menu (
    code, node_type, scope, name, route_path, component_key, permission_code, parent_id, order_no, icon, hidden, status, required
)
SELECT
    'TMP-BUTTON-PLATFORM-HOME-READ', 'button', 'platform', '查询平台首页', NULL, NULL, 'platform.login_log.read',
    (SELECT id FROM iam_admin_menu WHERE route_path = '/platform/home' LIMIT 1),
    0, NULL, 0, 1, 1
FROM dual
WHERE NOT EXISTS (
    SELECT 1
    FROM iam_admin_menu
    WHERE permission_code = 'platform.login_log.read'
);

INSERT INTO iam_admin_menu (
    code, node_type, scope, name, route_path, component_key, permission_code, parent_id, order_no, icon, hidden, status, required
)
SELECT
    'TMP-BUTTON-PLATFORM-USER-READ', 'button', 'platform', '查询平台用户', NULL, NULL, 'platform.user.read',
    (SELECT id FROM iam_admin_menu WHERE route_path = '/platform/user' LIMIT 1),
    0, NULL, 0, 1, 1
FROM dual
WHERE NOT EXISTS (
    SELECT 1
    FROM iam_admin_menu
    WHERE permission_code = 'platform.user.read'
);

INSERT INTO iam_admin_menu (
    code, node_type, scope, name, route_path, component_key, permission_code, parent_id, order_no, icon, hidden, status, required
)
SELECT
    'TMP-BUTTON-PLATFORM-DEPT-READ', 'button', 'platform', '查询平台组织', NULL, NULL, 'platform.user.create',
    (SELECT id FROM iam_admin_menu WHERE route_path = '/platform/dept' LIMIT 1),
    0, NULL, 0, 1, 1
FROM dual
WHERE NOT EXISTS (
    SELECT 1
    FROM iam_admin_menu
    WHERE permission_code = 'platform.user.create'
);

INSERT INTO iam_admin_menu (
    code, node_type, scope, name, route_path, component_key, permission_code, parent_id, order_no, icon, hidden, status, required
)
SELECT
    'TMP-BUTTON-PLATFORM-OFFBOARD-READ', 'button', 'platform', '查询离职池', NULL, NULL, 'platform.user.offboard_pool.read',
    (SELECT id FROM iam_admin_menu WHERE route_path = '/platform/offboard-pool' LIMIT 1),
    0, NULL, 0, 1, 1
FROM dual
WHERE NOT EXISTS (
    SELECT 1
    FROM iam_admin_menu
    WHERE permission_code = 'platform.user.offboard_pool.read'
);

INSERT INTO iam_admin_menu (
    code, node_type, scope, name, route_path, component_key, permission_code, parent_id, order_no, icon, hidden, status, required
)
SELECT
    'TMP-BUTTON-PLATFORM-IMPORT-EXPORT-READ', 'button', 'platform', '查询导入导出', NULL, NULL, 'platform.user.update',
    (SELECT id FROM iam_admin_menu WHERE route_path = '/platform/import-export' LIMIT 1),
    0, NULL, 0, 1, 1
FROM dual
WHERE NOT EXISTS (
    SELECT 1
    FROM iam_admin_menu
    WHERE permission_code = 'platform.user.update'
);

INSERT INTO iam_admin_menu (
    code, node_type, scope, name, route_path, component_key, permission_code, parent_id, order_no, icon, hidden, status, required
)
SELECT
    'TMP-BUTTON-PLATFORM-DOMAINS-READ', 'button', 'platform', '查询业务域管理', NULL, NULL, 'domain.read',
    (SELECT id FROM iam_admin_menu WHERE route_path = '/platform/domains' LIMIT 1),
    0, NULL, 0, 1, 1
FROM dual
WHERE NOT EXISTS (
    SELECT 1
    FROM iam_admin_menu
    WHERE permission_code = 'domain.read'
);

INSERT INTO iam_admin_menu (
    code, node_type, scope, name, route_path, component_key, permission_code, parent_id, order_no, icon, hidden, status, required
)
SELECT
    'TMP-BUTTON-PLATFORM-ONBOARDING-CREATE', 'button', 'platform', '客户入域', NULL, NULL, 'domain.customer.create',
    (SELECT id FROM iam_admin_menu WHERE route_path = '/platform/domain-onboarding' LIMIT 1),
    0, NULL, 0, 1, 1
FROM dual
WHERE NOT EXISTS (
    SELECT 1
    FROM iam_admin_menu
    WHERE permission_code = 'domain.customer.create'
);

INSERT INTO iam_admin_menu (
    code, node_type, scope, name, route_path, component_key, permission_code, parent_id, order_no, icon, hidden, status, required
)
SELECT
    'TMP-BUTTON-PLATFORM-TICKET-POOL-READ', 'button', 'platform', '查询工单池', NULL, NULL, 'ticket.read',
    (SELECT id FROM iam_admin_menu WHERE route_path = '/platform/ticket-pool' LIMIT 1),
    0, NULL, 0, 1, 1
FROM dual
WHERE NOT EXISTS (
    SELECT 1
    FROM iam_admin_menu
    WHERE permission_code = 'ticket.read'
);

INSERT INTO iam_admin_menu (
    code, node_type, scope, name, route_path, component_key, permission_code, parent_id, order_no, icon, hidden, status, required
)
SELECT
    'TMP-BUTTON-PLATFORM-INBOX-READ', 'button', 'platform', '查询站内信', NULL, NULL, 'inbox.read',
    (SELECT id FROM iam_admin_menu WHERE route_path = '/platform/inbox' LIMIT 1),
    0, NULL, 0, 1, 1
FROM dual
WHERE NOT EXISTS (
    SELECT 1
    FROM iam_admin_menu
    WHERE permission_code = 'inbox.read'
);

INSERT INTO iam_admin_menu (
    code, node_type, scope, name, route_path, component_key, permission_code, parent_id, order_no, icon, hidden, status, required
)
SELECT
    'TMP-BUTTON-PLATFORM-ATTACHMENTS-DOWNLOAD', 'button', 'platform', '查询附件', NULL, NULL, 'attachment.download',
    (SELECT id FROM iam_admin_menu WHERE route_path = '/platform/attachments' LIMIT 1),
    0, NULL, 0, 1, 1
FROM dual
WHERE NOT EXISTS (
    SELECT 1
    FROM iam_admin_menu
    WHERE permission_code = 'attachment.download'
);

INSERT INTO iam_admin_menu (
    code, node_type, scope, name, route_path, component_key, permission_code, parent_id, order_no, icon, hidden, status, required
)
SELECT
    'TMP-BUTTON-PLATFORM-PERMISSION-MANAGE', 'button', 'platform', '查询权限管理', NULL, NULL, 'platform.permission.manage',
    (SELECT id FROM iam_admin_menu WHERE route_path = '/platform/permission' LIMIT 1),
    0, NULL, 0, 1, 1
FROM dual
WHERE NOT EXISTS (
    SELECT 1
    FROM iam_admin_menu
    WHERE permission_code = 'platform.permission.manage'
);

INSERT INTO iam_admin_menu (
    code, node_type, scope, name, route_path, component_key, permission_code, parent_id, order_no, icon, hidden, status, required
)
SELECT
    'TMP-BUTTON-PLATFORM-ROLE-READ', 'button', 'platform', '查询平台角色', NULL, NULL, 'platform.role.read',
    (SELECT id FROM iam_admin_menu WHERE route_path = '/platform/role' LIMIT 1),
    0, NULL, 0, 1, 1
FROM dual
WHERE NOT EXISTS (
    SELECT 1
    FROM iam_admin_menu
    WHERE permission_code = 'platform.role.read'
);

INSERT INTO iam_admin_menu (
    code, node_type, scope, name, route_path, component_key, permission_code, parent_id, order_no, icon, hidden, status, required
)
SELECT
    'TMP-BUTTON-PLATFORM-MENU-READ', 'button', 'platform', '查询平台菜单', NULL, NULL, 'platform.menu.read',
    (SELECT id FROM iam_admin_menu WHERE route_path = '/platform/menu' LIMIT 1),
    0, NULL, 0, 1, 1
FROM dual
WHERE NOT EXISTS (
    SELECT 1
    FROM iam_admin_menu
    WHERE permission_code = 'platform.menu.read'
);

INSERT INTO iam_admin_menu (
    code, node_type, scope, name, route_path, component_key, permission_code, parent_id, order_no, icon, hidden, status, required
)
SELECT
    'TMP-BUTTON-PLATFORM-AUDIT-READ', 'button', 'platform', '查询审计日志', NULL, NULL, 'platform.audit_log.read',
    (SELECT id FROM iam_admin_menu WHERE route_path = '/platform/audit-logs' LIMIT 1),
    0, NULL, 0, 1, 1
FROM dual
WHERE NOT EXISTS (
    SELECT 1
    FROM iam_admin_menu
    WHERE permission_code = 'platform.audit_log.read'
);

INSERT INTO iam_admin_menu (
    code, node_type, scope, name, route_path, component_key, permission_code, parent_id, order_no, icon, hidden, status, required
)
SELECT
    'TMP-BUTTON-PLATFORM-DOMAIN-CONFIG-READ', 'button', 'platform', '查询域配置', NULL, NULL, 'domain.config.read',
    (SELECT id FROM iam_admin_menu WHERE route_path = '/platform/domain-config' LIMIT 1),
    0, NULL, 0, 1, 1
FROM dual
WHERE NOT EXISTS (
    SELECT 1
    FROM iam_admin_menu
    WHERE permission_code = 'domain.config.read'
);

INSERT INTO iam_admin_menu (
    code, node_type, scope, name, route_path, component_key, permission_code, parent_id, order_no, icon, hidden, status, required
)
SELECT
    'TMP-BUTTON-PLATFORM-SLA-READ', 'button', 'platform', '查询SLA管理', NULL, NULL, 'domain.sla.read',
    (SELECT id FROM iam_admin_menu WHERE route_path = '/platform/sla-management' LIMIT 1),
    0, NULL, 0, 1, 1
FROM dual
WHERE NOT EXISTS (
    SELECT 1
    FROM iam_admin_menu
    WHERE permission_code = 'domain.sla.read'
);

INSERT INTO iam_admin_menu (
    code, node_type, scope, name, route_path, component_key, permission_code, parent_id, order_no, icon, hidden, status, required
)
SELECT
    'TMP-BUTTON-PLATFORM-SYSTEM-SETTINGS-READ', 'button', 'platform', '查询系统设置', NULL, NULL, 'platform.system_config.read',
    (SELECT id FROM iam_admin_menu WHERE route_path = '/platform/system-settings' LIMIT 1),
    0, NULL, 0, 1, 1
FROM dual
WHERE NOT EXISTS (
    SELECT 1
    FROM iam_admin_menu
    WHERE permission_code = 'platform.system_config.read'
);

INSERT INTO iam_admin_menu (
    code, node_type, scope, name, route_path, component_key, permission_code, parent_id, order_no, icon, hidden, status, required
)
SELECT
    'TMP-BUTTON-PLATFORM-TICKET-DETAIL-REPLY', 'button', 'platform', '查询工单详情', NULL, NULL, 'ticket.reply',
    (SELECT id FROM iam_admin_menu WHERE route_path = '/platform/ticket-detail' LIMIT 1),
    0, NULL, 0, 1, 1
FROM dual
WHERE NOT EXISTS (
    SELECT 1
    FROM iam_admin_menu
    WHERE permission_code = 'ticket.reply'
);

UPDATE iam_admin_menu
SET code = CONCAT('ADM', LPAD(id, 10, '0'))
WHERE code LIKE 'TMP-%';

INSERT IGNORE INTO iam_admin_role_menu_relation (role_id, menu_id, created_at)
SELECT DISTINCT
    r.id,
    m.id,
    CURRENT_TIMESTAMP(3)
FROM role r
JOIN iam_admin_menu m ON m.scope = 'platform'
WHERE r.code = 'super_admin'
  AND m.node_type IN ('menu', 'button');
