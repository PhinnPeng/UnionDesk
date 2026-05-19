-- Backfill reset-password button access for default platform roles.
INSERT IGNORE INTO iam_admin_role_menu_relation (role_id, menu_id, created_at)
SELECT r.id, btn.id, CURRENT_TIMESTAMP(3)
FROM role r
JOIN iam_admin_menu btn ON btn.node_type = 'button'
JOIN iam_admin_menu parent ON parent.id = btn.parent_id
WHERE r.code IN ('super_admin', 'platform_admin')
  AND parent.route_path = '/platform/user'
  AND btn.permission_code = 'platform.user.reset_password'
  AND btn.status = 1;
