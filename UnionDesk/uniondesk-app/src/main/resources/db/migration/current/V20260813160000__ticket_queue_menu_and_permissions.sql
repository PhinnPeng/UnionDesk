-- S5 任务 7：工单队列（staff-ticket-queue）
-- 1) agent 补齐 ticket.merge 与工单队列所需只读权限（动态状态/优先级下拉）
-- 2) 业务端「工单队列」菜单（含按钮码）+ domain_admin/agent 菜单绑定

-- A) agent 权限补齐（域级权限，幂等）
INSERT INTO iam_role_permission (role_id, permission_id, created_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP(3)
FROM role r
JOIN iam_permission p ON p.status = 1
WHERE r.code = 'agent'
  AND p.code IN (
      'ticket.merge',
      'domain.ticket_status.read',
      'domain.priority_level.read'
  )
  AND NOT EXISTS (
      SELECT 1 FROM iam_role_permission existing
      WHERE existing.role_id = r.id AND existing.permission_id = p.id
  );

-- B) 工单队列菜单（业务端一级，置于运营概览之后）
INSERT INTO iam_admin_menu (code, node_type, scope, name, route_path, component_key, permission_code, parent_id, order_no, icon, hidden, status, required)
SELECT 'BUSINESS-DOMAIN-TICKET-QUEUE', 'menu', 'business', '工单队列', '/domain/ticket-queue', './domain/ticket-queue', NULL,
       NULL, 25, 'ProfileOutlined', 0, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-TICKET-QUEUE')
  AND NOT EXISTS (SELECT 1 FROM iam_admin_menu WHERE route_path = '/domain/ticket-queue' AND node_type = 'menu' AND scope = 'business');

-- C) 按钮码（uk_iam_admin_menu_permission_code 全局唯一，需按 permission_code 防重）
INSERT INTO iam_admin_menu (code, node_type, scope, name, permission_code, parent_id, order_no, status, required)
SELECT 'BUSINESS-DOMAIN-TICKET-QUEUE-READ', 'button', 'business', '查看工单队列', 'ticket.view.domain_all',
       (SELECT id FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-TICKET-QUEUE' LIMIT 1), 1, 1, 1
WHERE NOT EXISTS (SELECT 1 FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-TICKET-QUEUE-READ')
  AND NOT EXISTS (SELECT 1 FROM iam_admin_menu WHERE node_type = 'button' AND permission_code = 'ticket.view.domain_all');

INSERT INTO iam_admin_menu (code, node_type, scope, name, permission_code, parent_id, order_no, status, required)
SELECT 'BUSINESS-DOMAIN-TICKET-QUEUE-CLAIM', 'button', 'business', '领取工单', 'ticket.claim',
       (SELECT id FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-TICKET-QUEUE' LIMIT 1), 2, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-TICKET-QUEUE-CLAIM')
  AND NOT EXISTS (SELECT 1 FROM iam_admin_menu WHERE node_type = 'button' AND permission_code = 'ticket.claim');

INSERT INTO iam_admin_menu (code, node_type, scope, name, permission_code, parent_id, order_no, status, required)
SELECT 'BUSINESS-DOMAIN-TICKET-QUEUE-ASSIGN', 'button', 'business', '指派工单', 'ticket.assign',
       (SELECT id FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-TICKET-QUEUE' LIMIT 1), 3, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-TICKET-QUEUE-ASSIGN')
  AND NOT EXISTS (SELECT 1 FROM iam_admin_menu WHERE node_type = 'button' AND permission_code = 'ticket.assign');

INSERT INTO iam_admin_menu (code, node_type, scope, name, permission_code, parent_id, order_no, status, required)
SELECT 'BUSINESS-DOMAIN-TICKET-QUEUE-REPLY', 'button', 'business', '回复工单', 'ticket.reply',
       (SELECT id FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-TICKET-QUEUE' LIMIT 1), 4, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-TICKET-QUEUE-REPLY')
  AND NOT EXISTS (SELECT 1 FROM iam_admin_menu WHERE node_type = 'button' AND permission_code = 'ticket.reply');

INSERT INTO iam_admin_menu (code, node_type, scope, name, permission_code, parent_id, order_no, status, required)
SELECT 'BUSINESS-DOMAIN-TICKET-QUEUE-CLOSE', 'button', 'business', '关闭工单', 'ticket.close',
       (SELECT id FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-TICKET-QUEUE' LIMIT 1), 5, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-TICKET-QUEUE-CLOSE')
  AND NOT EXISTS (SELECT 1 FROM iam_admin_menu WHERE node_type = 'button' AND permission_code = 'ticket.close');

INSERT INTO iam_admin_menu (code, node_type, scope, name, permission_code, parent_id, order_no, status, required)
SELECT 'BUSINESS-DOMAIN-TICKET-QUEUE-MERGE', 'button', 'business', '合并工单', 'ticket.merge',
       (SELECT id FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-TICKET-QUEUE' LIMIT 1), 6, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-TICKET-QUEUE-MERGE')
  AND NOT EXISTS (SELECT 1 FROM iam_admin_menu WHERE node_type = 'button' AND permission_code = 'ticket.merge');

-- D) domain_admin / agent 菜单绑定（含按钮）
INSERT IGNORE INTO iam_admin_role_menu_relation (role_id, menu_id, created_at)
SELECT r.id, m.id, CURRENT_TIMESTAMP(3)
FROM role r
JOIN iam_admin_menu m ON m.status = 1 AND m.scope = 'business'
WHERE r.scope = 'domain'
  AND r.code IN ('domain_admin', 'agent')
  AND (
      m.code = 'BUSINESS-DOMAIN-TICKET-QUEUE'
      OR m.code LIKE 'BUSINESS-DOMAIN-TICKET-QUEUE-%'
  );
