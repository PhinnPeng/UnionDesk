-- step-up 会话类型放行（P0-③ step-up 真实校验）
-- 原 CHECK 约束仅允许 login/password_reset，LoginSessionService 的 step_up 会话（createStepUpToken）
-- 无法落库，导致签发的 step-up token 无法被 validateStepUpToken 校验。
ALTER TABLE `auth_login_session` DROP CHECK `chk_auth_login_session_session_type`;
ALTER TABLE `auth_login_session` ADD CONSTRAINT `chk_auth_login_session_session_type`
    CHECK ((`session_type` in (_utf8mb4'login',_utf8mb4'step_up',_utf8mb4'password_reset')));
