-- 团队模板仅在事项配置页内 sider 进入，主侧栏菜单隐藏
UPDATE iam_admin_menu
SET hidden = 1
WHERE route_path = '/platform/ticket-config/templates'
  AND (hidden IS NULL OR hidden = 0);
