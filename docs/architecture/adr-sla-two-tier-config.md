# ADR：SLA 双层配置架构（全局 SLA + 事项 SLA）

| 文档版本 | 日期 | 状态 |
|:---|:---|:---|
| 1.0 | 2026-08-16 | 提议（第 1 轮决策待用户确认） |
| 1.1 | 2026-08-16 | 已确认（D1/D2 默认；D3 动作扩展；D4 扫描纳入本轮） |
| 1.2 | 2026-08-16 | 已确认（D5 SLA 状态默认状态机；D6 动作一次性标志） |
| 1.3 | 2026-08-16 | 已确认（D7 唯一最终态取消 resolved；D8 取消关闭功能） |

> **已被 [ADR-005](adr-005-sla-config-redesign.md) 部分取代**（2026-08-16）：D1 双层规则 / D2 匹配链 / D3 规则粒度 改由「域内单份 SLA 配置」取代；D5/D6/D7/D8/D9/D10 仍有效（三态状态机、动作一次性、唯一终态、取消关闭、定时扫描、工作日历排除→现由 ADR-005 纳入）。

> 配套任务：[`.trellis/tasks/08-16-sla-config-optimization/`](../.trellis/tasks/08-16-sla-config-optimization/prd.md)（prd.md / design.md）。
> 术语定义见 design.md「附录：术语表」。

---

## 1. 背景与问题

- **无全局 SLA 概念**：`sla_rule.business_domain_id` NOT NULL、SLA 接口全部挂 `/api/v1/admin/domains/{domainId}`；平台页 `/platform/sla-management` 仅是「选域代管」，feature-list F4.15「平台级 SLA 保留」未落地为独立概念。
- **配置不可用**：规则表单手填类型/优先级/日历 ID、违约动作裸 JSON，普通域管理员无法配置；下拉数据接口（`fetchDomainTicketTypes`/`fetchDomainPriorityLevels`）已就绪未复用。
- **死字段**：`calendar_id`（工作日历）、`is_urgent_config`（紧急配置）从未参与计算；无定时扫描，超时判定仅事件触点惰性触发。

## 2. 决策

| # | 决策 | 说明 |
|:---|:---|:---|
| D1 | **双层架构：平台全局规则 + 域规则表** | 全局规则 = `sla_rule.business_domain_id IS NULL`（对齐 F4.13 全局屏蔽词先例）；不新建表；全局规则仅名称/首响/解决/动作，类型/优先级/日历必空 |
| D2 | **事项 SLA 为主** | 匹配链短路：域规则（类型+优先级 > 仅类型 > 仅优先级 > 域默认）命中即用；未命中回退全局默认；再未配则不设 SLA（现状行为） |
| D3 | **规则粒度：类型可选 × 优先级可选** | 下拉选择、留空=通配；保留现状匹配链 |
| D4 | **动作集：按序升级优先级 / 更换处理人 / 添加关注人** | `breach_action_json` 扩展键（`escalate_priority`/`assign_to_staff_account_id`/`add_watcher_staff_account_ids`），旧键 `raise_priority_to`/`sla_status` 兼容；按序升级 = 按域优先级 `sort_order ASC` 升到下一紧急级别；关注人为追加语义；更换处理人内部强制指派（绕过版本锁）；JSON 折叠为高级模式 |
| D5 | **SLA 状态默认状态机（不再配置「置 SLA 状态」）** | 超时→`breached`；客服回复（首响）后违约消除→`tracking`（evaluateTicket 自愈；解决时限仍超时保持 `breached`；客户回复不解除超时）；**统一保持一个最终态**（见 D7） |
| D6 | **违约动作每工单仅执行一次** | 新增 `ticket.sla_breach_actioned` 标志列；状态恢复 tracking 后再超时只翻转状态，不重复执行动作（防「回复→tracking→再超时」循环导致优先级反复升级） |
| D7 | **SLA 唯一最终态（取消 resolved）** | `sla_status` 收敛三态：tracking/breached/stopped（展示「已结束」）；取消 `resolved`，旧数据迁移归一 `stopped`；工单状态流转到终态（resolved/closed/withdrawn/merged 等硬编码终态码集合，E-EMP3 后切换 flow `state_type='terminal'` 判定）→ `stopped`，终态不被 evaluateTicket 覆盖 |
| D8 | **取消工单关闭功能** | 移除详情页「关闭」按钮/关闭确认入口、队列「关闭」行操作/批量关闭入口；结束工单唯一路径 = 变更状态 → 流转到终态 → SLA 达最终态；后端 `changeTicketStatus` 保留为状态控制唯一通道；`ticket.close` 权限码保留注册（清理随 E-EMP3） |
| D9 | **定时扫描纳入本轮** | `SlaScanJob` 每分钟（`@Scheduled` + 既有 `Clock`），候选=`tracking` 且任一时限已过，分批 LIMIT 100；单实例无分布式锁；超时动作按时自动执行 |
| D10 | **工作日历仍排除本轮** | `calendar_id` 不参与计算，表单 disabled「暂不生效」；独立任务激活 |
| D11 | **平台端新增 `platform.sla.*` 权限码** | 全局规则配置走 `/api/v1/admin/platform/sla-rules` + 新码（platform_admin/super_admin）；域代管沿用 `domain.sla.*` |

## 3. 备选方案（未采纳）

| 方案 | 弃因 |
|:---|:---|
| 事项 SLA 内联到事项类型配置（复用 `ticket_type` 遗留列 `sla_first_response_minutes`/`sla_resolve_minutes`） | 与现有规则表双轨并行、匹配逻辑分裂；类型配置页已承载 Formily 表单设计，再叠 SLA 字段耦合过重；遗留列历史上即未被引擎使用 |
| 只做域内两层（域默认 + 按类型），不引入跨域全局 | 平台无法统一下发兜底，与 feature-list「平台级 SLA 保留」方向不符；各域重复配置 |
| 全局规则复用 `domain.sla.*` 权限（不新增平台码） | 全局规则是跨域数据，平台管理员操作；复用域码语义错位，且域管理员误触风险 |
| 新增独立表 `platform_sla_rule` | `business_domain_id` 可空的单表改造成本更低，且与全局屏蔽词「空域=全局」既有约定一致 |

## 4. 影响与兼容性

- **兼容**：现有域规则数据零迁移（语义不变）；域内 API 与匹配 SQL 不动（仅 `loadPolicy` 增加全局回退查询）；`breach_action_json` 旧键 `raise_priority_to` 兼容；前端既有页面字段值可回显
- **破坏**：`sla_rule.business_domain_id` 去 NOT NULL + 去 FK（遗留外键，符合「数据库不用外键」规范）
- **回滚**：Flyway 下行还原 FK 与 NOT NULL；全局规则行在新代码回滚后不可见（数据无害）
- **联动**：E-EMP3（SLA 感知高亮引擎）与全局规则共存；工作台事项配置挂靠（08-15）不受影响

## 5. 待确认项

1. ~~第 1 轮 D1-D4~~ → 2026-08-16 已确认：D1/D2 默认；D3 动作集（按序升级/换处理人/加关注人）；D4 定时扫描纳入本轮、工作日历仍排除
2. ~~「置 SLA 状态」动作语义~~ → 2026-08-16 已确认（用户拍板）：改走**默认状态机**（D5/D6），不再配置；旧键 `sla_status` 引擎仍兼容（旧规则行为不变）
3. ~~resolved 与关闭功能~~ → 2026-08-16 已确认（用户拍板）：取消 `resolved`，SLA 唯一最终态 `stopped`（D7）；取消工单关闭功能，终态流转即 SLA 达终态（D8）
4. 扫描周期若需调优（默认每分钟）可在 implement 阶段调整
