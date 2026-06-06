-- identity-backend-prd-alignment：身份目标态对齐（DROP FK + 列重命名/新增 + 回填）

-- 1) DROP 身份核心表外键
ALTER TABLE identity_subject DROP FOREIGN KEY fk_identity_subject_merged;
ALTER TABLE staff_account DROP FOREIGN KEY fk_staff_account_subject;
ALTER TABLE customer_account DROP FOREIGN KEY fk_customer_account_subject;
ALTER TABLE domain_member DROP FOREIGN KEY fk_domain_member_staff;
ALTER TABLE domain_member DROP FOREIGN KEY fk_domain_member_domain;
ALTER TABLE domain_member_role DROP FOREIGN KEY fk_dmr_member;
ALTER TABLE domain_member_role DROP FOREIGN KEY fk_dmr_role;
ALTER TABLE domain_customer DROP FOREIGN KEY fk_domain_customer_account;
ALTER TABLE domain_customer DROP FOREIGN KEY fk_domain_customer_domain;
ALTER TABLE staff_account_platform_role DROP FOREIGN KEY fk_sapr_staff;
ALTER TABLE staff_account_platform_role DROP FOREIGN KEY fk_sapr_role;

-- 2) 登录账号 / 昵称列对齐
ALTER TABLE staff_account
    CHANGE COLUMN login_name username VARCHAR(64) NOT NULL COMMENT '登录账号',
    ADD COLUMN real_name VARCHAR(128) NULL COMMENT '真实姓名' AFTER username,
    ADD COLUMN nickname VARCHAR(128) NULL COMMENT '昵称' AFTER real_name,
    ADD COLUMN avatar_url VARCHAR(512) NULL COMMENT '头像' AFTER nickname;

ALTER TABLE staff_account
    RENAME INDEX uk_staff_account_login TO uk_staff_account_username;

ALTER TABLE customer_account
    CHANGE COLUMN login_name username VARCHAR(64) NOT NULL COMMENT '登录账号',
    CHANGE COLUMN display_name nickname VARCHAR(128) NULL COMMENT '昵称';

ALTER TABLE customer_account
    RENAME INDEX uk_customer_account_login TO uk_customer_account_username;

ALTER TABLE domain_member
    ADD COLUMN domain_nickname VARCHAR(128) NULL COMMENT '域内昵称' AFTER source,
    ADD COLUMN domain_avatar_url VARCHAR(512) NULL COMMENT '域内头像' AFTER domain_nickname,
    ADD COLUMN domain_contact_phone VARCHAR(32) NULL COMMENT '域内对外电话' AFTER domain_avatar_url,
    ADD COLUMN domain_contact_email VARCHAR(128) NULL COMMENT '域内对外邮箱' AFTER domain_contact_phone;

-- 3) 回填员工真实姓名（legacy user_account.nickname 在 UI 为「姓名」）
UPDATE staff_account sa
INNER JOIN user_account ua ON ua.id = sa.id
SET sa.real_name = ua.nickname
WHERE sa.real_name IS NULL
  AND ua.nickname IS NOT NULL
  AND TRIM(ua.nickname) <> '';

-- 4) user_domain_role → domain_member_role 补录
INSERT INTO domain_member_role (domain_member_id, domain_role_id, created_at)
SELECT dm.id, dr.id, udr.created_at
FROM user_domain_role udr
INNER JOIN domain_member dm
    ON dm.staff_account_id = udr.user_id
   AND dm.business_domain_id = udr.business_domain_id
INNER JOIN role r ON r.id = udr.role_id
INNER JOIN domain_role dr
    ON dr.business_domain_id = udr.business_domain_id
   AND dr.code = r.code
WHERE NOT EXISTS (
    SELECT 1
    FROM domain_member_role existing
    WHERE existing.domain_member_id = dm.id
      AND existing.domain_role_id = dr.id
);
