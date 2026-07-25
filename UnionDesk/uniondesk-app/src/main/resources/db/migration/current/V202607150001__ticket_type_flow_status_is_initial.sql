-- 工作流状态显式唯一初始态：ticket_type_flow_status.is_initial

ALTER TABLE ticket_type_flow_status
    ADD COLUMN is_initial TINYINT NOT NULL DEFAULT 0 COMMENT '1=该类型工作流唯一初始状态' AFTER is_resolved;

-- 历史数据回填：每个类型择一初始 —— 优先无入边，再按 sort_order、id
UPDATE ticket_type_flow_status fs
INNER JOIN (
    SELECT
        s.domain_id,
        s.ticket_type_id,
        SUBSTRING_INDEX(
            GROUP_CONCAT(
                s.state_code
                ORDER BY
                    CASE WHEN COALESCE(inbound.cnt, 0) = 0 THEN 0 ELSE 1 END ASC,
                    s.sort_order ASC,
                    s.id ASC
                SEPARATOR ','
            ),
            ',',
            1
        ) AS initial_code
    FROM ticket_type_flow_status s
    LEFT JOIN (
        SELECT domain_id, ticket_type_id, to_state_code, COUNT(*) AS cnt
        FROM ticket_type_flow_transition
        GROUP BY domain_id, ticket_type_id, to_state_code
    ) inbound
      ON inbound.domain_id = s.domain_id
     AND inbound.ticket_type_id = s.ticket_type_id
     AND inbound.to_state_code = s.state_code
    GROUP BY s.domain_id, s.ticket_type_id
) pick
  ON pick.domain_id = fs.domain_id
 AND pick.ticket_type_id = fs.ticket_type_id
 AND pick.initial_code = fs.state_code
SET fs.is_initial = 1;
