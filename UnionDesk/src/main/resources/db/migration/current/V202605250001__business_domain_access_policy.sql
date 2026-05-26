-- 业务域入域策略：registration_enabled / invitation_enabled（allowed|disallowed）
-- 自 registration_policy 回填后删除旧列。

ALTER TABLE business_domain
    ADD COLUMN registration_enabled VARCHAR(16) NOT NULL DEFAULT 'allowed' COMMENT 'allowed|disallowed' AFTER registration_policy;

ALTER TABLE business_domain
    ADD COLUMN invitation_enabled VARCHAR(16) NOT NULL DEFAULT 'allowed' COMMENT 'allowed|disallowed' AFTER registration_enabled;

UPDATE business_domain
SET registration_enabled = CASE LOWER(registration_policy)
        WHEN 'invitation_only' THEN 'disallowed'
        WHEN 'admin_only' THEN 'disallowed'
        ELSE 'allowed'
    END,
    invitation_enabled = CASE LOWER(registration_policy)
        WHEN 'admin_only' THEN 'disallowed'
        ELSE 'allowed'
    END;

ALTER TABLE business_domain
    DROP COLUMN registration_policy;
