-- US-S3-01 补绑 v2：super_admin 同步全部 platform 菜单节点（含 catalog/button）
-- 背景：V202606150001 仅对 platform_admin 做 platform 全量菜单绑定；super_admin 依赖各特性迁移逐条补绑，
--       易导致「admin 以 super_admin 登录时 permission-snapshot 缺 ticket_type.*」。
-- 幂等；不影响 domain/business scope。

-- 1. 工单类型 catalog / 按钮元数据（与 V202606170002 一致，双保险）
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

-- 2. super_admin：同步全部 platform 菜单节点（catalog / menu / button）
INSERT IGNORE INTO iam_admin_role_menu_relation (role_id, menu_id, created_at)
SELECT r.id, m.id, CURRENT_TIMESTAMP(3)
FROM role r
JOIN iam_admin_menu m ON m.scope = 'platform' AND m.status = 1
WHERE r.code = 'super_admin';

-- 3. super_admin + platform_admin：工单类型四码直授
INSERT IGNORE INTO iam_role_permission (role_id, permission_id, created_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP(3)
FROM role r
JOIN iam_permission p ON p.code IN (
    'platform.domain.control.ticket_type.read',
    'platform.domain.control.ticket_type.create',
    'platform.domain.control.ticket_type.update',
    'platform.domain.control.ticket_type.delete'
) AND p.status = 1
WHERE r.code IN ('super_admin', 'platform_admin');

-- 4. platform_admin：同步全部 platform 菜单（150001 之后新增节点补绑）
INSERT IGNORE INTO iam_admin_role_menu_relation (role_id, menu_id, created_at)
SELECT r.id, m.id, CURRENT_TIMESTAMP(3)
FROM role r
JOIN iam_admin_menu m ON m.scope = 'platform' AND m.status = 1
WHERE r.code = 'platform_admin';

-- 5. admin 账号：确保 platform_admin 绑定（US-S3-00 规范）；若仍保留 super_admin 则双绑不冲突
INSERT IGNORE INTO user_global_role (user_id, role_id, created_at)
SELECT ua.id, r.id, CURRENT_TIMESTAMP(3)
FROM user_account ua
JOIN role r ON r.code = 'platform_admin'
WHERE ua.username = 'admin';
