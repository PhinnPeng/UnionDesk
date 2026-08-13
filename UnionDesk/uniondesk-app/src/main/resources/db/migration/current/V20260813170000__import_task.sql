-- S6 任务 10：用户导入导出（异步 Excel 导入任务表）
-- 无外键，仅业务逻辑约束

CREATE TABLE import_task (
    id                 BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    business_domain_id BIGINT       NULL COMMENT '业务域ID（平台级为NULL）',
    task_type          VARCHAR(32)  NOT NULL COMMENT '任务类型：staff_import',
    file_key           VARCHAR(255) NOT NULL COMMENT '文件存储键',
    file_name          VARCHAR(255) NOT NULL COMMENT '原始文件名',
    status             VARCHAR(16)  NOT NULL DEFAULT 'pending' COMMENT '状态：pending/processing/success/failed',
    total_count        INT          NOT NULL DEFAULT 0 COMMENT '总行数',
    success_count      INT          NOT NULL DEFAULT 0 COMMENT '成功行数',
    fail_count         INT          NOT NULL DEFAULT 0 COMMENT '失败行数',
    error_summary      TEXT         NULL COMMENT '失败明细（JSON数组：[{"row":3,"message":"登录账号或手机号已存在"}]）',
    created_by         BIGINT       NULL COMMENT '创建人（staff_id）',
    created_at         DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    finished_at        DATETIME(3)  NULL COMMENT '完成时间',
    PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='导入任务表';
