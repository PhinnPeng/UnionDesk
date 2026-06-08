-- US-S2-01: domain.admin.delete → platform.domain.control.deleted

UPDATE iam_permission
SET code = 'platform.domain.control.deleted',
    name = '删除业务域',
    description = '软删除业务域',
    permission_scope = 'platform',
    action_code = 'platform_domain_control_deleted',
    updated_at = CURRENT_TIMESTAMP(3)
WHERE code = 'domain.admin.delete';

UPDATE iam_admin_menu
SET permission_code = 'platform.domain.control.deleted',
    updated_at = CURRENT_TIMESTAMP(3)
WHERE permission_code = 'domain.admin.delete';
