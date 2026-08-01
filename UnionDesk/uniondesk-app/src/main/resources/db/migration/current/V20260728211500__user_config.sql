-- 用户级配置（KV），无外键；引用关系由业务校验
CREATE TABLE IF NOT EXISTS user_config (
  id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id      BIGINT UNSIGNED NOT NULL,
  config_key   VARCHAR(128) NOT NULL,
  config_value TEXT,
  value_type   VARCHAR(16) NOT NULL DEFAULT 'string',
  description  VARCHAR(255) DEFAULT NULL,
  updated_at   DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
               ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_config (user_id, config_key),
  KEY idx_user_config_user (user_id)
) COMMENT='用户级配置（KV）';
