-- 业务端菜单扁平化 + 口径调整：
-- 1) 取消「域治理」catalog，子菜单上移为一级
-- 2) 「工作台/首页概览」改为一级菜单「概览」；原「域治理/概览」改为「运营概览」避免重名

-- A) 域治理子菜单上移
UPDATE iam_admin_menu
SET parent_id = NULL,
    updated_at = CURRENT_TIMESTAMP(3)
WHERE scope = 'business'
  AND node_type = 'menu'
  AND (
      code LIKE 'BUSINESS-DOMAIN-%'
      OR parent_id = (SELECT id FROM (SELECT id FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-GOVERN-CATALOG' LIMIT 1) t)
  );

UPDATE iam_admin_menu
SET name = '运营概览',
    order_no = 20,
    updated_at = CURRENT_TIMESTAMP(3)
WHERE code = 'BUSINESS-DOMAIN-OVERVIEW';

UPDATE iam_admin_menu
SET order_no = CASE code
    WHEN 'BUSINESS-DOMAIN-BASIC' THEN 30
    WHEN 'BUSINESS-DOMAIN-MEMBERS' THEN 40
    WHEN 'BUSINESS-DOMAIN-ROLES' THEN 50
    WHEN 'BUSINESS-DOMAIN-CUSTOMERS' THEN 60
    WHEN 'BUSINESS-DOMAIN-ONBOARDING' THEN 70
    WHEN 'BUSINESS-DOMAIN-TICKET-CONFIG' THEN 80
    WHEN 'BUSINESS-DOMAIN-BLOCKWORDS' THEN 90
    WHEN 'BUSINESS-DOMAIN-NOTIFICATIONS' THEN 100
    WHEN 'BUSINESS-DOMAIN-CONFIG' THEN 110
    WHEN 'BUSINESS-DOMAIN-AUDIT-LOGS' THEN 120
    WHEN 'BUSINESS-DOMAIN-LOGIN-LOGS' THEN 130
    ELSE order_no
END,
    updated_at = CURRENT_TIMESTAMP(3)
WHERE code LIKE 'BUSINESS-DOMAIN-%'
  AND node_type = 'menu'
  AND code <> 'BUSINESS-DOMAIN-OVERVIEW';

-- 隐藏「域治理」目录（保留记录便于回滚，不再出现在侧栏 / 菜单管理有效树）
UPDATE iam_admin_menu
SET hidden = 1,
    status = 0,
    updated_at = CURRENT_TIMESTAMP(3)
WHERE code = 'BUSINESS-DOMAIN-GOVERN-CATALOG';

-- B) 工作台/首页 → 一级「概览」
UPDATE iam_admin_menu
SET name = '概览',
    parent_id = NULL,
    order_no = 10,
    icon = 'HomeOutlined',
    required = 1,
    updated_at = CURRENT_TIMESTAMP(3)
WHERE code = 'BUSINESS-HOME-MENU'
   OR (route_path = '/home' AND node_type = 'menu' AND scope = 'business');

UPDATE iam_admin_menu
SET name = '查看概览',
    updated_at = CURRENT_TIMESTAMP(3)
WHERE code = 'BUSINESS-HOME-BTN-READ'
   OR (node_type = 'button' AND permission_code = 'domain.home.read' AND scope = 'business');

UPDATE iam_admin_menu
SET hidden = 1,
    status = 0,
    updated_at = CURRENT_TIMESTAMP(3)
WHERE code = 'BUSINESS-HOME-CATALOG';
