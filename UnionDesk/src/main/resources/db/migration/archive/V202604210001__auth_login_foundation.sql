ALTER TABLE user_account
    ADD COLUMN username VARCHAR(64) DEFAULT NULL AFTER id;

UPDATE user_account
SET username = CONCAT('user_', id)
WHERE username IS NULL;

UPDATE user_account
SET username = 'customer',
    password_hash = '{noop}customer123',
    status = 1
WHERE id = 1;

UPDATE user_account
SET username = 'admin',
    password_hash = '{noop}admin123',
    status = 1
WHERE id = 2;

ALTER TABLE user_account
    MODIFY username VARCHAR(64) NOT NULL;

ALTER TABLE user_account
    ADD UNIQUE KEY uk_user_username (username);

CREATE TABLE IF NOT EXISTS auth_login_config (
    config_key VARCHAR(64) NOT NULL,
    config_value VARCHAR(255) NOT NULL,
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO auth_login_config (config_key, config_value)
VALUES
    ('password_login_enabled', 'true'),
    ('username_login_enabled', 'true'),
    ('email_login_enabled', 'true'),
    ('mobile_login_enabled', 'true'),
    ('session_ttl_seconds', '604800'),
    ('max_active_sessions_per_user', '10')
ON DUPLICATE KEY UPDATE
    config_value = VALUES(config_value),
    updated_at = CURRENT_TIMESTAMP(3);

CREATE TABLE IF NOT EXISTS auth_login_session (
    sid CHAR(36) NOT NULL,
    user_id BIGINT UNSIGNED NOT NULL,
    role_code VARCHAR(32) NOT NULL,
    business_domain_id BIGINT UNSIGNED DEFAULT NULL,
    login_identifier_masked VARCHAR(128) NOT NULL,
    session_status VARCHAR(16) NOT NULL DEFAULT 'active',
    issued_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    expires_at DATETIME(3) NOT NULL,
    last_seen_at DATETIME(3) DEFAULT NULL,
    revoked_at DATETIME(3) DEFAULT NULL,
    revoked_reason VARCHAR(255) DEFAULT NULL,
    refresh_token_hash CHAR(64) DEFAULT NULL,
    client_ip VARCHAR(64) DEFAULT NULL,
    user_agent VARCHAR(255) DEFAULT NULL,
    PRIMARY KEY (sid),
    KEY idx_auth_login_session_user_status (user_id, session_status, expires_at),
    KEY idx_auth_login_session_status_expires (session_status, expires_at),
    KEY idx_auth_login_session_last_seen (last_seen_at),
    CONSTRAINT fk_auth_login_session_user FOREIGN KEY (user_id) REFERENCES user_account(id),
    CONSTRAINT fk_auth_login_session_domain FOREIGN KEY (business_domain_id) REFERENCES business_domain(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS auth_login_log (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    sid CHAR(36) DEFAULT NULL,
    user_id BIGINT UNSIGNED DEFAULT NULL,
    username VARCHAR(64) DEFAULT NULL,
    login_identifier_masked VARCHAR(128) NOT NULL,
    login_identifier_type VARCHAR(16) NOT NULL,
    event_type VARCHAR(32) NOT NULL,
    result VARCHAR(16) NOT NULL,
    reason VARCHAR(255) DEFAULT NULL,
    client_ip VARCHAR(64) DEFAULT NULL,
    user_agent VARCHAR(255) DEFAULT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_auth_login_log_sid_created (sid, created_at),
    KEY idx_auth_login_log_user_created (user_id, created_at),
    KEY idx_auth_login_log_event_created (event_type, created_at),
    CONSTRAINT fk_auth_login_log_session FOREIGN KEY (sid) REFERENCES auth_login_session(sid),
    CONSTRAINT fk_auth_login_log_user FOREIGN KEY (user_id) REFERENCES user_account(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO user_domain_role (user_id, role_id, business_domain_id)
SELECT 1, r.id, d.id
FROM role r
JOIN business_domain d ON d.code = 'default'
WHERE r.code = 'customer'
ON DUPLICATE KEY UPDATE
    user_id = VALUES(user_id),
    role_id = VALUES(role_id),
    business_domain_id = VALUES(business_domain_id);
