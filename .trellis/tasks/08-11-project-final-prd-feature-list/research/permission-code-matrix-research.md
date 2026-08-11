# Research: 操作矩阵能否一并映射权限代码（权限码）

- **Query**: 操作矩阵新增时，能否一并做到权限代码的适配？每个功能/操作对应哪些权限码
- **Scope**: 内部（仓库代码 + 文档 + Flyway 种子取证）
- **Date**: 2026-08-11

## 结论速览

1. **可以，且大部分可精确取码**：54 条中约 32 条（59%）可直接从代码/Flyway 取到权限码，约 6 条（11%）需推断映射，约 16 条（30%）无权限码（客户端 12 + 模板遗留 3 + 登录豁免 1）。
2. **推荐混合形态**：独立「权限码对照表」（形态 B）为主，操作矩阵不新增权限码列，仅在「备注」列按功能编号交叉引用。
3. **重要差异**：PRD §4.1.3 / foundation-rules §3.3 定义的 `模块:操作`（如 `ticket:delete`）是**逻辑格式**，实现权限码是**点分格式**（`ticket.delete`），数据范围后缀为 `ticket.view.self` / `ticket.view.domain_all`。矩阵应用实现码。

---

## 1. 权限码权威来源清单（全）

### 1.1 后端代码层（最高权威）

| 来源 | 路径 | 内容 |
|---|---|---|
| 权限码常量 | `UnionDesk/uniondesk-iam/src/main/java/com/uniondesk/iam/core/PermissionCodes.java` | 全量 ~190 个常量（216 行文件），点分字符串，如 `platform.user.disable` |
| 权限定义 catalog | `UnionDesk/uniondesk-iam/src/main/java/com/uniondesk/iam/admin/AdminPermissionCatalog.java` | 同码 `PermissionDefinition`（scope / httpMethod / pathPattern），如 `new PermissionDefinition(PermissionCodes.PLATFORM_USER_READ, ..., "GET", "/api/v1/admin/staff")` |
| 控制器注解 | 各模块 Controller `@RequirePermission(PermissionCodes.XXX)` | 分布在 `uniondesk-iam`（Staff/Organization/Iam/DomainRole 等）、`uniondesk-domain`（Domain/DomainCustomer/DomainMember/InvitationCode 等）、`uniondesk-ticket`（PlatformTicketConfigController 等）、`uniondesk-support`（Sla/NotificationTemplate/Inbox）、`uniondesk-app`（AuthController） |

**命名规律**（来自 `docs/architecture/permission-registration-checklist.md` §命名约定）：

```
platform.domain.control.<feature>.<action>   # 域控制台（平台岗）— 如 platform.domain.control.ticket_type.read
platform.log.<type>.read                     # 平台级日志 — 如 platform.log.audit.read
domain.member.<action>                       # 域内成员 — 如 domain.member.update_roles
platform.domain.list.read                    # 域列表
```

结构为 `{scope}.{resource}.{action}`，action 词根：`read / create / update / delete / update_status / reset_password / update_roles / bind / import / export / restore / disable / claim / assign / reply / close / merge` 等。

**精确示例码（5–10 个，逐条核实）**：

| 码 | 出处 |
|---|---|
| `platform.domain.control.ticket_type.read` | PermissionCodes.java:126 + Flyway V202606170001 + platform-domain-permissions.ts:20 |
| `platform.blocked_word.create` | PermissionCodes.java:118 + Flyway V202606080001:42 + platform/blockwords/index.tsx:216 |
| `domain.member.update_roles` | PermissionCodes.java:108 + domain/members/index.tsx:758 |
| `domain.customer.reset_password` | PermissionCodes.java:22 + DomainCustomerController:114 + Flyway V20260806120000 |
| `platform.user.offboard_pool.read` | PermissionCodes.java:64 + AdminPermissionCatalog.java:107 |
| `ticket.view.domain_all` | PermissionCodes.java:195 + permission-code-labels.ts:130 + detail-overview.tsx:29 |
| `platform.role.bind` | PermissionCodes.java:50 + StaffController.java:169 |
| `domain.sla.create` | PermissionCodes.java:171 + SlaController.java:40 |
| `domain.config.read` | PermissionCodes.java:166 + domain/config/index.tsx:39 |
| `platform.domain.control.general.delete` | PermissionCodes.java:87 + DomainController.java:87 |

### 1.2 数据库层（Flyway 种子，权限登记落地）

- 目录：`UnionDesk/uniondesk-app/src/main/resources/db/migration/current/`（约 70 个文件，`archive/` 为历史）
- 权限种子模式（代表性文件）：
  - `V202606170001__platform_domain_control_ticket_type.sql` — `iam_permission` INSERT（code/name/scope/resource_code/action_code/http_method/path_pattern）+ `iam_admin_menu` hidden catalog `PLATFORM-DOMAIN-CONTROL-*` + button 节点（`permission_code`）+ `iam_role_permission` 绑定 `super_admin`/`platform_admin`
  - `V202606080001__platform_blocked_word_permissions.sql` — 演示**重命名轨迹**：`domain.blocked_word.*` → `platform.domain.control.blocked_word.*`（双层架构：域内码迁平台岗）+ 平台全局码 `platform.blocked_word.*`
  - 其他：`V202605200006__platform_domain_list_detail_menu_and_permissions.sql`、`V202606060001__domain_member_permissions_and_menu.sql`、`V202606090001__platform_domain_logs_permissions.sql`、`V202607070003__platform_ticket_config_status_permissions.sql`、`V20260806120000__domain_customer_reset_password_permission.sql`
- 角色绑定：每个权限码都有 `INSERT IGNORE INTO iam_role_permission ... WHERE r.code IN ('super_admin','platform_admin')`（平台岗）或 `domain_admin`（域内码），可从 Flyway 提取「默认授权角色」

### 1.3 前端引用层（验证/交叉检查）

| 来源 | 路径 |
|---|---|
| 域内权限常量 | `UnionDeskWeb/apps/UnionDeskAdminWeb/src/pages/domain/domain-permissions.ts`（domain.customer.*、domain.blocked_word.*、domain.config.*、domain.ticket_type.*、domain.audit_log.read 等） |
| 域详情/平台侧权限常量 | `UnionDeskWeb/apps/UnionDeskAdminWeb/src/pages/platform/domains/platform-domain-permissions.ts`（platform.domain.control.* 全套 65 行） |
| 权限码中文标签 | `UnionDeskWeb/apps/UnionDeskAdminWeb/src/pages/platform/system/menu/components/permission-code-labels.ts`（含 ticket.* 全套 11 个：ticket.read/create/view.self/view.domain_all/claim/assign/reply.self/reply/close/withdraw.self/merge） |
| 门控组件 | `UnionDeskWeb/apps/UnionDeskAdminWeb/src/components/auth-guarded/index.tsx`（AuthGuarded），页面级 `hasPermission` |
| 客户端 | `UnionDeskCustomerWeb/src` **grep 0 命中** `permission/AuthGuarded/hasPermission` → 客户端无权限码体系 |

### 1.4 文档层

| 文档 | 相关内容 |
|---|---|
| `docs/architecture/permission-registration-checklist.md` | 登记步骤（代码 3 步 + DB 3 步 + 前端 4 步 + 验证 4 步）；命名约定；**禁止旧前缀** `domain.admin.*`、`domain.audit_log.*`、`platform.domain.customer.*` |
| `docs/architecture/iam-rbac-dual-table-architecture.md` | 双表分工：`iam_permission`（What）/ `iam_admin_menu`（Where to grant），`permission_code` 字符串关联 |
| `docs/product/foundation-rules.md` §3.3 权限项 | 格式 `模块:操作`（如 `ticket:delete`）；数据范围后缀 `:self` / `:domain_all`；FR-03：无按钮权限 → 按钮不可见 + API 403 + 中文 |
| `docs/product/prd.md` §4.1.3 权限体系 | 权限项精确到按钮，`模块:操作`，`:self`/`:domain_all` 控制数据范围；域管理员可组合自定义角色（≤20 个） |

> **格式差异注意**：PRD/foundation-rules 用 `模块:操作`（冒号），实现码为点分 `ticket.delete`；数据范围在实现中体现为独立码 `ticket.view.self` / `ticket.view.domain_all`（而非后缀）。

---

## 2. 54 条功能权限码覆盖度评估

### 2.1 逐类结论

**A. 可直接取码（feature-list 备注 / Flyway / 代码直接命中）— 约 32 条**

管理端已实现项基本全部命中。示例（精确到码）：

| 功能 | 权限码（实现码） |
|---|---|
| F3.1 工单类型设计 | `platform.domain.control.ticket_type.{read,create,update,delete}` / `domain.ticket_type.*`（feature-list 备注已写） |
| F3.4 事项属性与状态 | `domain.ticket_attribute.*`、`domain.ticket_status.*`、`domain.ticket_type.*`（feature-list 备注已写） |
| F3.3 成员/客户/角色 | `domain.member.{read,create,update_roles,update_status,delete}`、`domain.customer.*`、`domain.role.*` |
| F3.5 域客户增强 | `domain.customer.{update,update_status,reset_password}` |
| F3.9 域屏蔽词库 | `domain.blocked_word.*`（前端在用；DB 已迁 `platform.domain.control.blocked_word.*`，见 §4 风险） |
| F3.12 域日志 | `domain.audit_log.read`、`domain.login_log.read` |
| F3.13/3.14 域角色/菜单 | `domain.role.*`、`domain.menu.*` |
| F3.6 入域配置 | `domain.invitation_code.*` + `domain.general.read`（onboarding 页 auth 数组实证） |
| F3.7 域基础设置 | `domain.general.{read,update,update_status}` |
| F3.8 域参数 KV | `domain.config.{read,update}` |
| F3.10 域运营概览 | `domain.overview.read`（AdminPermissionCatalog.java:27） |
| F2.4 业务域端首页 | `domain.home.read`（feature-list 备注已写） |
| F4.7 用户管理 | `platform.user.{read,create,update,disable,reset_password,restore,delete}` |
| F4.8 组织/部门 | `platform.organization.{read,create,update,delete}` |
| F4.9 角色管理 | `platform.role.{read,create,update,delete}`、`platform.role_permission.*`、`platform.role.bind` |
| F4.10 菜单管理 | `platform.menu.*` |
| F4.13 全局屏蔽词 | `platform.blocked_word.{read,create,delete}`（feature-list 备注已写） |
| F4.14 事项配置 | `platform.ticket_config.{attr,type,status,template}.*`（16 个码） |
| F4.12 审计/登录日志 | `platform.log.audit.read`、`platform.log.login.read` |
| F4.1 业务域管理 | `platform.domain.list.read`、`platform.domain.create`、`platform.domain.control.*`（域详情全套） |
| F4.20 邀请码面板 | `domain.invitation_code.*`（平台侧可复用） |
| F4.3 离职池 | `platform.user.offboard_pool.{read,export,batch_restore}` |
| F4.17 附件上传 | `attachment.upload` / `attachment.download` |
| F4.16 站内信 | `inbox.read`、`inbox.mark_read` |
| F2.1 工单队列 | `ticket.*` 全套 11 码（feature-list 备注已写） |
| F4.15 SLA 规则 | `domain.sla.*`（平台侧 SLA 页复用同一后端） |

**B. 需推断 / 待确认（权限码存在但需人工映射）— 约 6 条**

| 功能 | 推断码 | 依据 |
|---|---|---|
| F4.2 模板中心 | `platform.ticket_config.template.*` | 权限已注册，菜单已隐藏（V20260726092200） |
| F4.4 系统设置 | `platform.system_config.{read,update}` | PermissionCodes.java:168-169 |
| F4.6 平台首页仪表盘 | `platform.dashboard.read` | PermissionCodes.java:212 |
| F4.19 域配置 KV（平台侧） | 待确认（域详情 Tab 与 `/platform/domain-config` 同源，疑 `platform.domain.control.config.*` 或复用 `domain.config.*`） | 未在 PermissionCodes 中直接命中域详情 KV 码 |
| F3.2 SLA 与通知模板 | `domain.sla.*` + `domain.notification_template.*` | SLA 已实现；通知页占位「菜单与权限已就绪」 |
| F2.2 SLA 感知 | `domain.sla.*`（读） | 依赖 F4.15 引擎 |

**C. 无权限码（认证豁免 / 公开 / 模板遗留）— 约 16 条**

| 类别 | 条目 | 结论 |
|---|---|---|
| 客户端全部 | F1.1–F1.12（12 条） | 客户身份走 RequireSession（登录即可），**不走 iam_permission 体系**；后端客户 API（如 `POST /api/v1/domains/{domain_id}/tickets`）无 `@RequirePermission` 管理岗门控 |
| 登录/公开页 | F4.5 平台端登录（1 条） | 认证豁免；动态菜单/权限快照为登录后行为，矩阵填「—（认证豁免）」 |
| 模板遗留 | F3.15、F3.16、F4.22（3 条） | 非业务功能，无业务权限码（`/access/*` 等 demo 路由不受控） |
| 占位但权限已就绪 | F3.11 域通知配置、F4.18 导入导出、F4.21 组织配置 | **有码但页面占位**：`domain.notification_template.*`、`platform.user.import` + `platform.user.offboard_pool.export`、`platform.organization.*` |

### 2.2 统计

| 类别 | 条数 | 占比 |
|---|---|---|
| 可直接取码 | ~32 | 59% |
| 需推断 | ~6 | 11% |
| 无权限码（客户端/公开/模板遗留） | ~16 | 30% |

管理端（F2/F3/F4，42 条）覆盖率约 90% 有码可映射；客户端（F1，12 条）100% 无权限码（预期行为，非缺口）。

---

## 3. 与操作矩阵的结合形态（推荐：混合，B 为主 + A 轻量引用）

### 形态对比

| 维度 | 形态 A：矩阵内嵌权限码列 | 形态 B：独立对照表 |
|---|---|---|
| 结构 | 每行追加 1 列（功能级通配码，特殊操作单元格注明） | 功能编号 \| 权限码 \| 来源 \| 默认授权角色 |
| 优点 | 单表即查、紧凑 | 可追溯（含来源列）；不受列宽限制；可容纳一功能多码 |
| 缺点 | 一个功能对应 1–4 个码（如 F4.14 有 16 个 `platform.ticket_config.*`），与 6 个操作列（列表/详情/新增/编辑/删除/其他）相乘 → 单元格爆炸；无来源列，无法区分 Flyway/AC/推断 | 与操作矩阵分离，需功能编号交叉引用 |
| 客户端条目 | 需填 12 个「—」 | 集中标注「—（认证豁免/公开）」一次说明 |

### 推荐方案（混合）

1. **独立「权限码对照表」**（§5 结构）：列 = 功能编号 | 功能 | 权限码（实现码） | 默认授权角色 | 来源（Flyway V*/PermissionCodes.java/AC/推断）。
2. **操作矩阵不新增权限码列**：矩阵保持 列表/详情/新增/编辑/删除/其他操作/校验边界 7 列；仅在「备注」列对敏感操作写精确码（如 F3.1 删除列注 `platform.domain.control.ticket_type.delete`），避免重复。
3. **头注声明**：feature-list 是只读快照，权限码以 `PermissionCodes.java` + Flyway 为准；对照表每行「来源」列天然标注漂移跟踪点。

理由：54 行 × 7 列矩阵内嵌权限码会因一功能多码（最多 16 码）导致列宽失控；对照表补「来源 + 角色」两列是矩阵放不下的关键追溯信息；且客户端 12 条无码，A 形态下会产生 12 行无效噪音列。

---

## 4. 缺口与风险清单

| # | 风险/缺口 | 处理建议（标注策略） |
|---|---|---|
| 1 | 客户端 12 条无权限码 | 对照表填「—（认证豁免/公开）」；操作矩阵无需任何权限码标注 |
| 2 | 模板遗留 3 条（F3.15/F3.16/F4.22）无业务码 | 填「—（模板遗留/非业务）」 |
| 3 | 权限码与角色绑定（super_admin / platform_admin / domain_admin） | 对照表加「默认授权角色」列，从 Flyway `iam_role_permission` INSERT 的 `WHERE r.code IN (...)` 提取；agent 角色码（如 `ticket.*`）绑定待 US-S3-04 联调确认 |
| 4 | 取不全的条目（F4.19、F3.2 通知模板等） | 标注「**待确认/以代码为准**」；`platform.domain.control.config.*` 未在 PermissionCodes 命中，需主 agent 决定是否补查后端 KV Controller |
| 5 | **重命名漂移**：`domain.blocked_word.*` 已被 Flyway V202606080001 迁为 `platform.domain.control.blocked_word.*`，但 PermissionCodes.java:23-25 与前端 domain-permissions.ts 仍保留 `domain.blocked_word.*` 常量 | 矩阵/对照表以**最新 Flyway 状态为准**，对在迁码标注「旧码保留，新码为准」；checklist 明确禁止旧前缀 `domain.admin.*`/`domain.audit_log.*`/`platform.domain.customer.*` |
| 6 | 格式差异：PRD `模块:操作`（冒号）vs 实现点分码 | 对照表用实现码，头注说明映射（`ticket:delete` → `ticket.delete`；数据范围 `:self`/`:domain_all` → `ticket.view.self`/`ticket.view.domain_all`） |
| 7 | S3+ 新增页无 Story 覆盖（F3.10 概览、F3.5 增强、F1.11/F1.12 客户端） | 管理端两条已有码（`domain.overview.read`、`domain.customer.*`），标注「S3+ 新增，无 Story，码以代码为准」；客户端两条填「—」 |
| 8 | 占位页「权限已就绪」（F3.11/F4.18/F4.21） | 对照表填码 + 来源，状态列沿用 feature-list「规划中（占位）」；矩阵权限码照填 |
| 9 | feature-list 快照漂移 | 头注固定文案：**「权限码以 PermissionCodes.java 与 Flyway migration 为准，本表为参考快照（2026-08-11）」** |

---

## 5. 推荐的最小表格结构示例（对照表，2–3 行）

```markdown
> 权限码以 `PermissionCodes.java` 与 Flyway migration 为准，本表为参考快照（2026-08-11）。
> 格式说明：PRD §4.1.3 的 `模块:操作`（如 `ticket:delete`）对应实现码 `ticket.delete`；数据范围后缀 `:self`/`:domain_all` 对应 `ticket.view.self`/`ticket.view.domain_all`。

| 功能编号 | 功能 | 权限码（实现码） | 默认授权角色 | 来源 |
| :--- | :--- | :--- | :--- | :--- |
| F3.1 | 工单类型设计 | `platform.domain.control.ticket_type.{read,create,update,delete}`、`domain.ticket_type.*` | super_admin / platform_admin | Flyway V202606170001 + PermissionCodes.java:126 |
| F4.13 | 全局屏蔽词 | `platform.blocked_word.{read,create,delete}` | super_admin / platform_admin | Flyway V202606080001 + US-S2-05 |
| F1.6 | 登录 | —（认证豁免，客户端会话） | — | 无权限码 |
```

---

## Caveats / Not Found

- **未确认**：`platform.domain.control.config.*`（F4.19 域配置 KV 平台侧）未在 PermissionCodes.java 命中，需以域配置 Controller 代码为准；`ticket.*` 的 agent 角色绑定未在 Flyway 抽查到（US-S3-04 联调项）。
- **代码与 DB 已存在重命名不一致**：`domain.blocked_word.*` 三码在 PermissionCodes.java 与前端 domain-permissions.ts 保留，但 iam_permission 表中已迁为 `platform.domain.control.blocked_word.*`（V202606080001）。本调研只记录事实，未做判断。
- **客户端后端 API 门控**未逐接口核实（如 `/api/v1/domains/{domain_id}/tickets` 是否纯 RequireSession），推断依据为 CustomerWeb 前端 0 权限引用 + feature-list 备注；如需逐接口精确断言需另查 AuthController/customer 模块拦截器配置。
- PRD §4.1.3 的「20 个自定义角色上限」等规则不在本次调研范围。
