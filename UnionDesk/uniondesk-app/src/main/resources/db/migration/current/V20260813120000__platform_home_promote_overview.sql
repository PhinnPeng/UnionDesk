-- 平台端「首页概览」上移为顶层菜单「概览」，删除「平台首页」目录（任务 08-13-platform-home-overview-promote）
-- 背景：目录下仅一个子菜单（单子节点包装冗余）；更名「概览」与前端组件注册表 label 一致；前端零改动（菜单名由 iam_admin_menu 渲染）。

-- 1. 上移：首页菜单脱离目录、更名「概览」、置顶层首位
UPDATE `iam_admin_menu`
SET parent_id = NULL,
    name = '概览',
    order_no = 1,
    updated_at = CURRENT_TIMESTAMP(3)
WHERE route_path = '/platform/home'
  AND node_type = 'menu'
  AND scope = 'platform';

-- 2. 角色菜单关联联动：删除指向「平台首页」目录树（目录及其遗留子节点）的关联；
--    「概览」菜单的既有关联保留（角色对首页的可见性不变）。
DELETE rel
FROM `iam_admin_role_menu_relation` rel
INNER JOIN `iam_admin_menu` m ON m.id = rel.menu_id
WHERE m.code = 'PLATFORM-HOME-CATALOG'
   OR m.parent_id = (
       SELECT id FROM (
           SELECT id FROM `iam_admin_menu` WHERE code = 'PLATFORM-HOME-CATALOG' LIMIT 1
       ) AS home_catalog
   );

-- 3. 删除目录树遗留子节点（防御：目录下除已上移菜单外的其他节点；当前无）
DELETE m
FROM `iam_admin_menu` m
WHERE m.parent_id = (
    SELECT id FROM (
        SELECT id FROM `iam_admin_menu` WHERE code = 'PLATFORM-HOME-CATALOG' LIMIT 1
    ) AS home_catalog
);

-- 4. 删除目录
DELETE FROM `iam_admin_menu` WHERE code = 'PLATFORM-HOME-CATALOG';
