-- 权限码命名规范统一：连字符 action → 下划线（全仓其余 action 均为下划线）
-- 涉及两个码：platform.domain.control.general.update-status、platform.domain.control.customer.update-status
-- 由 2026-08-12 评审决策 D2（连字符码统一迁移，落盘 08-11-prd-doc-fixes）发起

-- 1. iam_permission.code
UPDATE iam_permission
SET code = 'platform.domain.control.general.update_status',
    updated_at = CURRENT_TIMESTAMP(3)
WHERE code = 'platform.domain.control.general.update-status';

UPDATE iam_permission
SET code = 'platform.domain.control.customer.update_status',
    updated_at = CURRENT_TIMESTAMP(3)
WHERE code = 'platform.domain.control.customer.update-status';

-- 2. iam_admin_menu.permission_code（按钮节点引用同步）
UPDATE iam_admin_menu
SET permission_code = 'platform.domain.control.general.update_status',
    updated_at = CURRENT_TIMESTAMP(3)
WHERE permission_code = 'platform.domain.control.general.update-status';

UPDATE iam_admin_menu
SET permission_code = 'platform.domain.control.customer.update_status',
    updated_at = CURRENT_TIMESTAMP(3)
WHERE permission_code = 'platform.domain.control.customer.update-status';
