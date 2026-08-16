-- 移除业务域端「运营概览」菜单与页面（2026-08-16 用户决策：删除运营概览）
-- 幂等：以 code 为键；先删子节点（按钮）再删父节点（菜单），避免 FK 约束

-- 1. 删除「运营概览」菜单及按钮的角色绑定
DELETE FROM iam_admin_role_menu_relation
WHERE menu_id IN (
    SELECT id FROM iam_admin_menu
    WHERE code IN ('BUSINESS-DOMAIN-OVERVIEW', 'BUSINESS-DOMAIN-OVERVIEW-READ')
);

-- 2. 先删子节点（按钮），再删父节点（菜单）
DELETE FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-OVERVIEW-READ';
DELETE FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-OVERVIEW';

-- 3. 删除 domain.overview.read 权限码的角色绑定（菜单已无，权限码保留注册但不再授权）
DELETE FROM iam_role_permission
WHERE permission_id IN (SELECT id FROM iam_permission WHERE code = 'domain.overview.read');
