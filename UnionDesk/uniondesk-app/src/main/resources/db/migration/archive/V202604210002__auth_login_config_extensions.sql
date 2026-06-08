INSERT INTO auth_login_config (config_key, config_value)
VALUES
    ('captcha_enabled', 'false'),
    ('wechat_login_enabled', 'false'),
    ('captcha_hint', ''),
    ('wechat_hint', '')
ON DUPLICATE KEY UPDATE
    config_value = VALUES(config_value),
    updated_at = CURRENT_TIMESTAMP(3);
