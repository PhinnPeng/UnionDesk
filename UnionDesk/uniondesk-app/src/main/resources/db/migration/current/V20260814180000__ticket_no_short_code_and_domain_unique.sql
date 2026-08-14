-- 工单编号规则改造（后端部分）：
-- 1) ticket_type 新增 short_code（事项类型短码，用于工单编号前缀，如 FE-20260814-0001）
-- 2) 存量 ticket_type 回填默认短码：UPPER(LEFT(code,2))，同域冲突时追加数字后缀
-- 3) 新编号格式无域信息，跨域会撞原全局唯一索引，改为 (business_domain_id, xxx_no) 复合唯一

-- A) ticket_type 新增 short_code 列（域内建议唯一，业务层创建/更新时校验并默认生成）
ALTER TABLE ticket_type
    ADD COLUMN short_code VARCHAR(16) NULL COMMENT '事项类型短码（用于工单编号前缀，域内建议唯一）' AFTER code;

-- B) 存量回填：默认 UPPER(LEFT(code,2))（如 feedback -> FE）
--    同一域（business_domain_id，平台行按 0 归组）内前 2 位重复时追加定宽数字后缀，
--    定宽 4 位保证后缀结果（长度 >= 6）不可能与任何 2 位以内的既有短码碰撞，单次 UPDATE 即保证域内不重复。
UPDATE ticket_type tt
JOIN (
    SELECT
        id,
        CASE
            WHEN code IS NULL OR code = '' THEN 'TK'
            ELSE UPPER(LEFT(code, 2))
        END AS base_short,
        ROW_NUMBER() OVER (
            PARTITION BY COALESCE(business_domain_id, 0),
            CASE
                WHEN code IS NULL OR code = '' THEN 'TK'
                ELSE UPPER(LEFT(code, 2))
            END
            ORDER BY id
        ) AS rn
    FROM ticket_type
) d ON d.id = tt.id
SET tt.short_code = CONCAT(d.base_short, IF(d.rn > 1, LPAD(d.rn - 1, 4, '0'), ''))
WHERE tt.short_code IS NULL OR tt.short_code = '';

-- C) ticket：新格式 {短码}-{yyyyMMdd}-{4位序号} 不含域信息，跨域可能同号，
--    唯一约束从全局 ticket_no 改为 (business_domain_id, ticket_no) 复合唯一
ALTER TABLE ticket
    DROP INDEX uk_ticket_no,
    ADD UNIQUE INDEX uk_ticket_domain_no (business_domain_id, ticket_no);

-- D) consultation_session：会话号改为 CS-{yyyyMMdd}-{4位序号}，同样按域唯一
ALTER TABLE consultation_session
    DROP INDEX uk_consultation_session_no,
    ADD UNIQUE INDEX uk_consultation_domain_session_no (business_domain_id, session_no);
