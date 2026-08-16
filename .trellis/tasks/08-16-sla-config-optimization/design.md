# SLA 配置优化技术设计（全局 SLA + 事项 SLA 双层）

> 任务：`08-16-sla-config-optimization`。配套文档：`prd.md`（需求/验收）、`docs/architecture/adr-sla-two-tier-config.md`（决策记录）。
> 决策已确认：D1/D2 默认；D3 动作集（按序升级/更换处理人/添加关注人）；D4 定时扫描纳入本轮；D5 SLA 状态默认状态机（**唯一最终态，取消 resolved**）；D10 取消工单关闭功能、终态判定（用户拍板，第 3/4 轮）。

---

## 1. 目标

把 SLA 从「专家向配置」（手填 ID + 裸 JSON + 无全局概念）收敛为「普通域管理员可配置 + 平台统一下发兜底 + 超时动作按时自动执行 + SLA 状态自动管理」：

```
工单创建 / 定时扫描 / 工单事件
  └─ loadPolicy(域, 类型, 优先级)
       ├─ 1 域规则：类型+优先级 精确      ← 事项 SLA（优先）
       ├─ 2 域规则：仅类型（通配优先级）
       ├─ 3 域规则：仅优先级（通配类型）
       ├─ 4 域规则：域默认（类型/优先级全空）
       ├─ 5 全局规则：business_domain_id IS NULL   ← 全局 SLA（兜底）
       └─ 6 未配置 → 不设 SLA（现状行为）
超时判定（evaluateTicket，幂等）
  ├─ 违约 → 执行违约动作一次（按序升级优先级 / 更换处理人 / 添加关注人）→ sla_status=breached
  └─ 违约消除（如客服已首响）→ sla_status 自愈为 tracking
SLA 状态（唯一最终态）
  ├─ tracking（计时中）/ breached（已超时）
  └─ stopped（已结束）← 唯一最终态：工单状态流转到终态即触发（不再有 resolved）
工单结束方式（唯一路径）
  └─ 变更工单状态 → 流转到终态（取消独立的「关闭」功能）
```

## 2. 决策记录（已确认）

| # | 决策点 | 结论 |
|:---|:---|:---|
| D1 | 双层落点与载体 | 平台全局规则（`business_domain_id` 可空）+ 域规则表（现状表） |
| D2 | 规则粒度 | 类型可选 × 优先级可选（下拉，留空=通配，匹配链现状保留） |
| D3 | 超时动作集（第一版） | ① 升级优先级（**按序升级到下一阶段**）② 更换处理人 ③ 添加关注人；JSON 折叠为高级模式兜底 |
| D4 | 触发机制 | **定时扫描纳入本轮**（每分钟扫描超时工单并执行动作）；工作日历计算仍排除（`calendar_id` 维持不参与） |
| D5 | SLA 状态默认状态机 | **不再支持配置「置 SLA 状态」**：超时→`breached`；客服回复（首响）后违约消除→`tracking`；**统一保持一个最终态** |
| D6 | 违约动作幂等 | 新增 `ticket.sla_breach_actioned` 标志列，**每个工单违约动作只执行一次** |
| D7 | SLA 唯一最终态 | **取消 `resolved`**，仅保留 `stopped`（展示「已结束」）为最终态；工单流转到终态 → `stopped`；旧 `resolved` 数据迁移归一 |
| D8 | 取消工单关闭功能 | 移除关闭按钮/入口（详情页「关闭」、队列「关闭」操作），**仅允许变更工单状态**控制流转；流转到终态即视为 SLA 达终态 |

## 3. 数据模型

### 3.1 `sla_rule` 表改造 + `ticket` 表新列

```sql
-- V20260816xxxx__sla_rule_global_support.sql
ALTER TABLE `sla_rule` DROP FOREIGN KEY `fk_sla_rule_domain`;
ALTER TABLE `sla_rule` MODIFY `business_domain_id` bigint unsigned NULL;
-- 全局行约束（应用层校验）：ticket_type_id / priority_level_id / calendar_id IS NULL

-- ticket 表：违约动作一次性标志（幂等守卫，D6）
ALTER TABLE `ticket` ADD COLUMN `sla_breach_actioned` tinyint NOT NULL DEFAULT 0 COMMENT '违约动作已执行（每工单一次）';

-- ticket 表：SLA 状态归一（取消 resolved，唯一最终态 stopped，D7）
UPDATE `ticket` SET `sla_status` = 'stopped' WHERE `sla_status` = 'resolved';
```

- 语义：`business_domain_id IS NULL` = 全局默认规则（对齐 F4.13 全局屏蔽词「business_domain_id 为空」先例）
- 全局规则仅允许 `name / first_response_minutes / resolution_minutes / breach_action_json`（`is_urgent_config` 死字段保持默认 0）
- 不新建规则表、不动 `sla_calendar`（日历仍域级，且本轮不参与计算）

### 3.2 权限码

- 新增 `platform.sla.{read,create,update,delete}`：`PermissionCodes.java` 常量注册 + Flyway 菜单/角色种子（platform_admin / super_admin）
- 域端 `domain.sla.*` 不动；平台「业务域代管」区沿用 `domain.sla.*`（现状）
- `ticket.close` 权限码**保留注册**（本轮仅移除前端入口，权限/角色清理随 E-EMP3 状态机统一治理）

### 3.3 违约动作 JSON（`breach_action_json`）结构

保持**开放字典**（Map），本轮扩展键；旧键 `raise_priority_to`/`sla_status` 保留**兼容**（旧规则行为不变；新 UI 不再暴露 `sla_status`）：

```jsonc
{
  "escalate_priority": true,                // ① 按序升级到下一紧急阶段（新增）
  "raise_priority_to": "urgent",            // (旧键，兼容；与 escalate_priority 并存时以此绝对目标优先)
  "assign_to_staff_account_id": 42,         // ② 更换处理人（新增；null/缺省=不换）
  "add_watcher_staff_account_ids": [17, 9], // ③ 添加关注人（新增；追加而非替换）
  "sla_status": "escalated"                 // (旧键，兼容：仍被引擎读取覆盖状态；新配置不走此键)
}
```

- **按序升级**语义：域优先级列表按 `sort_order ASC` 排（urgent=0/high=10/normal=20/low=30，小=更紧急）；当前工单优先级 code 对应级别 → 升级到 **sort_order 更小（更紧急）的下一级**（normal→high→urgent；low→normal）；已是最高级（sort_order 最小）则不动
- **添加关注人**语义：追加（先 `listStaffIds` 合并去重再 `replaceWatchers`），不覆盖已有关注人
- **更换处理人**语义：`updateAssign` 指派目标员工（内部方法，绕过版本乐观锁与「已领取」校验——超时强制指派）
- **执行顺序**（固定）：升级优先级 → 更换处理人 → 添加关注人，同事务

## 4. 匹配链（后端）

`SlaRepository` 增加全局查询，`SlaService.loadPolicy` 改为两段式：

1. 现有 `findPolicy(domainId, ticketTypeId, priorityCode)`（域内通配回退，SQL 不动）
2. 未命中 → `findGlobalPolicy()`：`SELECT ... FROM sla_rule WHERE business_domain_id IS NULL ORDER BY id DESC LIMIT 1`
3. 仍无 → `new TicketSlaPolicy(null, null, null)`（现状：不设 deadline，`sla_status='tracking'`）

「事项 SLA 为主」由查询顺序天然保证：域内任何命中即短路，全局永不覆盖域规则。

## 5. SLA 状态默认状态机与动作执行

### 5.1 状态机（用户拍板，D5/D7/D8）

`sla_status` 取值固定三态 + 旧数据自定义值（下一次评估归一化）：

| 状态 | 触发 |
|:---|:---|
| `tracking` 计时中 | 创建/应用规则（现状）；**客服回复/领取/转 processing 后违约条件消除 → evaluateTicket 自愈为 tracking** |
| `breached` 已超时 | 任一 deadline 已过且对应完成时间为空（evaluateTicket 设置；兼容旧键 `sla_status` 覆盖） |
| `stopped` 已结束（**唯一最终态**） | 工单状态**流转到终态**（`resolved`/`closed`/`withdrawn`/`merged` 等终态码统一映射，TicketMapper.xml；E-EMP3 后可切换为按 flow 配置 `state_type='terminal'` 判定） |

要点：

- **唯一最终态**：取消 `resolved`；所有终态流转统一落 `stopped`（前端展示「已结束」，`slaStatusMeta` 移除 resolved 映射）
- **终态判定**：本轮维持现状硬编码终态码集合（`resolved/closed/withdrawn/merged`），SQL 分支统一为 → `stopped`；**「最终态」的权威来源为可配置工作流**（`ticket_status.state_type='terminal'` / `status_flow_config.states[].state_type='terminal'`），E-EMP3 将运行时接入可配置工作流后，此处改为按 flow 判定（预留切换点）
- **「回复→计时中」的实现**：由 evaluateTicket 的**自愈**语义天然达成——客服回复调用 `recordFirstResponse`（置 `sla_first_responded_at`），随后 `refreshTicketSla` 重算发现首响违约消除 → 状态回到 `tracking`；若**解决时限仍超时**，则保持/恢复 `breached`（如实展示，避免「回复了却仍超时」误判）——「回复重置」针对首响超时场景
- **客户回复不解除超时**：不置首响时间，违约事实不变，状态不变（超时未处理仍是已超时）
- **终态保护**：`stopped` 为终态，evaluateTicket 跳过（不覆盖，不违约）
- **旧自定义值**（旧规则 `sla_status` 键，如 `escalated`）：超时瞬间仍按旧键覆盖；后续评估归一化为状态机标准值（违约→breached / 消除→tracking / 终态→stopped）

### 5.2 `evaluateTicket` 改造（幂等，D6）

```
evaluateTicket(businessDomainId, ticketId):
  snapshot; now
  sla_status = stopped → return（终态，跳过）
  frBreached = 首响 deadline 已过 && 未首响
  resBreached = 解决 deadline 已过 && 未解决
  if (!frBreached && !resBreached):
      if sla_status ≠ tracking → updateSlaStatus(tracking)   // 自愈
      return decision(false, false, ...)
  // 违约：
  if (!sla_breach_actioned):
      执行动作（升级优先级/换处理人/加关注人）→ 置 sla_breach_actioned=1
  updatePriorityAndSlaStatus(priority, breachAction.sla_status ?? 'breached')
```

- **动作每工单只执行一次**：`sla_breach_actioned` 置位后不再重复（状态恢复 tracking 再超时，仅翻转状态，不重复升级/换人/加关注）——避免「回复→tracking→再超时」循环导致优先级反复升级
- 新增仓储方法：`updateSlaStatus(ticketId, domainId, status)`（自愈用）；`markBreachActioned(ticketId)`；扫描候选 SQL 追加 `sla_breach_actioned = 0` 条件可减负（非必需）

### 5.3 定时扫描 `SlaScanJob`（新增，D4）

- `@Scheduled(cron = "0 * * * * *")`（每分钟），注入既有 `Clock`
- 候选 SQL：`sla_status='tracking' AND ((sla_first_response_deadline < now AND sla_first_responded_at IS NULL) OR (sla_resolution_deadline < now AND sla_resolved_at IS NULL))`，分批（LIMIT 100）扫描
- 每个候选调 `evaluateTicket`（动作一次性标志保证不重复处置）
- 单实例部署，无需分布式锁（MVP）；扫描失败单条隔离，不中断整批
- 工单事件触点 `refreshTicketSla` 保留（即时性），扫描兜底（按时性）——两者共享同一 `evaluateTicket`

## 6. 取消关闭功能（D8）

- **现状事实**：后端无独立 close 端点——「关闭」= `changeTicketStatus(status='closed')`（`updateAdminTicketStatus`）；前端入口：详情页「关闭」按钮（`detail.tsx`，`AuthGuarded ticket.close`）+ 队列「关闭」行操作（`index.tsx`）
- **本轮改动**：
  - 移除详情页「关闭」按钮与「关闭确认」Modal 入口（保留「变更状态」入口/状态下拉）
  - 移除工单队列「关闭」行操作与批量关闭入口
  - 后端 `changeTicketStatus` 不动（状态控制唯一通道）；`ticket.close` 权限码保留注册（清理随 E-EMP3）
- **SLA 联动**：工单结束唯一路径 = 变更状态 → 流转到终态 → `sla_status='stopped'`（SLA 达最终态）

## 7. API 设计

```text
GET    /api/v1/admin/platform/sla-rules        # 全局规则列表
POST   /api/v1/admin/platform/sla-rules        # 创建（校验类型/优先级/日历必空）
PUT    /api/v1/admin/platform/sla-rules/{id}   # 更新（同上校验）
DELETE /api/v1/admin/platform/sla-rules/{id}   # 删除
```

- 新 `PlatformSlaController`（`/api/v1/admin/platform` 命名空间，对齐 `PlatformTicketConfigController` 先例），全部 `@RequirePermission(PLATFORM_SLA_*)`
- 域内端点（`SlaController` `/api/v1/admin/domains/{domainId}`）不动；`SlaRuleView` 复用
- 前端：`@uniondesk/shared` 增加 `fetchGlobalSlaRules / createGlobalSlaRule / updateGlobalSlaRule / deleteGlobalSlaRule`（对齐既有 `fetchSlaRules` 封装风格，注意 list `{total, items}` 解包约定）
- 动作执行所需数据源复用现有接口：优先级列表（`fetchDomainPriorityLevels`，含 sort_order）、域成员（member-picker 数据源）、关注人（既有 watcher 端点）

## 8. 前端表单（易用化）

### 8.1 域端 `/domain/sla` 规则编辑器

| 字段 | 现状 | 改造后 |
|:---|:---|:---|
| 规则名称 | Input | 不变 |
| 事项类型 | InputNumber（手填 ID） | **Select**：`fetchDomainTicketTypes`，allowClear，placeholder「全部类型（域默认）」 |
| 优先级 | InputNumber（手填 ID） | **Select**：`fetchDomainPriorityLevels`，allowClear，placeholder「全部优先级」 |
| 日历 | InputNumber（手填 ID） | Select（`fetchSlaCalendars`）**disabled** + Tooltip「工作日历计算暂未启用（预留）」 |
| 首响/解决分钟 | InputNumber | 不变 |
| 紧急配置 | Switch | 不变（死字段，标注遗留，不动逻辑） |
| 超时动作 | 裸 JSON TextArea | **可视化块**（见 8.3）；折叠「高级模式」= 原 JSON TextArea，双向同步（提交时统一序列化为 `breachAction` Map） |

### 8.2 平台端 `/platform/sla-management`

- 顶部 Segmented：`全局默认规则` | `业务域规则（代管）`
- 全局区：规则列表（无类型/优先级/日历列）+ 编辑器（仅名称/首响/解决/动作，复用域端抽离的 `SlaRuleForm`，隐藏域级字段）
- 代管区：现状「选域 → 规则 CRUD」，表单与域端同一 `SlaRuleForm`（同步易用化）
- 组件抽离：`pages/domain/sla/` 内新增私有子组件（按 §2.5 单组件复用规则，不新建公共目录文件）

### 8.3 超时动作可视化块

| 动作 | 控件 | 值 |
|:---|:---|:---|
| 升级优先级 | Checkbox/Switch「超时按序升级优先级」 | 勾选 → `escalate_priority: true` |
| 更换处理人 | 成员单选（复用 member-picker，域成员数据源） | → `assign_to_staff_account_id` |
| 添加关注人 | 成员多选（复用 member-picker） | → `add_watcher_staff_account_ids` |

- **不再提供「置 SLA 状态」配置项**（D5 默认状态机自动管理）；工单详情/列表的 SLA 状态展示 `slaStatusMeta` 收敛为三态（tracking/breached/stopped「已结束」）
- 高级模式 JSON 兜底可读写同一 `breachAction`（含旧键 `raise_priority_to`/`sla_status`，引擎兼容）

## 9. 数据迁移与发布

1. Flyway `V20260816xxxx__sla_rule_global_support.sql`：DROP FK + `sla_rule.business_domain_id` 改可空 + `ticket.sla_breach_actioned` 新列 + `ticket.sla_status='resolved' → 'stopped'` 归一 + 平台权限码/菜单种子（幂等，以 code 为键）
2. 现有域规则数据零迁移（语义不变：仍是域级；`breach_action_json` 旧键兼容）
3. 发布顺序：后端（匹配链 + 平台端点 + 权限 + 状态机 + 扫描任务 + 终态映射统一）→ 前端（规则表单 + 关闭入口移除 + slaStatusMeta 三态）（同版本）

## 10. 边界（Out of Scope）

- **工作日历计算**（`calendar_id` 仍不参与 deadline 计算；表单 disabled「暂不生效」）——建议后续独立任务激活
- **可配置工作流运行时接入**（`changeTicketStatus` 改走 `TicketTypeFlowService`/`StatusFlowValidator`、DB 状态码统一）——E-EMP3 已立项；本轮终态判定维持现状硬编码集合，预留切换点
- 「即将超时」阈值语义（E-EMP3 R3 UI 高亮需引擎，独立任务）
- 通知模板联调（超时发站内信/邮件，依赖 F3.11 通知模板）
- SLA 暂停/恢复（`sla_paused_duration` 字段已有）
- 动作编排顺序自定义（本轮固定执行顺序：升级优先级 → 换处理人 → 加关注人）
- `ticket.close` 权限码/角色清理（随 E-EMP3 状态机治理）

## 11. 测试

- 单元/集成（`SlaServiceTests` 等）：
  - 匹配链：域×类型×优先级 > 域×类型 > 域默认 > 全局默认 > 不设 SLA；事项优先（域与全局同时命中）
  - 动作：按序升级（normal→high→urgent；最高级不动；`raise_priority_to` 并存时优先）、更换处理人、添加关注人（追加不覆盖）
  - **幂等/状态机**：违约→`breached` 且动作执行一次（`sla_breach_actioned=1`）；回复（首响）后违约消除→`tracking`；再超时仅翻回 `breached` 不重复动作；解决时限仍超时回复后保持 `breached`；**终态统一**——流转 resolved/closed/withdrawn/merged 均→`stopped` 且不再违约；客户回复不解除超时；旧 `resolved` 数据迁移后展示一致
  - 平台端点校验：全局规则类型/优先级/日历非空拒绝
- 扫描任务（`SlaScanJob` 测试，Clock 注入）：超时候选被处置；已处置跳过；单条失败不中断
- 回归：`SlaRuleCrudIntegrationTest`、`SlaTimingEngineTest`（旧键兼容）、`TicketWorkflowTests`（含 FR 测试）不回归
- 前端：typecheck + 浏览器冒烟（AC8）；工单详情/队列无「关闭」入口，仅「变更状态」可流转

---

## 附录：术语表（Glossary）

| 术语 | 定义 |
|:---|:---|
| SLA（服务级别协议） | 对工单处理时限的承诺：首响时限、解决时限（分钟） |
| 全局 SLA 规则 | 平台级默认规则（`sla_rule.business_domain_id IS NULL`），跨业务域兜底，不关联类型/优先级/日历 |
| 事项 SLA 规则 | 业务域内按事项类型/优先级配置的规则（现状 `sla_rule` 行），与全局冲突时优先 |
| 通配规则（域默认） | `ticket_type_id IS NULL` 且/或 `priority_level_id IS NULL` 的域内规则，按精确度回退 |
| 违约动作（breach action） | SLA 超时后执行的动作：按序升级优先级 / 更换处理人 / 添加关注人（**每工单一次**，`sla_breach_actioned`） |
| 按序升级 | 按域优先级 `sort_order ASC`（小=更紧急）升至下一紧急级别；已是最高级则不动 |
| 关注人（watcher） | `ticket_watcher` 表员工关注列表，工单详情可维护；超时动作可追加 |
| SLA 状态（sla_status） | 默认状态机三态：`tracking`（计时中）/`breached`（已超时）/`stopped`（**已结束，唯一最终态**，取消 resolved）；超时→breached、客服首响后违约消除→tracking、工单流转到终态→stopped |
| 自愈（self-heal） | evaluateTicket 在违约条件消除时将状态归一为 tracking（回复/领取/转 processing 后自然触发） |
| 终态（terminal state） | 工单可配置工作流的状态类型（`ticket_status.state_type='terminal'` / `status_flow_config.states[].state_type='terminal'`）；流转到终态即 SLA 达最终态；本轮以硬编码终态码集合判定（resolved/closed/withdrawn/merged），E-EMP3 后切换 flow 判定 |
| 关闭功能 | 独立的工单关闭入口（详情页「关闭」按钮、队列「关闭」操作）——**已取消**，结束工单唯一路径为「变更状态 → 流转到终态」 |
| 定时扫描（SlaScanJob） | 每分钟扫描超时工单并执行违约动作；与事件触点共享幂等 evaluateTicket |
| 工作日历（calendar） | `sla_calendar` 域级日历配置；**预留能力，未参与 deadline 计算** |
| 紧急配置（is_urgent_config） | `sla_rule` 遗留字段，全链路存在但未参与匹配与判定（死字段） |
