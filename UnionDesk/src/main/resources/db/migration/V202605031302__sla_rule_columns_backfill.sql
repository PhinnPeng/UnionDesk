SET @exist := (SELECT COUNT(*)
               FROM information_schema.COLUMNS
               WHERE TABLE_SCHEMA = DATABASE()
                 AND TABLE_NAME = 'sla_rule'
                 AND COLUMN_NAME = 'name');
SET @sql := IF(@exist = 0,
               'ALTER TABLE sla_rule ADD COLUMN name VARCHAR(128) NOT NULL DEFAULT '''' AFTER business_domain_id',
               'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @exist := (SELECT COUNT(*)
               FROM information_schema.COLUMNS
               WHERE TABLE_SCHEMA = DATABASE()
                 AND TABLE_NAME = 'sla_rule'
                 AND COLUMN_NAME = 'priority_level_id');
SET @sql := IF(@exist = 0,
               'ALTER TABLE sla_rule ADD COLUMN priority_level_id BIGINT UNSIGNED DEFAULT NULL AFTER ticket_type_id',
               'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @exist := (SELECT COUNT(*)
               FROM information_schema.TABLE_CONSTRAINTS
               WHERE TABLE_SCHEMA = DATABASE()
                 AND TABLE_NAME = 'sla_rule'
                 AND CONSTRAINT_NAME = 'fk_sla_rule_priority_level');
SET @sql := IF(@exist = 0,
               'ALTER TABLE sla_rule ADD CONSTRAINT fk_sla_rule_priority_level FOREIGN KEY (priority_level_id) REFERENCES ticket_priority_level (id)',
               'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
