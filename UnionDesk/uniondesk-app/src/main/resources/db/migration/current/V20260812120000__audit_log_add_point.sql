-- 审计日志补充操作入口端点标记（P0-② 审计补齐）
-- platform=平台控制台 / domain=业务域控制台
ALTER TABLE `audit_log` ADD COLUMN `point` varchar(16) DEFAULT NULL COMMENT '操作入口端点（platform=平台控制台/domain=业务域控制台）' AFTER `operator_actor_type`;
