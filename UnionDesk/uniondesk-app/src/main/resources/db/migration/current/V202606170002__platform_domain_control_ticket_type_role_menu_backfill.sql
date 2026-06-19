-- US-S3-01 补绑：域详情「工单管理」catalog/按钮 + super_admin/platform_admin 菜单与权限
-- 幂等；对齐 V202606080001 屏蔽词库模式

-- 1. 权限码（若 V202606170001 未执行则补齐）
INSERT INTO iam_permission (
    code, name, description, permission_scope, resource_code, action_code, http_method, path_pattern, status
)
SELECT 'platform.domain.control.ticket_type.read', '查看工单类型', '查看业务域工单类型与模板', 'platform',
       'ticket_type', 'platform_domain_control_ticket_type_read', 'GET', '/api/v1/admin/domains/*/ticket-types', 1
WHERE NOT EXISTS (SELECT 1 FROM iam_permission WHERE code = 'platform.domain.control.ticket_type.read');

INSERT INTO iam_permission (
    code, name, description, permission_scope, resource_code, action_code, http_method, path_pattern, status
)
SELECT 'platform.domain.control.ticket_type.create', '新建工单类型', '创建业务域工单类型与模板', 'platform',
       'ticket_type', 'platform_domain_control_ticket_type_create', 'POST', '/api/v1/admin/domains/*/ticket-types', 1
WHERE NOT EXISTS (SELECT 1 FROM iam_permission WHERE code = 'platform.domain.control.ticket_type.create');

INSERT INTO iam_permission (
    code, name, description, permission_scope, resource_code, action_code, http_method, path_pattern, status
)
SELECT 'platform.domain.control.ticket_type.update', '编辑工单类型', '更新业务域工单类型与模板', 'platform',
       'ticket_type', 'platform_domain_control_ticket_type_update', 'PUT', '/api/v1/admin/domains/*/ticket-types/*', 1
WHERE NOT EXISTS (SELECT 1 FROM iam_permission WHERE code = 'platform.domain.control.ticket_type.update');

INSERT INTO iam_permission (
    code, name, description, permission_scope, resource_code, action_code, http_method, path_pattern, status
)
SELECT 'platform.domain.control.ticket_type.delete', '删除工单类型', '删除业务域工单类型与模板', 'platform',
       'ticket_type', 'platform_domain_control_ticket_type_delete', 'DELETE', '/api/v1/admin/domains/*/ticket-types/*', 1
WHERE NOT EXISTS (SELECT 1 FROM iam_permission WHERE code = 'platform.domain.control.ticket_type.delete');

-- 2. 域详情 hidden catalog「工单管理」+ 按钮
INSERT INTO iam_admin_menu (code, node_type, scope, name, route_path, component_key, permission_code, parent_id, order_no, icon, hidden, status, required)
SELECT 'PLATFORM-DOMAIN-CONTROL-TICKET-TYPE', 'catalog', 'platform', '工单管理', NULL, NULL, NULL,
       (SELECT id FROM iam_admin_menu WHERE code = 'PLATFORM-DOMAIN-DETAIL' LIMIT 1),
       12, 'FormOutlined', 1, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM iam_admin_menu WHERE code = 'PLATFORM-DOMAIN-CONTROL-TICKET-TYPE');

UPDATE iam_admin_menu
SET name = '工单管理',
    parent_id = (SELECT id FROM (SELECT id FROM iam_admin_menu WHERE code = 'PLATFORM-DOMAIN-DETAIL' LIMIT 1) AS detail_menu),
    scope = 'platform',
    hidden = 1,
    icon = 'FormOutlined',
    order_no = 12,
    status = 1,
    updated_at = CURRENT_TIMESTAMP(3)
WHERE code = 'PLATFORM-DOMAIN-CONTROL-TICKET-TYPE';

INSERT INTO iam_admin_menu (code, node_type, scope, name, permission_code, parent_id, order_no, status, required)
SELECT 'TMP-BTN-DOMAIN-TT-READ', 'button', 'platform', '查看工单类型', 'platform.domain.control.ticket_type.read',
       (SELECT id FROM iam_admin_menu WHERE code = 'PLATFORM-DOMAIN-CONTROL-TICKET-TYPE' LIMIT 1),
       0, 1, 0
WHERE NOT EXISTS (
    SELECT 1 FROM iam_admin_menu WHERE permission_code = 'platform.domain.control.ticket_type.read' AND node_type = 'button'
);

INSERT INTO iam_admin_menu (code, node_type, scope, name, permission_code, parent_id, order_no, status, required)
SELECT 'TMP-BTN-DOMAIN-TT-CREATE', 'button', 'platform', '新建工单类型', 'platform.domain.control.ticket_type.create',
       (SELECT id FROM iam_admin_menu WHERE code = 'PLATFORM-DOMAIN-CONTROL-TICKET-TYPE' LIMIT 1),
       1, 1, 0
WHERE NOT EXISTS (
    SELECT 1 FROM iam_admin_menu WHERE permission_code = 'platform.domain.control.ticket_type.create' AND node_type = 'button'
);

INSERT INTO iam_admin_menu (code, node_type, scope, name, permission_code, parent_id, order_no, status, required)
SELECT 'TMP-BTN-DOMAIN-TT-UPDATE', 'button', 'platform', '编辑工单类型', 'platform.domain.control.ticket_type.update',
       (SELECT id FROM iam_admin_menu WHERE code = 'PLATFORM-DOMAIN-CONTROL-TICKET-TYPE' LIMIT 1),
       2, 1, 0
WHERE NOT EXISTS (
    SELECT 1 FROM iam_admin_menu WHERE permission_code = 'platform.domain.control.ticket_type.update' AND node_type = 'button'
);

INSERT INTO iam_admin_menu (code, node_type, scope, name, permission_code, parent_id, order_no, status, required)
SELECT 'TMP-BTN-DOMAIN-TT-DELETE', 'button', 'platform', '删除工单类型', 'platform.domain.control.ticket_type.delete',
       (SELECT id FROM iam_admin_menu WHERE code = 'PLATFORM-DOMAIN-CONTROL-TICKET-TYPE' LIMIT 1),
       3, 1, 0
WHERE NOT EXISTS (
    SELECT 1 FROM iam_admin_menu WHERE permission_code = 'platform.domain.control.ticket_type.delete' AND node_type = 'button'
);

UPDATE iam_admin_menu
SET code = CONCAT('ADM', LPAD(id, 10, '0'))
WHERE code IN ('TMP-BTN-DOMAIN-TT-READ', 'TMP-BTN-DOMAIN-TT-CREATE', 'TMP-BTN-DOMAIN-TT-UPDATE', 'TMP-BTN-DOMAIN-TT-DELETE');

UPDATE iam_admin_menu btn
JOIN iam_admin_menu catalog ON catalog.code = 'PLATFORM-DOMAIN-CONTROL-TICKET-TYPE'
SET btn.parent_id = catalog.id,
    btn.scope = 'platform',
    btn.hidden = 0,
    btn.updated_at = CURRENT_TIMESTAMP(3)
WHERE btn.node_type = 'button'
  AND btn.permission_code IN (
      'platform.domain.control.ticket_type.read',
      'platform.domain.control.ticket_type.create',
      'platform.domain.control.ticket_type.update',
      'platform.domain.control.ticket_type.delete'
  );

-- 3. super_admin 权限与菜单
INSERT IGNORE INTO iam_role_permission (role_id, permission_id, created_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP(3)
FROM role r
JOIN iam_permission p ON p.code IN (
    'platform.domain.control.ticket_type.read',
    'platform.domain.control.ticket_type.create',
    'platform.domain.control.ticket_type.update',
    'platform.domain.control.ticket_type.delete'
) AND p.status = 1
WHERE r.code = 'super_admin';

INSERT IGNORE INTO iam_admin_role_menu_relation (role_id, menu_id, created_at)
SELECT r.id, m.id, CURRENT_TIMESTAMP(3)
FROM role r
JOIN iam_admin_menu m ON m.code = 'PLATFORM-DOMAIN-CONTROL-TICKET-TYPE' AND m.status = 1
WHERE r.code = 'super_admin';

INSERT IGNORE INTO iam_admin_role_menu_relation (role_id, menu_id, created_at)
SELECT r.id, btn.id, CURRENT_TIMESTAMP(3)
FROM role r
JOIN iam_admin_menu btn ON btn.node_type = 'button' AND btn.status = 1
    AND btn.permission_code IN (
        'platform.domain.control.ticket_type.read',
        'platform.domain.control.ticket_type.create',
        'platform.domain.control.ticket_type.update',
        'platform.domain.control.ticket_type.delete'
    )
WHERE r.code = 'super_admin';

-- 4. platform_admin 权限与菜单（admin 账号默认角色）
INSERT IGNORE INTO iam_role_permission (role_id, permission_id, created_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP(3)
FROM role r
JOIN iam_permission p ON p.code IN (
    'platform.domain.control.ticket_type.read',
    'platform.domain.control.ticket_type.create',
    'platform.domain.control.ticket_type.update',
    'platform.domain.control.ticket_type.delete'
) AND p.status = 1
WHERE r.code = 'platform_admin';

INSERT IGNORE INTO iam_admin_role_menu_relation (role_id, menu_id, created_at)
SELECT r.id, m.id, CURRENT_TIMESTAMP(3)
FROM role r
JOIN iam_admin_menu m ON m.code = 'PLATFORM-DOMAIN-CONTROL-TICKET-TYPE' AND m.status = 1
WHERE r.code = 'platform_admin';

INSERT IGNORE INTO iam_admin_role_menu_relation (role_id, menu_id, created_at)
SELECT r.id, btn.id, CURRENT_TIMESTAMP(3)
FROM role r
JOIN iam_admin_menu btn ON btn.node_type = 'button' AND btn.status = 1
    AND btn.permission_code IN (
        'platform.domain.control.ticket_type.read',
        'platform.domain.control.ticket_type.create',
        'platform.domain.control.ticket_type.update',
        'platform.domain.control.ticket_type.delete'
    )
WHERE r.code = 'platform_admin';

-- 5. 已有域详情菜单权限的角色补绑
INSERT IGNORE INTO iam_admin_role_menu_relation (role_id, menu_id, created_at)
SELECT DISTINCT r.id, m.id, CURRENT_TIMESTAMP(3)
FROM role r
JOIN iam_admin_role_menu_relation rel ON rel.role_id = r.id
JOIN iam_admin_menu detail ON detail.id = rel.menu_id
    AND detail.route_path = '/platform/domains/detail'
    AND detail.node_type = 'menu'
    AND detail.status = 1
JOIN iam_admin_menu m ON (
    m.code = 'PLATFORM-DOMAIN-CONTROL-TICKET-TYPE'
    OR m.permission_code IN (
        'platform.domain.control.ticket_type.read',
        'platform.domain.control.ticket_type.create',
        'platform.domain.control.ticket_type.update',
        'platform.domain.control.ticket_type.delete'
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
    'platform.domain.control.ticket_type.read',
    'platform.domain.control.ticket_type.create',
    'platform.domain.control.ticket_type.update',
    'platform.domain.control.ticket_type.delete'
) AND p.status = 1;
