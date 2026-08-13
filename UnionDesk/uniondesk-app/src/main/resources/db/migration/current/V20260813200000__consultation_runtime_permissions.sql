-- S7 任务 16：在线咨询生产化（consultation runtime）
-- 1) 新增权限码：consultation.view（客服查看）、consultation.convert（转工单）、consultation.customer（客户咨询）
-- 2) 授权：agent/domain_admin 授 view+convert；customer 授 customer
-- 3) 业务端「在线咨询」菜单 + 按钮码 + domain_admin/agent 菜单绑定

-- A) 权限码（幂等）
INSERT INTO iam_permission (
    code,
    name,
    description,
    permission_scope,
    resource_code,
    action_code,
    http_method,
    path_pattern,
    status
)
VALUES
    ('consultation.view', '查看咨询会话', '查看业务域内咨询会话及消息', 'domain', 'consultation', 'view', 'GET', '/api/v1/admin/domains/*/consultations/**', 1),
    ('consultation.convert', '咨询转工单', '将咨询会话转为工单', 'domain', 'consultation', 'convert', 'POST', '/api/v1/admin/domains/*/consultations/*/ticket', 1),
    ('consultation.customer', '客户咨询', '客户发起咨询、查看与回复自己的咨询会话', 'domain', 'consultation', 'customer', NULL, NULL, 1)
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

-- B) 角色授权（幂等）
INSERT INTO iam_role_permission (role_id, permission_id, created_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP(3)
FROM role r
JOIN iam_permission p ON p.status = 1
WHERE p.code IN ('consultation.view', 'consultation.convert')
  AND r.scope = 'domain'
  AND r.code IN ('domain_admin', 'agent')
  AND NOT EXISTS (
      SELECT 1 FROM iam_role_permission existing
      WHERE existing.role_id = r.id AND existing.permission_id = p.id
  );

INSERT INTO iam_role_permission (role_id, permission_id, created_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP(3)
FROM role r
JOIN iam_permission p ON p.status = 1
WHERE p.code = 'consultation.customer'
  AND r.code = 'customer'
  AND NOT EXISTS (
      SELECT 1 FROM iam_role_permission existing
      WHERE existing.role_id = r.id AND existing.permission_id = p.id
  );

-- C) 在线咨询菜单（业务端一级，置于工单队列之后）
INSERT INTO iam_admin_menu (code, node_type, scope, name, route_path, component_key, permission_code, parent_id, order_no, icon, hidden, status, required)
SELECT 'BUSINESS-DOMAIN-CONSULTATION', 'menu', 'business', '在线咨询', '/domain/consultations', './domain/consultations', NULL,
       NULL, 26, 'CustomerServiceOutlined', 0, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-CONSULTATION')
  AND NOT EXISTS (SELECT 1 FROM iam_admin_menu WHERE route_path = '/domain/consultations' AND node_type = 'menu' AND scope = 'business');

-- D) 按钮码（uk_iam_admin_menu_permission_code 全局唯一，需按 permission_code 防重）
INSERT INTO iam_admin_menu (code, node_type, scope, name, permission_code, parent_id, order_no, status, required)
SELECT 'BUSINESS-DOMAIN-CONSULTATION-READ', 'button', 'business', '查看咨询会话', 'consultation.view',
       (SELECT id FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-CONSULTATION' LIMIT 1), 1, 1, 1
WHERE NOT EXISTS (SELECT 1 FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-CONSULTATION-READ')
  AND NOT EXISTS (SELECT 1 FROM iam_admin_menu WHERE node_type = 'button' AND permission_code = 'consultation.view');

INSERT INTO iam_admin_menu (code, node_type, scope, name, permission_code, parent_id, order_no, status, required)
SELECT 'BUSINESS-DOMAIN-CONSULTATION-REPLY', 'button', 'business', '回复咨询', 'consultation.reply',
       (SELECT id FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-CONSULTATION' LIMIT 1), 2, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-CONSULTATION-REPLY')
  AND NOT EXISTS (SELECT 1 FROM iam_admin_menu WHERE node_type = 'button' AND permission_code = 'consultation.reply');

INSERT INTO iam_admin_menu (code, node_type, scope, name, permission_code, parent_id, order_no, status, required)
SELECT 'BUSINESS-DOMAIN-CONSULTATION-CONVERT', 'button', 'business', '咨询转工单', 'consultation.convert',
       (SELECT id FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-CONSULTATION' LIMIT 1), 3, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-CONSULTATION-CONVERT')
  AND NOT EXISTS (SELECT 1 FROM iam_admin_menu WHERE node_type = 'button' AND permission_code = 'consultation.convert');

-- E) domain_admin / agent 菜单绑定（含按钮）
INSERT IGNORE INTO iam_admin_role_menu_relation (role_id, menu_id, created_at)
SELECT r.id, m.id, CURRENT_TIMESTAMP(3)
FROM role r
JOIN iam_admin_menu m ON m.status = 1 AND m.scope = 'business'
WHERE r.scope = 'domain'
  AND r.code IN ('domain_admin', 'agent')
  AND (
      m.code = 'BUSINESS-DOMAIN-CONSULTATION'
      OR m.code LIKE 'BUSINESS-DOMAIN-CONSULTATION-%'
  );
