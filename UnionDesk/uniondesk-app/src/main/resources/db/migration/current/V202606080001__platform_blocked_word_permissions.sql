-- US-S2-05：双层屏蔽词库 — 域内码迁移 + 平台全局权限/菜单 + 域详情 catalog/按钮

-- 1. 域内 permission 重命名 domain.blocked_word.* → platform.domain.control.blocked_word.*
UPDATE iam_permission
SET code = 'platform.domain.control.blocked_word.read',
    name = '查看屏蔽词',
    description = '查看业务域屏蔽词列表',
    permission_scope = 'platform',
    action_code = 'platform_domain_control_blocked_word_read',
    path_pattern = '/api/v1/admin/domains/*/blocked-words',
    updated_at = CURRENT_TIMESTAMP(3)
WHERE code = 'domain.blocked_word.read';

UPDATE iam_permission
SET code = 'platform.domain.control.blocked_word.create',
    name = '添加屏蔽词',
    description = '为业务域新增屏蔽词（含批量）',
    permission_scope = 'platform',
    action_code = 'platform_domain_control_blocked_word_create',
    updated_at = CURRENT_TIMESTAMP(3)
WHERE code = 'domain.blocked_word.create';

UPDATE iam_permission
SET code = 'platform.domain.control.blocked_word.delete',
    name = '删除屏蔽词',
    description = '删除业务域内屏蔽词',
    permission_scope = 'platform',
    action_code = 'platform_domain_control_blocked_word_delete',
    updated_at = CURRENT_TIMESTAMP(3)
WHERE code = 'domain.blocked_word.delete';

-- 2. 平台全局 permission
INSERT INTO iam_permission (
    code, name, description, permission_scope, resource_code, action_code, http_method, path_pattern, status
)
SELECT 'platform.blocked_word.read', '查看平台屏蔽词', '查看平台全局屏蔽词列表', 'platform', 'blocked_word', 'platform_blocked_word_read', 'GET', '/api/v1/admin/blocked-words', 1
WHERE NOT EXISTS (SELECT 1 FROM iam_permission WHERE code = 'platform.blocked_word.read');

INSERT INTO iam_permission (
    code, name, description, permission_scope, resource_code, action_code, http_method, path_pattern, status
)
SELECT 'platform.blocked_word.create', '添加平台屏蔽词', '添加平台全局屏蔽词（含批量）', 'platform', 'blocked_word', 'platform_blocked_word_create', 'POST', '/api/v1/admin/blocked-words', 1
WHERE NOT EXISTS (SELECT 1 FROM iam_permission WHERE code = 'platform.blocked_word.create');

INSERT INTO iam_permission (
    code, name, description, permission_scope, resource_code, action_code, http_method, path_pattern, status
)
SELECT 'platform.blocked_word.delete', '删除平台屏蔽词', '删除平台全局屏蔽词', 'platform', 'blocked_word', 'platform_blocked_word_delete', 'DELETE', '/api/v1/admin/blocked-words/*', 1
WHERE NOT EXISTS (SELECT 1 FROM iam_permission WHERE code = 'platform.blocked_word.delete');

-- 3. 平台菜单 /platform/blockwords
INSERT INTO iam_admin_menu (code, node_type, scope, name, route_path, component_key, permission_code, order_no, icon, hidden, status, required)
SELECT 'PLATFORM-BLOCKWORDS-MENU', 'menu', 'platform', '屏蔽词库', '/platform/blockwords', './platform/blockwords', NULL, 8, 'StopOutlined', 0, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM iam_admin_menu WHERE route_path = '/platform/blockwords' AND node_type = 'menu');

INSERT INTO iam_admin_menu (code, node_type, scope, name, permission_code, parent_id, order_no, status, required)
SELECT 'TMP-BTN-PLATFORM-BW-READ', 'button', 'platform', '查询屏蔽词', 'platform.blocked_word.read',
       (SELECT id FROM iam_admin_menu WHERE route_path = '/platform/blockwords' AND node_type = 'menu' LIMIT 1),
       0, 1, 0
WHERE NOT EXISTS (
    SELECT 1 FROM iam_admin_menu WHERE permission_code = 'platform.blocked_word.read' AND node_type = 'button'
);

INSERT INTO iam_admin_menu (code, node_type, scope, name, permission_code, parent_id, order_no, status, required)
SELECT 'TMP-BTN-PLATFORM-BW-CREATE', 'button', 'platform', '添加屏蔽词', 'platform.blocked_word.create',
       (SELECT id FROM iam_admin_menu WHERE route_path = '/platform/blockwords' AND node_type = 'menu' LIMIT 1),
       1, 1, 0
WHERE NOT EXISTS (
    SELECT 1 FROM iam_admin_menu WHERE permission_code = 'platform.blocked_word.create' AND node_type = 'button'
);

INSERT INTO iam_admin_menu (code, node_type, scope, name, permission_code, parent_id, order_no, status, required)
SELECT 'TMP-BTN-PLATFORM-BW-DELETE', 'button', 'platform', '删除屏蔽词', 'platform.blocked_word.delete',
       (SELECT id FROM iam_admin_menu WHERE route_path = '/platform/blockwords' AND node_type = 'menu' LIMIT 1),
       2, 1, 0
WHERE NOT EXISTS (
    SELECT 1 FROM iam_admin_menu WHERE permission_code = 'platform.blocked_word.delete' AND node_type = 'button'
);

UPDATE iam_admin_menu
SET code = CONCAT('ADM', LPAD(id, 10, '0'))
WHERE code IN ('TMP-BTN-PLATFORM-BW-READ', 'TMP-BTN-PLATFORM-BW-CREATE', 'TMP-BTN-PLATFORM-BW-DELETE');

-- 4. 域详情 hidden catalog「屏蔽词库」+ 按钮
INSERT INTO iam_admin_menu (code, node_type, scope, name, route_path, component_key, permission_code, parent_id, order_no, icon, hidden, status, required)
SELECT 'PLATFORM-DOMAIN-CONTROL-BLOCKED-WORD', 'catalog', 'platform', '屏蔽词库', NULL, NULL, NULL,
       (SELECT id FROM iam_admin_menu WHERE code = 'PLATFORM-DOMAIN-DETAIL' LIMIT 1),
       13, NULL, 1, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM iam_admin_menu WHERE code = 'PLATFORM-DOMAIN-CONTROL-BLOCKED-WORD');

UPDATE iam_admin_menu
SET name = '屏蔽词库',
    parent_id = (SELECT id FROM (SELECT id FROM iam_admin_menu WHERE code = 'PLATFORM-DOMAIN-DETAIL' LIMIT 1) AS detail_menu),
    scope = 'platform',
    hidden = 1,
    order_no = 13,
    status = 1,
    updated_at = CURRENT_TIMESTAMP(3)
WHERE code = 'PLATFORM-DOMAIN-CONTROL-BLOCKED-WORD';

INSERT INTO iam_admin_menu (code, node_type, scope, name, permission_code, parent_id, order_no, status, required)
SELECT 'TMP-BTN-DOMAIN-BW-READ', 'button', 'platform', '查看屏蔽词', 'platform.domain.control.blocked_word.read',
       (SELECT id FROM iam_admin_menu WHERE code = 'PLATFORM-DOMAIN-CONTROL-BLOCKED-WORD' LIMIT 1),
       0, 1, 0
WHERE NOT EXISTS (
    SELECT 1 FROM iam_admin_menu WHERE permission_code = 'platform.domain.control.blocked_word.read' AND node_type = 'button'
);

INSERT INTO iam_admin_menu (code, node_type, scope, name, permission_code, parent_id, order_no, status, required)
SELECT 'TMP-BTN-DOMAIN-BW-CREATE', 'button', 'platform', '添加屏蔽词', 'platform.domain.control.blocked_word.create',
       (SELECT id FROM iam_admin_menu WHERE code = 'PLATFORM-DOMAIN-CONTROL-BLOCKED-WORD' LIMIT 1),
       1, 1, 0
WHERE NOT EXISTS (
    SELECT 1 FROM iam_admin_menu WHERE permission_code = 'platform.domain.control.blocked_word.create' AND node_type = 'button'
);

INSERT INTO iam_admin_menu (code, node_type, scope, name, permission_code, parent_id, order_no, status, required)
SELECT 'TMP-BTN-DOMAIN-BW-DELETE', 'button', 'platform', '删除屏蔽词', 'platform.domain.control.blocked_word.delete',
       (SELECT id FROM iam_admin_menu WHERE code = 'PLATFORM-DOMAIN-CONTROL-BLOCKED-WORD' LIMIT 1),
       2, 1, 0
WHERE NOT EXISTS (
    SELECT 1 FROM iam_admin_menu WHERE permission_code = 'platform.domain.control.blocked_word.delete' AND node_type = 'button'
);

UPDATE iam_admin_menu
SET code = CONCAT('ADM', LPAD(id, 10, '0'))
WHERE code IN ('TMP-BTN-DOMAIN-BW-READ', 'TMP-BTN-DOMAIN-BW-CREATE', 'TMP-BTN-DOMAIN-BW-DELETE');

UPDATE iam_admin_menu btn
JOIN iam_admin_menu catalog ON catalog.code = 'PLATFORM-DOMAIN-CONTROL-BLOCKED-WORD'
SET btn.parent_id = catalog.id,
    btn.scope = 'platform',
    btn.hidden = 0,
    btn.updated_at = CURRENT_TIMESTAMP(3)
WHERE btn.node_type = 'button'
  AND btn.permission_code IN (
      'platform.domain.control.blocked_word.read',
      'platform.domain.control.blocked_word.create',
      'platform.domain.control.blocked_word.delete'
  );

-- 5. super_admin 权限与菜单
INSERT IGNORE INTO iam_role_permission (role_id, permission_id, created_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP(3)
FROM role r
JOIN iam_permission p ON p.code IN (
    'platform.blocked_word.read',
    'platform.blocked_word.create',
    'platform.blocked_word.delete',
    'platform.domain.control.blocked_word.read',
    'platform.domain.control.blocked_word.create',
    'platform.domain.control.blocked_word.delete'
) AND p.status = 1
WHERE r.code = 'super_admin';

INSERT IGNORE INTO iam_admin_role_menu_relation (role_id, menu_id, created_at)
SELECT r.id, m.id, CURRENT_TIMESTAMP(3)
FROM role r
JOIN iam_admin_menu m ON m.route_path = '/platform/blockwords' AND m.node_type = 'menu' AND m.status = 1
WHERE r.code = 'super_admin';

INSERT IGNORE INTO iam_admin_role_menu_relation (role_id, menu_id, created_at)
SELECT r.id, btn.id, CURRENT_TIMESTAMP(3)
FROM role r
JOIN iam_admin_menu btn ON btn.node_type = 'button' AND btn.status = 1
    AND btn.permission_code IN (
        'platform.blocked_word.read',
        'platform.blocked_word.create',
        'platform.blocked_word.delete'
    )
WHERE r.code = 'super_admin';

INSERT IGNORE INTO iam_admin_role_menu_relation (role_id, menu_id, created_at)
SELECT r.id, m.id, CURRENT_TIMESTAMP(3)
FROM role r
JOIN iam_admin_menu m ON m.code = 'PLATFORM-DOMAIN-CONTROL-BLOCKED-WORD' AND m.status = 1
WHERE r.code = 'super_admin';

INSERT IGNORE INTO iam_admin_role_menu_relation (role_id, menu_id, created_at)
SELECT r.id, btn.id, CURRENT_TIMESTAMP(3)
FROM role r
JOIN iam_admin_menu btn ON btn.node_type = 'button' AND btn.status = 1
    AND btn.permission_code IN (
        'platform.domain.control.blocked_word.read',
        'platform.domain.control.blocked_word.create',
        'platform.domain.control.blocked_word.delete'
    )
WHERE r.code = 'super_admin';

-- 6. 已有域详情菜单权限的角色补绑
INSERT IGNORE INTO iam_admin_role_menu_relation (role_id, menu_id, created_at)
SELECT DISTINCT r.id, m.id, CURRENT_TIMESTAMP(3)
FROM role r
JOIN iam_admin_role_menu_relation rel ON rel.role_id = r.id
JOIN iam_admin_menu detail ON detail.id = rel.menu_id
    AND detail.route_path = '/platform/domains/detail'
    AND detail.node_type = 'menu'
    AND detail.status = 1
JOIN iam_admin_menu m ON (
    m.code = 'PLATFORM-DOMAIN-CONTROL-BLOCKED-WORD'
    OR m.permission_code IN (
        'platform.domain.control.blocked_word.read',
        'platform.domain.control.blocked_word.create',
        'platform.domain.control.blocked_word.delete'
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
    'platform.domain.control.blocked_word.read',
    'platform.domain.control.blocked_word.create',
    'platform.domain.control.blocked_word.delete'
) AND p.status = 1;
