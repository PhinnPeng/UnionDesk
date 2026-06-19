-- 补全 V202606170004：P0 演示菜单按钮仍挂 domain/shared 权限码，显式勾选时 super_admin 保存仍失败

UPDATE iam_admin_menu
SET permission_code = 'platform.domain.control.read',
    required = 0,
    updated_at = CURRENT_TIMESTAMP(3)
WHERE node_type = 'button'
  AND scope = 'platform'
  AND permission_code IN ('ticket.read', 'ticket.reply', 'inbox.read', 'attachment.download', 'attachment.upload');

UPDATE iam_admin_menu
SET permission_code = 'platform.system_config.read',
    required = 0,
    updated_at = CURRENT_TIMESTAMP(3)
WHERE node_type = 'button'
  AND scope = 'platform'
  AND permission_code = 'domain.config.read';

UPDATE iam_admin_menu
SET permission_code = 'platform.domain.control.overview',
    required = 0,
    updated_at = CURRENT_TIMESTAMP(3)
WHERE node_type = 'button'
  AND scope = 'platform'
  AND permission_code = 'domain.sla.read';

INSERT IGNORE INTO iam_role_permission (role_id, permission_id, created_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP(3)
FROM role r
JOIN iam_permission p ON p.code IN (
    'platform.domain.control.read',
    'platform.domain.control.overview',
    'platform.system_config.read'
) AND p.status = 1
WHERE r.code IN ('super_admin', 'platform_admin');
