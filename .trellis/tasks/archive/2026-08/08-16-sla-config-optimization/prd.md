# SLA 超时动态配置优化（全局 SLA + 事项 SLA 双层 + 配置易用化 + 动作扩展与定时触发）

> 2026-08-16 立项。来源：用户两点诉求（grill-with-docs 会话），已完成现状勘察（@trellis-research 08-16）与两轮设计决策（第 2 轮已确认 D1/D2 默认、D3 动作扩展、D4 扫描纳入本轮）。

## Goal

1. **双层 SLA 架构**：新增「全局 SLA」（平台级跨域兜底）+「事项 SLA」（域内按事项类型/优先级，现状规则表）双层；两种规则同时命中时**以事项 SLA 为主**（域规则优先，全局仅兜底）。
2. **配置易用化**：SLA 配置从「手填 ID + 裸 JSON」改为普通域管理员可配置的表单——关联数据（事项类型/优先级/日历）下拉选择；超时动作可视化配置（JSON 仅作高级模式兜底）。
3. **超时动作扩展 + 定时触发**：违约动作集扩展为「按序升级优先级 / 置 SLA 状态 / 更换处理人 / 添加关注人」，并新增每分钟定时扫描，超时动作**按时自动执行**（不再依赖工单被操作才判定）。

## 现状（勘察核实）

- `sla_rule` 表 `business_domain_id` NOT NULL（含 FK `fk_sla_rule_domain`），**无平台级/全局行**；所有 SLA 接口挂 `/api/v1/admin/domains/{domainId}`；平台页 `/platform/sla-management` 仅是「选域代管」，无全局概念（feature-list F4.15「平台级保留」未落地为独立概念）
- 规则匹配 `selectPolicy` 已支持通配回退（`ticket_type_id IS NULL` / `priority_level_id IS NULL` 即通配，按精确度排序取一条），域内「默认规则」能力已具备，缺跨域全局层
- 前端表单：事项类型 ID/优先级 ID/日历 ID 全部手填 `InputNumber`（`pages/domain/sla/index.tsx`、`pages/platform/sla-management/index.tsx`）；违约动作裸 JSON 文本域；下拉接口 `fetchDomainTicketTypes` / `fetchDomainPriorityLevels`（shared 包）已就绪未复用
- 死字段：`calendar_id`（工作日历）与 `is_urgent_config`（紧急配置）全链路存在但未参与任何计算（deadline = `created_at + 分钟` 硬算自然时间）
- 无生产定时扫描：超时判定仅在工单事件触点（状态变更/领取/指派/回复）由 `refreshTicketSla` → `evaluateTicket` 惰性触发；「即将超时」语义不存在（E-EMP3 R3 计划接 UI 高亮，引擎需新建）
- 权限：`domain.sla.{read,create,update}`（domain_admin 全量、agent 只读）；平台侧复用同一套码代管域规则

## Requirements

- R1 **全局规则数据层**：`sla_rule.business_domain_id` 改可空（DROP FK），`business_domain_id IS NULL` 即全局规则（对齐 F4.13 全局屏蔽词「business_domain_id 为空」先例）；全局规则约束：`ticket_type_id`/`priority_level_id`/`calendar_id` 必须为空（平台无域级类型/优先级/日历）
- R2 **匹配链（事项优先）**：`loadPolicy` 增加全局兜底——域内规则（现状 selectPolicy：类型+优先级精确 > 仅类型 > 仅优先级 > 域默认）未命中时，查全局默认规则（`business_domain_id IS NULL`）；全局也未配 → 不设 SLA（现状行为，`null,null,null`）
- R3 **平台端全局规则配置**：`/platform/sla-management` 增加「全局默认规则」区（与「业务域代管」并列）；新端点 `GET/POST/PUT/DELETE /api/v1/admin/platform/sla-rules`；新权限码 `platform.sla.{read,create,update,delete}`（PermissionCodes 注册 + 菜单授权 platform_admin/super_admin）
- R4 **域端表单易用化**（`/domain/sla`）：事项类型、优先级改为 Select 下拉（可清空=通配，placeholder「全部类型/全部优先级」）；日历下拉保留但 disabled + 提示「暂不生效（预留）」；超时动作可视化（见 R7），JSON 编辑折叠为「高级模式」兜底
- R5 **平台代管域规则表单**同步域端易用化（选域后同一表单组件复用）
- R6 **违约动作扩展**（`breach_action_json` 开放字典扩展，旧键 `raise_priority_to`/`sla_status` 兼容）：
  - ① 升级优先级：`escalate_priority: true`，**按序升级到下一紧急阶段**（域优先级按 `sort_order ASC`，小=更紧急；normal→high→urgent；已是最高级不动；与旧绝对目标并存时绝对目标优先）
  - ② 更换处理人：`assign_to_staff_account_id`（超时强制指派，内部方法绕过版本乐观锁）
  - ③ 添加关注人：`add_watcher_staff_account_ids`（**追加**，先查后合并，不覆盖已有关注人）
  - 固定执行顺序：升级优先级 → 换处理人 → 加关注人，同事务
- R7 **SLA 状态默认状态机**（不再提供「置 SLA 状态」配置项，用户拍板）：
  - 超时（任一时限已过且未完成）→ `breached`（已超时）
  - 客服回复（首响）后违约条件消除 → `tracking`（计时中，evaluateTicket 自愈；解决时限仍超时则保持 `breached` 如实展示；客户回复不解除超时）
  - **唯一最终态**：取消 `resolved`，仅保留 `stopped`（展示「已结束」）——工单状态**流转到终态**（resolved/closed/withdrawn/merged 等终态码统一映射 `stopped`；E-EMP3 后可切换为按可配置工作流 `state_type='terminal'` 判定）即 SLA 达最终态；终态不被覆盖
- R8 **违约动作幂等**：新增 `ticket.sla_breach_actioned` 标志列，动作**每工单仅执行一次**——状态恢复 tracking 后再超时只翻转状态，不重复升级/换人/加关注人
- R9 **定时扫描**：新增 `SlaScanJob`（`@Scheduled` 每分钟，注入既有 `Clock`），候选=`sla_status='tracking'` 且任一时限已过未完成，分批（LIMIT 100）调 `evaluateTicket`；单条失败不中断整批
- R10 **取消工单关闭功能**（用户拍板）：移除详情页「关闭」按钮/关闭确认入口与队列「关闭」行操作/批量关闭入口；结束工单唯一路径 = **变更状态 → 流转到终态**；后端 `changeTicketStatus` 保留为状态控制唯一通道；`ticket.close` 权限码保留注册（清理随 E-EMP3）
- R11 现有域规则数据语义不变，无需迁移；`ticket.sla_status` 旧 `resolved` 值迁移为 `stopped`；既有 `is_urgent_config` 等死字段不动（标注遗留）

## Acceptance Criteria

- [ ] AC1 平台端可配置全局默认规则；新建域工单在无域规则命中时按全局规则设置首响/解决 deadline（跨域生效）
- [ ] AC2 域内任意规则命中时全局规则不生效（事项 SLA 为主）；匹配顺序：域×类型×优先级 > 域×类型 > 域默认 > 全局默认 > 不设 SLA
- [ ] AC3 域端/平台端规则编辑器不再出现手填 ID；类型/优先级为下拉且可留空；超时动作可视化，JSON 高级模式可读写同一 `breachAction`
- [ ] AC4 全局规则接口仅接受无类型/优先级/日历的规则（后端校验拒绝）；`platform.sla.*` 权限门控生效，域管理员不可见平台全局区
- [ ] AC5 违约动作按时自动执行：工单超时后（≤1 分钟扫描周期）自动升级优先级（按序）、按配置更换处理人/追加关注人；**动作每工单仅执行一次**（`sla_breach_actioned`）
- [ ] AC6 SLA 状态默认状态机：超时→已超时；客服回复（首响）后违约消除→计时中（解决时限仍超时保持已超时）；**唯一最终态**——流转到终态（resolved/closed/withdrawn/merged 等）→已结束（原「已解决」取消），终态不被覆盖；重复评估不重复动作
- [ ] AC7 工单关闭功能已移除：详情页无「关闭」按钮、队列无「关闭」行操作/批量关闭；结束工单仅可通过「变更状态」流转到终态，流转后 SLA 状态=已结束
- [ ] AC8 既有 SLA 测试（`SlaServiceTests`/`SlaRuleControllerTests`/`SlaRuleCrudIntegrationTest`）与工单测试（含 FR）不回归；新增：全局兜底匹配、事项优先、按序升级（含最高级边界）、动作执行、状态机/幂等、终态统一、扫描任务用例
- [ ] AC9 typecheck 通过；浏览器冒烟：域端配规则（含动作）、平台配全局、创建工单验 deadline、超时后验动作执行与状态流转、终态流转验 SLA 已结束

## Constraints

- 本轮**不纳入**：工作日历计算（`calendar_id` 仍不参与计算，表单 disabled「暂不生效」）、可配置工作流运行时接入（`changeTicketStatus` 改走 `TicketTypeFlowService`、DB 状态码统一——E-EMP3 已立项，本轮终态判定维持硬编码终态码集合并预留切换点）、「即将超时」阈值语义、通知模板联调、SLA 暂停/恢复、动作编排顺序自定义、`ticket.close` 权限码清理
- 定时扫描为单实例部署（无分布式锁，MVP）；扫描候选 SQL 以 `sla_status='tracking'` 为幂等边界
- 权限码命名遵守连字符合规（`platform.sla.*`）；迁移以 code/权限码为幂等键；临时脚本仅放 `agent-work/`
- 后端规范：数据库无外键（遗留 FK 迁移时一并去除）；新增模块改动前查后端结构给适配方案
