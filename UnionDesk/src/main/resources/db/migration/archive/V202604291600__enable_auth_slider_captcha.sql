INSERT INTO auth_login_config (config_key, config_value)
VALUES
    ('captcha_enabled', 'true'),
    ('captcha_hint', '请按住滑块，拖动到最右边')
ON DUPLICATE KEY UPDATE
    config_value = VALUES(config_value),
    updated_at = CURRENT_TIMESTAMP(3);
