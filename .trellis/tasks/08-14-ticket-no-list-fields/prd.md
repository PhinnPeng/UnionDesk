# 工单编号规则改造 + 工单列表字段调整

> 2026-08-14 立项，来源：@trellis-research 架构五问（用户确认「按本轮执行」；聚合工作台待后续实现）。

## Goal

1. **工单编号**改为「事项类型短码 + 时序 + 固定顺序码」（如 `FB-20260814-0001`），**咨询会话编号统一同规则**（`CS-20260814-0001`），并修复编号生成**并发竞态**（MAX+1 + 唯一索引 + 冲突重试）。
2. **工单列表字段调整**：受理人显示姓名（非「员工 #id」）、创建时间列改为更新时间、新增客户名称列。

## 现状（勘察核实）

- 工单号：`{domainCode}-{yyyyMMdd}-{序号}`（如 `domain-udkl8l-20260814-1`）——域 code 为随机串、24+ 字符、不可口述；`TicketService.nextTicketNo()`（TicketService.java:618-622）+ `findNextTicketSequence`（TicketMapper.xml:328-332）为 MAX+1 且**无并发重试**（唯一索引 uk_ticket_no 冲突即 500）
- 咨询会话号：`CS + yyyyMMdd*10000 + seq`（ConsultationService.java:294-298），`nextSessionSequence`（ConsultationMapper.xml:98-102）**全局跨域取 MAX**，与工单号按域口径分裂
- 工单列表 8 列（ticket-queue/index.tsx:262-339）：编号/标题/类型/状态/优先级/SLA/受理人(渲染「员工 #id」)/创建时间；无客户名称列；排序键已是 updated_at
- ticket_type 表（域级）code 如 `feedback`，无短码字段
- 存量工单号、咨询号保留不动（新旧格式并存，格式天然可区分）

## Requirements

- R1 编号格式：`{类型短码}-{yyyyMMdd}-{4位定长序号}`；类型短码来自 ticket_type 新增 `short_code` 列（域级可配，默认取 code 前 2 位大写，如 feedback→FE，可改；排序/sort_order 不变）
- R2 咨询会话号：`CS-{yyyyMMdd}-{4位定长序号}`，**与工单一致按域递增**（域内每日期独立序号）
- R3 并发安全：工单/咨询编号生成捕获唯一键冲突自动重试（≥3 次），避免并发建单 500
- R4 新旧编号并存：存量号不回填改写；新号按「短码-日期」前缀独立取序号（与旧格式隔离，不冲突）
- R5 列表字段：受理人列显示姓名（staff 姓名）、新增客户名称列（customer 姓名）、创建时间列改为更新时间（或两列并存、创建降级详情——按实现取舍，默认替换）
- R6 姓名数据源：查证 staff_account/customer_account（user_account 已清理）实际姓名字段并联查；查不到的兜底显示「员工 #id」/「客户 #id」
- R7 前端 ticket-config 事项类型配置支持编辑 `short_code`（域级配置入口）

## Acceptance Criteria

- [ ] AC1 新建工单编号为 `{短码}-{yyyyMMdd}-{4位序号}`；咨询会话号为 `CS-{yyyyMMdd}-{4位序号}` 且按域递增
- [ ] AC2 并发建单（同域同日）不 500（重试生效，可单测验证）
- [ ] AC3 存量编号不受影响（旧号查询/搜索正常）
- [ ] AC4 列表页：受理人姓名、客户名称、更新时间列正确显示；详情页不受影响
- [ ] AC5 ticket_type 短码可配置（默认值生成 + 配置页可编辑）
- [ ] AC6 后端单测/编译通过；前端 typecheck 通过；浏览器冒烟确认列表渲染

## Constraints

- 不改编号唯一性约束/表结构主体（仅新增 short_code 列，Flyway 迁移）
- 遵循既有模块结构（uniondesk-ticket）；不夹带其他需求（P0-4 snowflake id、description 详情展示等另行立项）
- 临时验证脚本仅放 agent-work/
