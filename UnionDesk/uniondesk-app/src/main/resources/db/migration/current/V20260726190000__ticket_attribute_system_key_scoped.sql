-- system_key 应为「作用域内唯一」，而非全表唯一。
-- 否则域内复制平台系统属性（title/description/...）会与平台行冲突，导致无法从平台添加事项类型。

ALTER TABLE ticket_attribute
    DROP INDEX uk_ticket_attribute_system_key;

-- MySQL UNIQUE 允许多个 NULL system_key；非空时在同一 scope+domain 内唯一
CREATE UNIQUE INDEX uk_ticket_attribute_scope_domain_system_key
    ON ticket_attribute (scope, scope_domain_key, system_key);
