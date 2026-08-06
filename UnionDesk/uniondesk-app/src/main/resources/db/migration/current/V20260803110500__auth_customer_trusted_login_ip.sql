-- 客户账号级常用登录 IP（无外键；业务层保证 user_id 指向 customer_account）
CREATE TABLE IF NOT EXISTS `auth_customer_trusted_login_ip` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '客户账号 id',
  `client_ip` VARCHAR(64) NOT NULL COMMENT '规范化 IP 文本',
  `last_used_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_auth_customer_trusted_login_ip_user_ip` (`user_id`, `client_ip`),
  KEY `idx_auth_customer_trusted_login_ip_user_used` (`user_id`, `last_used_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='客户常用登录 IP';
