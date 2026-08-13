-- 审计日志 detail 列类型修正（P0-② 审计补齐）
-- detail 读写均为普通文本（写入侧 AuditDetailTextBuilder 生成中文文本，读取侧 CAST AS CHAR），
-- json 类型导致写入非 JSON 文本时被 MySQL 拒绝（Data truncation: Invalid JSON text），
-- 所有审计写入静默失败。改为 text 与既有读写语义一致。
ALTER TABLE `audit_log` MODIFY COLUMN `detail` text DEFAULT NULL COMMENT '审计详情文本';
