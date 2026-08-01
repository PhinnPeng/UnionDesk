-- 业务端：「组织成员」提升为侧栏一级目录（从系统设置下拆出）

UPDATE iam_admin_menu
SET parent_id = NULL,
    node_type = 'catalog',
    route_path = NULL,
    component_key = NULL,
    name = '组织成员',
    icon = 'TeamOutlined',
    order_no = 35,
    hidden = 0,
    status = 1,
    updated_at = CURRENT_TIMESTAMP(3)
WHERE code = 'BUSINESS-DOMAIN-SETTINGS-ORG';

-- 子项顺序：员工 / 角色 / 入域（路径不变）
UPDATE iam_admin_menu
SET parent_id = (SELECT id FROM (SELECT id FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-SETTINGS-ORG' LIMIT 1) t),
    order_no = 10,
    hidden = 0,
    status = 1,
    updated_at = CURRENT_TIMESTAMP(3)
WHERE code = 'BUSINESS-DOMAIN-MEMBERS';

UPDATE iam_admin_menu
SET parent_id = (SELECT id FROM (SELECT id FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-SETTINGS-ORG' LIMIT 1) t),
    order_no = 20,
    hidden = 0,
    status = 1,
    updated_at = CURRENT_TIMESTAMP(3)
WHERE code = 'BUSINESS-DOMAIN-ROLES';

UPDATE iam_admin_menu
SET parent_id = (SELECT id FROM (SELECT id FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-SETTINGS-ORG' LIMIT 1) t),
    order_no = 30,
    hidden = 0,
    status = 1,
    updated_at = CURRENT_TIMESTAMP(3)
WHERE code = 'BUSINESS-DOMAIN-ONBOARDING';

-- 系统设置下剩余分组顺序：通用设置 10、功能配置 20、安全与审计 30
UPDATE iam_admin_menu
SET order_no = 20,
    updated_at = CURRENT_TIMESTAMP(3)
WHERE code = 'BUSINESS-DOMAIN-SETTINGS-FEATURES';

UPDATE iam_admin_menu
SET order_no = 30,
    updated_at = CURRENT_TIMESTAMP(3)
WHERE code = 'BUSINESS-DOMAIN-SETTINGS-SECURITY';

-- 有组织成员叶子的角色，确保绑定一级「组织成员」目录
INSERT IGNORE INTO iam_admin_role_menu_relation (role_id, menu_id, created_at)
SELECT DISTINCT rmr.role_id, org.id, CURRENT_TIMESTAMP(3)
FROM iam_admin_role_menu_relation rmr
JOIN iam_admin_menu leaf ON leaf.id = rmr.menu_id
JOIN iam_admin_menu org ON org.code = 'BUSINESS-DOMAIN-SETTINGS-ORG'
WHERE leaf.code IN (
    'BUSINESS-DOMAIN-MEMBERS',
    'BUSINESS-DOMAIN-ROLES',
    'BUSINESS-DOMAIN-ONBOARDING',
    'BUSINESS-DOMAIN-SETTINGS-ORG'
);
