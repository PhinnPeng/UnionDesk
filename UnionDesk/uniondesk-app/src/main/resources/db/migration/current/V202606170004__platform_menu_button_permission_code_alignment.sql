-- US-S3-01 收尾：平台 scope 按钮 permission_code 与 AdminPermissionCatalog 对齐
-- 背景：super_admin（role.scope=global）保存角色权限时，ensureRoleMenuScopeAlignment 要求
--       所有 platform 按钮权限码 permissionScope=platform 且 code 以 platform. 开头。
--       历史 seed / 部分迁移未同步 iam_admin_menu.permission_code，导致 PUT /roles/{id}/permissions 报「角色范围与权限码不一致」。

-- 1. 业务域列表 / CRUD / 控制台入口：对齐 platform.*
UPDATE iam_admin_menu
SET permission_code = 'platform.domain.list.read',
    updated_at = CURRENT_TIMESTAMP(3)
WHERE node_type = 'button'
  AND scope = 'platform'
  AND permission_code IN ('domain.read', 'domain.admin.read', 'domain.admin.list.read');

UPDATE iam_admin_menu
SET permission_code = 'platform.domain.create',
    updated_at = CURRENT_TIMESTAMP(3)
WHERE node_type = 'button'
  AND scope = 'platform'
  AND permission_code = 'domain.admin.create';

UPDATE iam_admin_menu
SET permission_code = 'platform.domain.control.entry',
    updated_at = CURRENT_TIMESTAMP(3)
WHERE node_type = 'button'
  AND scope = 'platform'
  AND permission_code IN ('domain.admin.detail', 'domain.admin.detail.read');

UPDATE iam_admin_menu
SET permission_code = 'platform.domain.control.general.update',
    updated_at = CURRENT_TIMESTAMP(3)
WHERE node_type = 'button'
  AND scope = 'platform'
  AND permission_code = 'domain.admin.update';

UPDATE iam_admin_menu
SET permission_code = 'platform.domain.control.general.delete',
    updated_at = CURRENT_TIMESTAMP(3)
WHERE node_type = 'button'
  AND scope = 'platform'
  AND permission_code IN ('domain.admin.delete', 'platform.domain.control.deleted');

UPDATE iam_admin_menu
SET permission_code = 'platform.domain.control.customer.create',
    updated_at = CURRENT_TIMESTAMP(3)
WHERE node_type = 'button'
  AND scope = 'platform'
  AND permission_code IN ('domain.customer.create', 'platform.domain.customer.create');

UPDATE iam_admin_menu
SET permission_code = 'platform.domain.control.customer.read',
    updated_at = CURRENT_TIMESTAMP(3)
WHERE node_type = 'button'
  AND scope = 'platform'
  AND permission_code IN ('platform.domain.customer.read');

UPDATE iam_admin_menu
SET permission_code = 'platform.domain.control.customer.update-status',
    updated_at = CURRENT_TIMESTAMP(3)
WHERE node_type = 'button'
  AND scope = 'platform'
  AND permission_code IN ('platform.domain.customer.update', 'platform.domain.customer.update-status');

-- 2. 首页 / 日志：Catalog 中无 platform.home.query / query_legacy，改用已注册码
UPDATE iam_admin_menu
SET permission_code = 'platform.dashboard.read',
    updated_at = CURRENT_TIMESTAMP(3)
WHERE node_type = 'button'
  AND scope = 'platform'
  AND permission_code = 'platform.home.query';

UPDATE iam_admin_menu
SET permission_code = 'platform.log.audit.read',
    updated_at = CURRENT_TIMESTAMP(3)
WHERE node_type = 'button'
  AND scope = 'platform'
  AND permission_code IN ('platform.audit_log.read', 'platform.audit_log.query_legacy');

UPDATE iam_admin_menu
SET permission_code = 'platform.log.login.read',
    updated_at = CURRENT_TIMESTAMP(3)
WHERE node_type = 'button'
  AND scope = 'platform'
  AND permission_code = 'platform.login_log.read';

-- 3. 尚无 platform.* 等价码的 P0 演示按钮：取消 required，避免 global 角色保存时被强制注入
UPDATE iam_admin_menu
SET required = 0,
    updated_at = CURRENT_TIMESTAMP(3)
WHERE node_type = 'button'
  AND scope = 'platform'
  AND permission_code IN (
      'domain.config.read',
      'domain.sla.read',
      'ticket.read',
      'ticket.reply',
      'inbox.read',
      'attachment.download'
  );

-- 4. iam_role_permission：旧码 → 新码（super_admin / platform_admin）
INSERT IGNORE INTO iam_role_permission (role_id, permission_id, created_at)
SELECT rp.role_id, p_new.id, CURRENT_TIMESTAMP(3)
FROM iam_role_permission rp
JOIN iam_permission p_old ON p_old.id = rp.permission_id
JOIN iam_permission p_new ON p_new.status = 1
JOIN role r ON r.id = rp.role_id AND r.code IN ('super_admin', 'platform_admin')
WHERE (p_old.code = 'domain.read' AND p_new.code = 'platform.domain.list.read')
   OR (p_old.code = 'domain.admin.read' AND p_new.code = 'platform.domain.list.read')
   OR (p_old.code = 'domain.admin.list.read' AND p_new.code = 'platform.domain.list.read')
   OR (p_old.code = 'domain.admin.create' AND p_new.code = 'platform.domain.create')
   OR (p_old.code = 'domain.admin.detail.read' AND p_new.code = 'platform.domain.control.entry')
   OR (p_old.code = 'domain.admin.update' AND p_new.code = 'platform.domain.control.general.update')
   OR (p_old.code IN ('domain.admin.delete', 'platform.domain.control.deleted') AND p_new.code = 'platform.domain.control.general.delete')
   OR (p_old.code = 'domain.customer.create' AND p_new.code = 'platform.domain.control.customer.create')
   OR (p_old.code = 'platform.domain.customer.create' AND p_new.code = 'platform.domain.control.customer.create')
   OR (p_old.code = 'platform.home.query' AND p_new.code = 'platform.dashboard.read')
   OR (p_old.code IN ('platform.audit_log.read', 'platform.audit_log.query_legacy') AND p_new.code = 'platform.log.audit.read')
   OR (p_old.code = 'platform.login_log.read' AND p_new.code = 'platform.log.login.read');

-- 5. platform.dashboard.read 直授（首页按钮改码后）
INSERT IGNORE INTO iam_role_permission (role_id, permission_id, created_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP(3)
FROM role r
JOIN iam_permission p ON p.code = 'platform.dashboard.read' AND p.status = 1
WHERE r.code IN ('super_admin', 'platform_admin');
