-- 工单满意度评价表（S4-6 satisfaction-survey）
-- 仅工单 closed/resolved 后可由客户本人评价一次（ticket_id 唯一约束兜底防重）；
-- 无外键：遵循项目约束，归属/本人校验由业务逻辑完成。

CREATE TABLE IF NOT EXISTS ticket_satisfaction (
    id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    business_domain_id  BIGINT UNSIGNED NOT NULL COMMENT '业务域 id',
    ticket_id           BIGINT UNSIGNED NOT NULL COMMENT '工单 id（一单一评）',
    customer_id         BIGINT UNSIGNED NOT NULL COMMENT '评价客户账号 id',
    rating              TINYINT         NOT NULL COMMENT '评分 1-5 星',
    comment             TEXT            NULL COMMENT '评价文字',
    status              VARCHAR(16)     NOT NULL DEFAULT 'submitted' COMMENT '评价状态',
    created_at          DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at          DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_satisfaction_ticket (ticket_id),
    KEY idx_satisfaction_domain_customer (business_domain_id, customer_id)
) COMMENT='工单满意度评价';
