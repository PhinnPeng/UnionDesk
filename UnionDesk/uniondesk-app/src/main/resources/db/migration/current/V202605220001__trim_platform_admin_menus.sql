-- 平台侧菜单精简：仅保留 平台首页、组织管理、业务域管理、日志审计、权限管理 及其子节点

INSERT INTO iam_admin_menu (code, node_type, scope, name, route_path, order_no, icon, status, required)
SELECT 'PLATFORM-HOME-CATALOG', 'catalog', 'platform', '平台首页', NULL, 1, 'DashboardOutlined', 1, 0
WHERE NOT EXISTS (SELECT 1 FROM iam_admin_menu WHERE code = 'PLATFORM-HOME-CATALOG');

UPDATE iam_admin_menu
SET parent_id = (SELECT id FROM (SELECT id FROM iam_admin_menu WHERE code = 'PLATFORM-HOME-CATALOG' LIMIT 1) AS home_catalog),
    order_no = 1,
    name = '首页概览',
    scope = 'platform',
    status = 1,
    hidden = 0
WHERE route_path = '/platform/home'
  AND node_type = 'menu';

UPDATE iam_admin_menu
SET status = 1,
    hidden = 1
WHERE route_path = '/platform/audit-logs'
  AND node_type = 'menu';

DROP TEMPORARY TABLE IF EXISTS tmp_keep_admin_menu;
CREATE TEMPORARY TABLE tmp_keep_admin_menu (id BIGINT PRIMARY KEY);

INSERT INTO tmp_keep_admin_menu (id)
WITH RECURSIVE keep_tree AS (
    SELECT id
    FROM iam_admin_menu
    WHERE code IN (
        'PLATFORM-HOME-CATALOG',
        'ADM0000000070',
        'PLATFORM-DOMAIN-CATALOG',
        'PLATFORM-AUDIT-CATALOG',
        'ADM0000000048'
    )
    UNION ALL
    SELECT m.id
    FROM iam_admin_menu m
    INNER JOIN keep_tree k ON m.parent_id = k.id
)
SELECT id FROM keep_tree;

DELETE rel
FROM iam_admin_role_menu_relation rel
INNER JOIN iam_admin_menu m ON m.id = rel.menu_id
WHERE m.id NOT IN (SELECT id FROM tmp_keep_admin_menu);

SET @fk_checks = @@FOREIGN_KEY_CHECKS;
SET FOREIGN_KEY_CHECKS = 0;

DELETE m
FROM iam_admin_menu m
WHERE m.id NOT IN (SELECT id FROM tmp_keep_admin_menu);

SET FOREIGN_KEY_CHECKS = @fk_checks;

DROP TEMPORARY TABLE IF EXISTS tmp_keep_admin_menu;

INSERT IGNORE INTO iam_admin_role_menu_relation (role_id, menu_id, created_at)
SELECT r.id, m.id, CURRENT_TIMESTAMP(3)
FROM role r
JOIN iam_admin_menu m ON m.scope = 'platform' AND m.status = 1
WHERE r.code = 'super_admin';
