-- 幂等：仅修正仍然保留 /system/% 路径的平台菜单，已改为 /platform/% 的记录不会重复处理
UPDATE iam_admin_menu
SET route_path = REPLACE(route_path, '/system/', '/platform/')
WHERE scope = 'platform'
  AND node_type = 'menu'
  AND route_path LIKE '/system/%';
