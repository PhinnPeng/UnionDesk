-- 权限管理：由可访问 menu 改为 catalog 目录（无 /platform/permission 路由）

UPDATE iam_admin_menu
SET node_type = 'catalog',
    route_path = NULL,
    component_key = NULL,
    permission_code = NULL
WHERE code = 'ADM0000000048'
  AND node_type = 'menu';
