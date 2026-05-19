-- ============================================================
-- 补回 super_admin 的平台端 CRUD 按钮权限
-- 说明：
-- 003 号迁移保留了目标菜单，但遗漏了菜单/角色/角色授权的 CRUD 按钮
-- 这里按 permission_code 显式补齐，避免依赖 parent_id 或 required 标记
-- ============================================================
INSERT IGNORE INTO iam_admin_role_menu_relation (role_id, menu_id, created_at)
SELECT r.id, btn.id, CURRENT_TIMESTAMP(3)
FROM role r
JOIN iam_admin_menu btn ON btn.node_type = 'button'
WHERE r.code = 'super_admin'
  AND btn.status = 1
  AND btn.permission_code IN (
      'platform.menu.create',
      'platform.menu.update',
      'platform.menu.delete',
      'platform.role.create',
      'platform.role.update',
      'platform.role.delete',
      'platform.role_permission.read',
      'platform.role_permission.update',
      'platform.role.bind'
  );
