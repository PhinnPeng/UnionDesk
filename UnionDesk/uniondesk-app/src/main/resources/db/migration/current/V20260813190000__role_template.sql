-- P1-1 集团统一角色模板（任务 08-11-group-role-management，design §3/§4/§5）
-- 1) 三张新表（无外键，引用关系由业务逻辑保证）
-- 2) domain_role 增列 template_id / template_version / locked_fields（可空，非模板角色行为不变）
-- 3) 平台权限码 platform.role_template.* 注册并授权 super_admin

-- A) role_template
CREATE TABLE IF NOT EXISTS `role_template` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `code` VARCHAR(64) NOT NULL COMMENT '模板编码（作为域实例角色编码，域内唯一）',
  `name` VARCHAR(128) NOT NULL COMMENT '模板名称',
  `description` VARCHAR(255) DEFAULT NULL COMMENT '模板描述',
  `sync_strategy` VARCHAR(16) NOT NULL DEFAULT 'immediate' COMMENT '同步策略：immediate/manual/none',
  `locked_fields` JSON NOT NULL COMMENT '锁定字段列表（默认 ["permissions"]，域端不可修改）',
  `preset` TINYINT NOT NULL DEFAULT 0 COMMENT '是否内置模板',
  `version` INT NOT NULL DEFAULT 1 COMMENT '模板版本（下发实例记录，用于漂移对比）',
  `created_by` BIGINT UNSIGNED DEFAULT NULL COMMENT '创建人（员工账号 id）',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_template_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色模板（集团统一角色）';

-- B) role_template_permission（复用 permission_item 权限目录，与 domain_role_permission 同构）
CREATE TABLE IF NOT EXISTS `role_template_permission` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `template_id` BIGINT UNSIGNED NOT NULL,
  `permission_item_id` BIGINT UNSIGNED NOT NULL,
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_template_permission` (`template_id`, `permission_item_id`),
  KEY `idx_role_template_permission_item` (`permission_item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色模板权限项';

-- C) role_template_domain（模板下发域记录）
CREATE TABLE IF NOT EXISTS `role_template_domain` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `template_id` BIGINT UNSIGNED NOT NULL,
  `business_domain_id` BIGINT UNSIGNED NOT NULL,
  `instance_domain_role_id` BIGINT UNSIGNED NOT NULL COMMENT '该域生成的 domain_role 实例 id',
  `sync_mode` VARCHAR(16) NOT NULL DEFAULT 'immediate' COMMENT '下发时的同步模式',
  `applied_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_template_domain` (`template_id`, `business_domain_id`),
  KEY `idx_role_template_domain_domain` (`business_domain_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色模板下发域';

-- D) domain_role 增列（可空，非模板角色三列均为空，行为不变）
ALTER TABLE `domain_role`
  ADD COLUMN `template_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '来源角色模板 id（非模板角色为空）',
  ADD COLUMN `template_version` INT DEFAULT NULL COMMENT '下发时的模板版本（漂移对比）',
  ADD COLUMN `locked_fields` JSON DEFAULT NULL COMMENT '实例锁定字段列表（继承模板，非模板角色为空）';

-- E) 平台权限码注册（platform.role_template.{read,create,update,delete,apply,sync}）
INSERT INTO iam_permission (code, name, description, permission_scope, resource_code, action_code, http_method, path_pattern, status)
SELECT v.code, v.name, v.name, 'platform', v.code, v.code, NULL, NULL, 1
FROM (
    SELECT 'platform.role_template.read' AS code, '角色模板-查看' AS name
    UNION ALL SELECT 'platform.role_template.create', '角色模板-创建'
    UNION ALL SELECT 'platform.role_template.update', '角色模板-更新'
    UNION ALL SELECT 'platform.role_template.delete', '角色模板-删除'
    UNION ALL SELECT 'platform.role_template.apply', '角色模板-下发/解绑/绑定成员'
    UNION ALL SELECT 'platform.role_template.sync', '角色模板-同步'
) v
WHERE NOT EXISTS (
    SELECT 1 FROM iam_permission p WHERE p.code = v.code
);

INSERT INTO iam_role_permission (role_id, permission_id, created_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP(3)
FROM role r
JOIN iam_permission p ON p.code IN (
    'platform.role_template.read',
    'platform.role_template.create',
    'platform.role_template.update',
    'platform.role_template.delete',
    'platform.role_template.apply',
    'platform.role_template.sync'
)
WHERE r.code = 'super_admin'
  AND NOT EXISTS (
      SELECT 1 FROM iam_role_permission rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
