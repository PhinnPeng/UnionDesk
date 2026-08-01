-- 域管控事项状态：补齐菜单按钮 + 角色菜单绑定（与事项类型/属性一致）

INSERT INTO iam_admin_menu (code, node_type, scope, name, route_path, component_key, permission_code, parent_id, order_no, icon, hidden, status, required)
SELECT 'PLATFORM-DOMAIN-CONTROL-TICKET-STATUS', 'catalog', 'platform', '事项状态', NULL, NULL, NULL,
       (SELECT id FROM iam_admin_menu WHERE code = 'PLATFORM-DOMAIN-DETAIL' LIMIT 1),
       46, NULL, 0, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM iam_admin_menu WHERE code = 'PLATFORM-DOMAIN-CONTROL-TICKET-STATUS');

UPDATE iam_admin_menu
SET parent_id = (SELECT id FROM (SELECT id FROM iam_admin_menu WHERE code = 'PLATFORM-DOMAIN-DETAIL' LIMIT 1) AS detail_menu),
    name = '事项状态',
    scope = 'platform',
    node_type = 'catalog',
    status = 1
WHERE code = 'PLATFORM-DOMAIN-CONTROL-TICKET-STATUS';

INSERT INTO iam_admin_menu (code, node_type, scope, name, permission_code, parent_id, order_no, status, required)
SELECT 'TMP-BTN-DOMAIN-TS-READ', 'button', 'platform', '查看事项状态', 'platform.domain.control.ticket_status.read',
       (SELECT id FROM iam_admin_menu WHERE code = 'PLATFORM-DOMAIN-CONTROL-TICKET-STATUS' LIMIT 1),
       1, 1, 1
WHERE NOT EXISTS (
    SELECT 1 FROM iam_admin_menu WHERE permission_code = 'platform.domain.control.ticket_status.read' AND node_type = 'button'
);

INSERT INTO iam_admin_menu (code, node_type, scope, name, permission_code, parent_id, order_no, status, required)
SELECT 'TMP-BTN-DOMAIN-TS-CREATE', 'button', 'platform', '创建事项状态', 'platform.domain.control.ticket_status.create',
       (SELECT id FROM iam_admin_menu WHERE code = 'PLATFORM-DOMAIN-CONTROL-TICKET-STATUS' LIMIT 1),
       2, 1, 0
WHERE NOT EXISTS (
    SELECT 1 FROM iam_admin_menu WHERE permission_code = 'platform.domain.control.ticket_status.create' AND node_type = 'button'
);

INSERT INTO iam_admin_menu (code, node_type, scope, name, permission_code, parent_id, order_no, status, required)
SELECT 'TMP-BTN-DOMAIN-TS-UPDATE', 'button', 'platform', '编辑事项状态', 'platform.domain.control.ticket_status.update',
       (SELECT id FROM iam_admin_menu WHERE code = 'PLATFORM-DOMAIN-CONTROL-TICKET-STATUS' LIMIT 1),
       3, 1, 0
WHERE NOT EXISTS (
    SELECT 1 FROM iam_admin_menu WHERE permission_code = 'platform.domain.control.ticket_status.update' AND node_type = 'button'
);

INSERT INTO iam_admin_menu (code, node_type, scope, name, permission_code, parent_id, order_no, status, required)
SELECT 'TMP-BTN-DOMAIN-TS-DELETE', 'button', 'platform', '删除事项状态', 'platform.domain.control.ticket_status.delete',
       (SELECT id FROM iam_admin_menu WHERE code = 'PLATFORM-DOMAIN-CONTROL-TICKET-STATUS' LIMIT 1),
       4, 1, 0
WHERE NOT EXISTS (
    SELECT 1 FROM iam_admin_menu WHERE permission_code = 'platform.domain.control.ticket_status.delete' AND node_type = 'button'
);

-- 绑定到已拥有事项类型按钮的角色（super_admin / platform_admin 等）
INSERT INTO iam_admin_role_menu_relation (role_id, menu_id, created_at)
SELECT DISTINCT rm.role_id, btn.id, CURRENT_TIMESTAMP(3)
FROM iam_admin_role_menu_relation rm
JOIN iam_admin_menu type_btn ON type_btn.id = rm.menu_id
    AND type_btn.permission_code = 'platform.domain.control.ticket_type.read'
    AND type_btn.node_type = 'button'
JOIN iam_admin_menu btn ON btn.permission_code IN (
    'platform.domain.control.ticket_status.read',
    'platform.domain.control.ticket_status.create',
    'platform.domain.control.ticket_status.update',
    'platform.domain.control.ticket_status.delete'
) AND btn.node_type = 'button' AND btn.status = 1
WHERE NOT EXISTS (
    SELECT 1 FROM iam_admin_role_menu_relation existing
    WHERE existing.role_id = rm.role_id AND existing.menu_id = btn.id
);

INSERT INTO iam_admin_role_menu_relation (role_id, menu_id, created_at)
SELECT r.id, catalog.id, CURRENT_TIMESTAMP(3)
FROM role r
JOIN iam_admin_menu catalog ON catalog.code = 'PLATFORM-DOMAIN-CONTROL-TICKET-STATUS' AND catalog.status = 1
WHERE r.code IN ('super_admin', 'platform_admin')
  AND NOT EXISTS (
      SELECT 1 FROM iam_admin_role_menu_relation existing
      WHERE existing.role_id = r.id AND existing.menu_id = catalog.id
  );

-- 再次确保角色 API 权限存在
INSERT INTO iam_role_permission (role_id, permission_id, created_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP(3)
FROM role r
JOIN iam_permission p ON p.code IN (
    'platform.domain.control.ticket_status.read',
    'platform.domain.control.ticket_status.create',
    'platform.domain.control.ticket_status.update',
    'platform.domain.control.ticket_status.delete'
)
WHERE r.code IN ('super_admin', 'platform_admin')
  AND NOT EXISTS (
      SELECT 1 FROM iam_role_permission rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
