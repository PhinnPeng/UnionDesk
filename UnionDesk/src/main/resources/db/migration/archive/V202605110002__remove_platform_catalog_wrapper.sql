-- ============================================================
-- 去掉"平台管理" catalog 包装层，将所有平台菜单项提升为根节点
--
-- 原因：侧边栏不显示 catalog 节点（loadPermissionSnapshot 只取 nodeType='menu'），
-- 导致菜单管理页与侧边栏结构不一致。
-- 层级约束：menu 的父节点可以是 catalog 或 menu，设为 null 后成为根节点是合法的。
-- ============================================================

-- 1. 将所有"平台管理" catalog 下的子菜单 parent_id 设为 null（提升为根节点）
UPDATE iam_admin_menu
SET parent_id = NULL
WHERE parent_id = (
    SELECT id FROM (SELECT id FROM iam_admin_menu WHERE node_type = 'catalog' AND name = '平台管理' AND parent_id IS NULL LIMIT 1) AS cat
);

-- 2. 删除"平台管理" catalog 与角色的关联关系
DELETE FROM iam_admin_role_menu_relation
WHERE menu_id = (
    SELECT id FROM (SELECT id FROM iam_admin_menu WHERE node_type = 'catalog' AND name = '平台管理' AND parent_id IS NULL LIMIT 1) AS cat
);

-- 3. 删除"平台管理" catalog 节点
DELETE FROM iam_admin_menu
WHERE node_type = 'catalog'
  AND name = '平台管理'
  AND parent_id IS NULL;
