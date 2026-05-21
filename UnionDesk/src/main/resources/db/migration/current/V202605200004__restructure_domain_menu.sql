-- Create new catalog for Domain Management
INSERT INTO iam_admin_menu (code, node_type, scope, name, route_path, order_no, icon, status, required)
VALUES ('PLATFORM-DOMAIN-CATALOG', 'catalog', 'platform', '业务域管理', NULL, 6, 'GlobalOutlined', 1, 0);

-- Move 'Business Domain List' under the new catalog
UPDATE iam_admin_menu
SET parent_id = (SELECT id FROM (SELECT id FROM iam_admin_menu WHERE code = 'PLATFORM-DOMAIN-CATALOG') x),
    name = '业务域列表'
WHERE code = 'ADM0000000043';
