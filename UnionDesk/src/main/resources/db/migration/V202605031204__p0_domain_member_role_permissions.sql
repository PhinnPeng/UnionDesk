-- P0 域成员与域角色权限种子
-- 仅做增量补齐，不回滚已有数据

INSERT IGNORE INTO permission_item (code, name, module, type)
VALUES
    ('user_manage:menu', '域成员管理菜单', 'domain', 'menu'),
    ('domain_config:menu', '域配置菜单', 'domain', 'menu'),
    ('log:menu', '日志查看菜单', 'domain', 'menu'),
    ('word_filter:menu', '屏蔽词菜单', 'domain', 'menu'),
    ('domain:user_manage', '域成员管理', 'domain', 'button'),
    ('domain:config', '域配置管理', 'domain', 'button'),
    ('domain:log_view', '域日志查看', 'domain', 'button'),
    ('domain:word_filter', '域屏蔽词管理', 'domain', 'button');

INSERT INTO domain_role (business_domain_id, code, name, preset)
SELECT bd.id, 'super_admin', '业务域超级管理员', 1
FROM business_domain bd
WHERE NOT EXISTS (
    SELECT 1
    FROM domain_role dr
    WHERE dr.business_domain_id = bd.id
      AND dr.code = 'super_admin'
);

INSERT INTO domain_role (business_domain_id, code, name, preset)
SELECT bd.id, 'domain_admin', '业务域管理员', 1
FROM business_domain bd
WHERE NOT EXISTS (
    SELECT 1
    FROM domain_role dr
    WHERE dr.business_domain_id = bd.id
      AND dr.code = 'domain_admin'
);

INSERT INTO domain_role (business_domain_id, code, name, preset)
SELECT bd.id, 'agent', '客服', 1
FROM business_domain bd
WHERE NOT EXISTS (
    SELECT 1
    FROM domain_role dr
    WHERE dr.business_domain_id = bd.id
      AND dr.code = 'agent'
);

INSERT IGNORE INTO domain_role_permission (domain_role_id, permission_item_id)
SELECT dr.id, pi.id
FROM domain_role dr
JOIN permission_item pi
  ON pi.code IN (
      'user_manage:menu',
      'domain_config:menu',
      'log:menu',
      'word_filter:menu',
      'domain:user_manage',
      'domain:config',
      'domain:log_view',
      'domain:word_filter'
  )
WHERE dr.code IN ('super_admin', 'domain_admin');
