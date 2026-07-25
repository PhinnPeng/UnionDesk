-- 修复平台级系统属性标记
-- 将标题和描述标记为系统属性，确保与种子数据一致

UPDATE ticket_attribute 
SET is_system = 1,
    updated_at = CURRENT_TIMESTAMP(3)
WHERE scope = 'platform' 
  AND business_domain_id IS NULL
  AND name IN ('标题', '描述')
  AND is_system = 0;
