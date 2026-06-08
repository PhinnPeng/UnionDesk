CREATE TABLE IF NOT EXISTS user_organization (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    organization_id BIGINT UNSIGNED NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_organization (user_id, organization_id),
    CONSTRAINT fk_user_org_user FOREIGN KEY (user_id) REFERENCES user_account(id),
    CONSTRAINT fk_user_org_org FOREIGN KEY (organization_id) REFERENCES platform_organization(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户-部门关系表';
