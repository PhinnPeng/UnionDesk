-- SLA 配置重构（ADR-005）：域内单份通配配置取代双层规则列表
-- 1) 新表 sla_config（每业务域一行）；2) 域菜单更名「SLA 配置」+ 补图标；3) 平台 SLA 菜单隐藏（域内自配置，平台不管理）

-- A) sla_config 单行配置表
CREATE TABLE IF NOT EXISTS `sla_config` (
  `id`                            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `business_domain_id`            BIGINT UNSIGNED NOT NULL COMMENT '业务域 id（每域一行）',
  `first_response_minutes`        INT UNSIGNED NULL COMMENT '首次响应时限（分钟，空=不启用）',
  `resolution_minutes`            INT UNSIGNED NULL COMMENT '解决响应时限（分钟，空=不启用）',
  `breach_action_json`            JSON NULL COMMENT '超时动作：{escalate_priority:{enabled,to_priority_level_id}, assign_to_staff_account_id, add_watcher_staff_account_ids[]}',
  `calendar_json`                 JSON NULL COMMENT '工作日历：{working_days:[1..7], weekend_work:false, holidays:["2026-10-01"]}',
  `urgent_first_response_minutes` INT UNSIGNED NULL COMMENT '预留：紧急配置首响（后置）',
  `urgent_resolution_minutes`     INT UNSIGNED NULL COMMENT '预留：紧急配置解决（后置）',
  `created_at`                    DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at`                    DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sla_config_domain` (`business_domain_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='域 SLA 配置（单行，取代 sla_rule 规则列表）';

-- B) 域菜单更名「SLA 配置」+ 补图标（FieldTimeOutlined 需在前端 menu-icons 注册）
UPDATE `iam_admin_menu`
SET `name` = 'SLA 配置', `icon` = 'FieldTimeOutlined'
WHERE `code` = 'BUSINESS-DOMAIN-SLA';

-- C) 平台 SLA 菜单隐藏（页面/接口/权限码保留不删，域内自配置后平台不再管理）
UPDATE `iam_admin_menu`
SET `hidden` = 1
WHERE `code` = 'ADM0000000053' OR `route_path` = '/platform/sla-management';
