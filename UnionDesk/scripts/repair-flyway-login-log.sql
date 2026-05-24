-- 修复 V202605230001 失败后 Flyway 无法启动、登录 500 的问题。
-- 在 MySQL 客户端执行（库：uniondesk），然后重启后端。

-- 1) 清除失败记录，允许后续迁移继续
DELETE FROM flyway_schema_history
WHERE version = '202605230001'
  AND success = 0;

-- 2) 若 login_log 仍缺列，可手工执行（与 V202605230002 一致，列已存在则跳过报错可忽略）
-- ALTER TABLE login_log ADD COLUMN client_code VARCHAR(64) NULL AFTER portal_type;
-- ...
