-- MySQL 初始化配置脚本
-- 用于设置 Flyway 迁移所需的全局参数
-- 
-- 使用方法（需要 root 或具有 SUPER 权限的用户执行）：
-- mysql -u root -p < init-mysql-config.sql
--
-- 或者在 MySQL 命令行中执行：
-- mysql> SOURCE /path/to/init-mysql-config.sql;

-- 允许创建函数和触发器（Flyway 迁移脚本中需要）
SET GLOBAL log_bin_trust_function_creators = 1;

-- 验证设置
SELECT @@GLOBAL.log_bin_trust_function_creators AS log_bin_trust_function_creators;
