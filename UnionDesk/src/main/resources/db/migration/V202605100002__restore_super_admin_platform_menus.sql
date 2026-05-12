INSERT IGNORE INTO iam_admin_role_menu_relation (role_id, menu_id, created_at)
SELECT r.id, m.id, CURRENT_TIMESTAMP(3)
FROM role r
JOIN iam_admin_menu m ON m.node_type = 'menu'
WHERE r.code = 'super_admin'
  AND m.scope = 'platform'
  AND m.status = 1;
