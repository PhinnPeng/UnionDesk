-- ============================================================
-- 补回 /platform/home 的系统保护标记与 super_admin 授权
-- ============================================================
UPDATE iam_admin_menu
SET required = 1
WHERE route_path = '/platform/home';

INSERT IGNORE INTO iam_admin_role_menu_relation (role_id, menu_id, created_at)
SELECT r.id, m.id, CURRENT_TIMESTAMP(3)
FROM role r
JOIN iam_admin_menu m ON m.route_path = '/platform/home' AND m.node_type = 'menu'
WHERE r.code = 'super_admin';

INSERT IGNORE INTO iam_admin_role_menu_relation (role_id, menu_id, created_at)
SELECT r.id, btn.id, CURRENT_TIMESTAMP(3)
FROM role r
JOIN iam_admin_menu home ON home.route_path = '/platform/home' AND home.node_type = 'menu'
JOIN iam_admin_menu btn ON btn.parent_id = home.id
                        AND btn.node_type = 'button'
                        AND btn.required = 1
WHERE r.code = 'super_admin';
