-- S6 任务：org-config-merge — 组织配置占位页并入组织架构页
-- 1) 隐藏「组织配置」菜单（ADM0000000071）：
--    - status=0：菜单快照查询（AdminMenuMapper.selectAuthorizedByRoleCodes 按 m.status=1 过滤）不再返回该菜单，
--      前端不再生成 /platform/org-config 路由，页面目录删除后不会产生悬空路由
--    - hidden=1：菜单管理页中该菜单以隐藏状态展示
UPDATE iam_admin_menu
SET hidden = 1,
    status = 0
WHERE code = 'ADM0000000071'
  AND node_type = 'menu'
  AND route_path = '/platform/org-config';
