CREATE TABLE IF NOT EXISTS platform_organization (
    id bigint unsigned NOT NULL AUTO_INCREMENT,
    code varchar(64) NOT NULL,
    name varchar(128) NOT NULL,
    parent_id bigint unsigned DEFAULT NULL,
    leader_user_id bigint unsigned DEFAULT NULL,
    order_no int NOT NULL DEFAULT 0,
    status tinyint NOT NULL DEFAULT 1,
    remark varchar(255) DEFAULT NULL,
    created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_platform_organization_code (code),
    KEY idx_platform_organization_parent_order (parent_id, order_no, id),
    KEY idx_platform_organization_status (status),
    CONSTRAINT fk_platform_organization_parent FOREIGN KEY (parent_id) REFERENCES platform_organization(id),
    CONSTRAINT fk_platform_organization_leader FOREIGN KEY (leader_user_id) REFERENCES user_account(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='平台内部组织树';

INSERT INTO platform_organization (code, name, parent_id, leader_user_id, order_no, status, remark)
VALUES ('platform-root', '平台组织', NULL, 1, 10, 1, '平台组织根节点')
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    parent_id = VALUES(parent_id),
    leader_user_id = VALUES(leader_user_id),
    order_no = VALUES(order_no),
    status = VALUES(status),
    remark = VALUES(remark);

INSERT INTO platform_organization (code, name, parent_id, leader_user_id, order_no, status, remark)
SELECT 'platform-ops', '平台运营部', parent.id, 1, 20, 1, '负责平台账号与角色治理'
FROM platform_organization parent
WHERE parent.code = 'platform-root'
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    parent_id = VALUES(parent_id),
    leader_user_id = VALUES(leader_user_id),
    order_no = VALUES(order_no),
    status = VALUES(status),
    remark = VALUES(remark);

INSERT INTO platform_organization (code, name, parent_id, leader_user_id, order_no, status, remark)
SELECT 'security-audit', '安全审计组', parent.id, 1, 30, 1, '负责平台审计日志与安全策略核查'
FROM platform_organization parent
WHERE parent.code = 'platform-root'
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    parent_id = VALUES(parent_id),
    leader_user_id = VALUES(leader_user_id),
    order_no = VALUES(order_no),
    status = VALUES(status),
    remark = VALUES(remark);
