# Research: 集团统一管理 + 域级精细化权限模型（推翻方案 D「平台只读」目标态）

- **Query**: 用户需求「平台域统一管理用户、各业务域角色统一（一次创建多域生效）、批量停用多域、同时保留按域精细化控制」→ 评估候选模型并给出推荐设计
- **Scope**: mixed（仓库现状取证 + 行业对标）
- **Date**: 2026-08-11
- **取证基线**: 文档快照 2026-08-11（feature-list.md / foundation-rules.md / data-model.md）；代码以当前工作区 HEAD 为准（uniondesk-iam / uniondesk-domain / AdminWeb）

---

## 1. 需求映射（用户原话 → R-A~R-D）

| 需求 | 用户原话片段 | 目标能力 |
|---|---|---|
| **R-A 统一用户** | 「平台域要统一管理用户」 | 平台统一管理全局员工库（`staff_account`），跨域视图（员工 → 多域成员关系） |
| **R-B 统一角色** | 「各业务域角色应该是统一的，平台即可统一一次性创建一个角色的多个业务域名」 | 平台一次创建角色 → 自动生成多个业务域的角色（一次创建，多域生效） |
| **R-C 批量停用** | 「也能一次性关闭一个员工多个业务域名」 | 平台对某员工批量关闭其在 N 个域的成员/权限 |
| **R-D 精细化** | 「或者精细化控制各域角色」 | 保留按域微调（名称/权限包），不是一刀切 |

**对方案 D 的推翻**：`dual-console-permission-design-review.md` 推荐的方案 D（平台 = 员工库 + 平台角色 + 审计监管 + break-glass；域内成员/客户/角色域端全权；平台域详情只读）以「平台默认只读监管」为目标态。本需求要求**平台对角色与成员拥有跨域写能力（模板化统一下发 + 批量停用）**，方案 D 的「平台只读」目标态作废；保留其**安全债结论不变**（US-S1-08 跨域拦截仍为 P0 前置）。

---

## 2. 候选模型对比

### 模型 1：跨域共享角色（Shared Role）
一条 `domain_role` 记录直接关联多个 `business_domain`（改 `business_domain_id` 为可空或新增 `role_domain` 绑定表），成员绑定该角色即在所有关联域生效。

| 维度 | 评估 |
|---|---|
| R-A | ✅ 不涉及 |
| R-B | ✅ 天然一次创建多域 |
| R-C | ✅ 解除成员绑定即全域生效（但做不到「部分域保留」） |
| R-D | ❌ **弱**：共享角色无法按域差异化（域覆盖需额外的 per-domain 覆盖层，等于又造一套实例） |
| 实现成本 | 中：`domain_role` 唯一键 `(business_domain_id, code)` 需改为全局 code 唯一或新建绑定表；`domain_member_role` 语义变化（绑定共享角色 = 多域生效，现有权限校验路径 `findRoleCodesByMemberId` 按域查角色需重构） |
| 权限冲突 | 共享角色权限即所有域生效；域级排除只能靠成员级处理（复杂） |
| 漂移风险 | 无漂移（单一记录），但**无法承载"各域微调"**，与 R-D 直接冲突 |

### 模型 2：角色模板 + 域实例（Template → Per-domain Instances）
平台创建模板（含权限包），一键推送到选定域生成各域 `domain_role` 实例；实例继承模板，域内可改；模板变更可同步/可不同步。

| 维度 | 评估 |
|---|---|
| R-A | ✅ 不涉及 |
| R-B | ✅ apply 生成实例，一次创建多域 |
| R-C | ✅ 成员操作不受影响（绑定在各域实例上，可逐域解除） |
| R-D | ✅ 实例可微调（默认放开，需额外锁定机制防止漂移） |
| 实现成本 | **低~中：与现状天然契合**（`domain_role` 已是 per-domain 实例表，模板只需新增 `role_template` 表 + apply 服务，实例写入复用 `DomainRoleService.createRole` 逻辑） |
| 权限冲突 | 实例间相互独立，无共享冲突；模板推送与域内手改的冲突需同步策略裁决 |
| 漂移风险 | **中**：无同步策略时实例随域内修改漂移，模板失去"统一"意义 |

### 模型 3：混合（模板 + 实例 + 域级覆盖 + 同步策略与锁定字段）—— 推荐候选
模型 2 + 「模板同步策略」（立即同步 / 手动同步 / 不同步）+ 「锁定字段」（域内不可改）。

| 维度 | 评估 |
|---|---|
| R-A | ✅ |
| R-B | ✅ 模板 apply |
| R-C | ✅ 批量停用 API（与角色模型解耦） |
| R-D | ✅ 锁定字段外可微调，锁定字段保持统一 |
| 实现成本 | 中：模型 2 之上增加 `sync_strategy` / `locked_fields` 列与同步服务；成本可控 |
| 权限冲突 | 锁定字段域内改 → 拒绝（403 + 中文）；非锁定字段域覆盖 → 推送时按策略合并（immediate 强制覆盖 / manual 提示冲突由平台决策） |
| 漂移风险 | **低**：同步策略 + 漂移检测（`template_version` 对比，域端展示"落后 N 版本"） |

### 对比结论

| 判据 | 模型 1 共享角色 | 模型 2 模板→实例 | 模型 3 混合 |
|---|---|---|---|
| R-A 统一用户 | ✅ | ✅ | ✅ |
| R-B 一次创建多域 | ✅ | ✅ | ✅ |
| R-C 批量停用 | ✅ | ✅ | ✅ |
| R-D 域级精细化 | ❌ 弱 | ✅（缺约束） | ✅（锁定字段约束） |
| 实现成本（对照现有表） | 中（改唯一键/绑定语义） | 低~中 | 中 |
| 权限冲突规则 | 复杂（域排除） | 简单（实例独立） | 简单（锁定+同步策略） |
| 漂移风险 | 无漂移但无差异 | 中 | 低 |
| **推荐** | ✗ | 过渡 | **✓ 推荐** |

**推荐：模型 3**。理由：① 与仓库现状（`domain_role` 已是 per-domain 实例 + `domain_member_role` 绑定）完全同构，增量最小；② 模型 1 牺牲 R-D 且需要重构域角色唯一键与权限校验路径，收益不抵成本；③ 模型 3 的「统一模板 + 域覆盖 + 同步策略」在业界有成熟先例（见 §4：Jira permission scheme）。

---

## 3. 仓库现状承载能力取证（as-built）

### 3.1 角色体系现状（双轨）
- **轨 A `domain_role` 族（域内角色实例，`data-model.md` 称「域 RBAC 唯一写入路径」）**：
  - `domain_role`（`business_domain_id, code, name, preset`，唯一键 `uk_domain_role_domain_code`）— `V202605200002__rebaseline_current_schema.sql:121-132`
  - `permission_item`（权限项，可与 iam_permission 并存的域角色权限目录，`rebaseline:486-496`）
  - `domain_role_permission`（`domain_role_id + permission_item_id`，`rebaseline:498-506`）
  - `domain_member_role`（`domain_member_id + domain_role_id`，`rebaseline:634-642`）—— **成员绑定的唯一路径**
  - 写入路径：`DomainRoleController.java`（`/api/v1/admin/domains/{domainId}/roles`，`domain.role.*`，**逐域**创建/编辑/删除，`preset` 角色禁改禁删：`DomainRoleService.java:44-96`）；平台只读走 `PlatformDomainRoleController.java`（`platform.domain.roles.*`）
  - **注意**：`DomainRoleService` 中未见「每域自定义角色 ≤20」的数量校验（foundation-rules §3.2 为文档口径，实现待确认）
- **轨 B IAM `role(scope=domain)` 族（定义层）**：
  - `role`（id, code 唯一, scope platform/domain, is_system）+ `iam_role_permission` + `iam_admin_menu`（business）+ `iam_admin_role_menu_relation` — `rebaseline:536-582`
  - 写入路径：`IamController.java:214-266`（`/api/v1/iam/roles`，`platform.role.*`；F4.9 平台角色页可创建 scope=domain 角色）；`AdminMenuService.replaceRolePermissions`（菜单+按钮权限树）
  - `iam_role_binding`：**遗留表**（FK 指向已 DROP 的 `user_account`，`V20260719100446` 卸 FK 但表保留），当前域角色授权实际不走它
- **双轨关系无权威定义**（`dual-console-permission-design-review.md:35`；feature-list §8 F3.13 备注自认待确认）。**推荐模型落点 = 轨 A**（成员绑定与权限生效均走 `domain_role`/`domain_member_role`），轨 B 冻结为定义层或后续废弃（决策点见 §7）。

### 3.2 平台 F4.9（`pages/platform/role` = re-export `pages/system/role`）与域端 F3.13 同源
- `system/role/index.tsx:44-47`：`routeScope === "platform" ? "platform" : "domain"` 决定权限码前缀与菜单树 scope → **同一页面双控制台复用，已具备「平台创建 domain 角色」雏形**（scope=domain + business 菜单树）。
- 扩展为「一次创建多域」只需在平台端增加**模板层**（新 Tab），实例层仍复用 `domain_role` 创建逻辑。

### 3.3 员工/多域绑定现状（R-B 的成员侧已具备）
- `StaffAccountService.create/update`（`StaffAccountService.java:52-113`）→ `bindDomainMemberships(staffAccountId, businessDomainIds, roleCodes)`（:181-207）：**一次调用即可将同一员工绑定多个域 + 同名域角色**（`ensureDomainMember` 自动建 `domain_member`；`findDomainRoleId(domainId, code)` 要求**每域已存在该 code 的角色**，否则报「业务域角色不存在」）。
- **推论**：R-B 缺的只是「角色创建的多域化」（`DomainRoleController` 逐域建角色），成员侧多域绑定已实现。模板 apply = 批量执行「每域 createRole + 权限包」，随后 `bindDomainMemberships` 即可直接复用。
- `staff_account_platform_role`（`platform_role` 表）承载平台角色绑定，与域角色模型正交。

### 3.4 批量操作现状（R-C 缺后端批量端点）
- 前端批量启停 = **逐条循环**：`detail-members.tsx:667-696` `confirmStatusChange` 对 `selectedRowKeys` 逐个 `updateDomainMemberStatus(domainId, id, nextStatus)`；`detail-customers.tsx:730-744` 同模式。**后端无批量端点**（`DomainMemberController.java:101-111` 仅单条 `PUT /members/{memberId}/status`；`DomainCustomerController.java:92-102` 仅单条 status）。
- 工单批量领取（TR-04 部分成功）是项目内唯一的「批量 + 部分成功」先例（foundation-rules §6.7），R-C API 应复用该语义。
- 单域批量已有先例 UI（rowSelection + 批量按钮），R-C 需扩展为「跨域」：选定员工 → 选定域集 → 批量。

### 3.5 审计承载（批量操作逐域记录可行）
- `audit_log`（`rebaseline:250-267`）含 `business_domain_id`（可空）、`operator_subject_id`、`target`、`action`、`detail`(json)、`result`、`request_id` → **逐域写 N 行可完整记录批量操作**；无 console/入口维度（`dual-console-permission-design-review.md:39`）。
- `AuditLogSemanticsListener.java:34-80` 仅监听 2 类事件（角色权限变更、域成员状态变更）；成员增删/改角色、域角色 CRUD 不落审计（既有缺口，模板/批量操作审计需扩展 listener + `AuditActionCodes`（现有：`platform.role.permissions.update`、`platform.domain.member.update_status` 等））。

### 3.6 US-S1-08 跨域拦截（P0 前置，现状 Todo）
- `PermissionScopePolicy.isPermissionEffective(..., targetBusinessDomainId)`（`PermissionScopePolicy.java:36-60`）与 `IamService.hasPermissionForDomains`（`IamService.java:59-67`，allMatch 语义）**机制已存在**；但 `RequirePermissionInterceptor → hasAnyPermission(context, codes)` 传 `targetBusinessDomainId = null` → 恒通过（`IamService.java:55-57,77-96`）。US-S1-08 状态 Todo（`backlog-stories.md:223-229`，S4+）。
- **对集团管理的放大**：平台模板推送/批量停用是「跨域写」，若不加目标域校验，任何持 `domain.role.*`/`domain.member.*` 的域管理员都可对任意域执行——批量 API 更必须做 `hasPermissionForDomains(operator, code, domainIds)` 逐域校验。

---

## 4. 行业/集团管理对标

| # | 来源 | 要点 | 对 UnionDesk 映射 |
|---|---|---|---|
| 1 | **Atlassian Jira permission scheme**（support.atlassian.com，✅ 已抓取原文） | scheme 定义一次、**应用到多个 project/space**（"Create a new permissions scheme that can be used across multiple spaces"）；scheme 内用 project role 组合授权 | **模型 2/3 最直接先例**：平台建 scheme（=角色模板）→ 多域应用（=apply）；Jira 的 scheme 变更对已应用 project 即时生效 ≈ immediate 同步策略 |
| 2 | **Salesforce Permission Set**（help.salesforce.com，页面 JS 渲染未能抓取，📚 常识） | 用户 = Profile（基线）+ 多个 Permission Set（**叠加授权**）；一个 Permission Set 可分配给任意多用户 | 「一次创建、多处分配」的叠加模式印证 R-B；但 Permission Set 是**用户级叠加**而非域实例，与 UnionDesk「域隔离角色」语义不同，仅作参考 |
| 3 | **Microsoft Entra 动态组 / 组授权**（learn.microsoft.com，✅ 已抓取原文） | 组可承载批量授权；动态组规则基于属性自动增删成员（attribute-based，无法手动增删） | 印证「组 = 批量赋权载体」；动态规则属高级演进，本需求**不做**（见 §6 后置项）；静态组批量赋权 ≈ 角色绑定 |
| 4 | **企业微信/钉钉/飞书 集团与子公司管理员**（📚 行业常识） | 集团管理员可跨子公司下发统一角色/审批流，子公司管理员在集团策略内自治；普遍为「统一框架 + 属地微调」 | 与模型 3 的「统一模板 + 域覆盖 + 同步策略」同构；集团统一建角色下发 vs 子公司自治的平衡正是本需求 |
| 5 | **NIST RBAC / GitHub Enterprise（引用前序调研）**（`dual-console-permission-design-review.md:85-88`，✅ 已抓取） | RBAC 支持集中+委托并存；GitHub 平台默认只读监管、加入组织才获权限 | 方案 D 的理论支撑仍有效；本次需求将「平台角色管理」从只读监管升级为「集中创建+下发」，不违背 RBAC 集中管理形态 |

**关键判据**：业界平衡「统一 vs 差异化」的主流模式 = **统一模板/方案 + 租户/域级覆盖 + 变更同步策略**（Jira scheme、企业微信集团下发、Salesforce Profile+Permission Set 叠加）。无主流产品采用「单一共享角色跨域生效且域内不可差异」的形态（模型 1 弱）。

---

## 5. 推荐模型设计骨架（模型 3：模板 + 实例 + 覆盖 + 同步策略）

### 5.1 数据模型（表名级）

| 变更 | 表 | 说明 |
|---|---|---|
| **新增** | `role_template` | 平台级角色模板：`id, code(唯一), name, description, sync_strategy('immediate'|'manual'|'none'), locked_fields(json, 如 ["permissions"]), preset, created_by, created_at, updated_at` |
| **新增** | `role_template_permission` | 模板权限包：`template_id + permission_item_id`（复用 `permission_item` 目录，与 `domain_role_permission` 同构） |
| **新增** | `role_template_domain` | 模板→域应用关系：`template_id + business_domain_id + instance_domain_role_id + sync_mode + applied_at + updated_at`（唯一键 `(template_id, business_domain_id)`） |
| **扩展** | `domain_role` | 增 `template_id`(可空 FK)、`template_version`(int)、`locked_fields`(json) —— 实例标记来源与锁定；非模板角色保持现状 |
| **不变** | `domain_role_permission` | 实例权限（apply 时批量写入） |
| **不变（可选增列）** | `domain_member_role` | 批量绑定来源可仅靠 audit_log 追溯，不强制加列 |
| **不变** | `staff_account` / `domain_member` | R-A 用户库已就绪；跨域成员视图由 `domain_member` join 提供 |

### 5.2 权限模型
- 模板角色权限码 = `permission_item`（轨 A 目录）；`domain_role` 实例权限 = 继承模板权限包。
- **覆盖规则**：`locked_fields` 内字段（默认锁定「权限包」，可选锁定「名称」）域端不可改（403 + 中文）；非锁定字段域端可改。
- **同步传播**：`immediate` = 模板权限变更自动下发各实例（域覆盖项跳过并记录）；`manual` = 平台手动触发 `sync`，域端可见「落后 N 版本」；`none` = 纯一次性模板（创建后解耦）。
- 新权限码：`platform.role_template.{read,create,update,delete,apply,sync}`、`platform.staff.domain_batch_status`（批量停用）。

### 5.3 批量 API 形态（示例）

```text
# R-B 创建模板 + 推送多域
POST   /api/v1/iam/role-templates                     # body: {code,name,permission_item_ids,locked_fields,sync_strategy}
POST   /api/v1/iam/role-templates/{templateId}/apply  # body: {domain_ids:[...], sync_mode:'immediate'}
# 同步/撤回
POST   /api/v1/iam/role-templates/{templateId}/sync   # body: {domain_ids?:[...]}（manual 模式触发）
POST   /api/v1/iam/role-templates/{templateId}/unapply# body: {domain_ids:[...]}（解绑实例，实例成员校验后转独立）
# R-C 跨域批量停用（TR-04 部分成功语义，逐域审计）
POST   /api/v1/admin/staff/{staffId}/domain-members/batch-status
       # body: {domain_ids:[...], status:'disabled'} → 响应: {success:[domainId...], failed:[{domainId,reason}...]}
# R-B 成员绑定（复用既有能力，可选补充）
POST   /api/v1/iam/role-templates/{templateId}/bind-members
       # body: {staff_ids:[...], domain_ids:[...]}（内部 = 现有 bindDomainMemberships 多域循环）
```

### 5.4 UI 落点
- **F4.9 平台角色页**：新增「模板」Tab（模板列表/创建/推送选域 Modal/同步按钮/漂移状态列）。
- **F3.13 域端角色页 + F3.3 域角色**：实例行展示「模板来源 + 版本」徽标；锁定字段只读禁用；非锁定字段可编辑（保存时后端校验锁定）。
- **F4.7 用户管理 / F4.3 离职池**：行内「跨域批量停用」入口（选员工 → 选域集 → 确认 → 部分成功摘要，复用 `detail-members.tsx` rowSelection 交互模式）。

### 5.5 职责矩阵重定义（一句话版）
> **平台 = 统一管人（员工库）+ 统一管角色（模板创建/下发/同步）+ 跨域批量停用 + 审计监管；域 = 成员日常运营 + 模板实例微调（锁定字段除外）+ 域内业务自治（客户/工单/配置）；两端同源数据可写，冲突以「锁定字段 + 同步策略」裁决，跨域写一律逐域鉴权（US-S1-08）。**

### 5.6 迁移路径（分阶段）

| 阶段 | 内容 | 前置关系 |
|---|---|---|
| **P0 安全债（先行）** | ① 落地 US-S1-08 目标域校验（批量/模板 API 全部经 `hasPermissionForDomains` 逐域校验）；② 审计补齐（成员/域角色写事件 + 批量操作逐域 audit 行 + 新 action code） | 平台跨域写能力的安全底座，**必须先于模板/批量功能上线** |
| **P1 模板层** | `role_template` + `role_template_permission` + `role_template_domain` 建表；平台「模板」Tab（F4.9）；apply/sync/unapply 服务（复用 `DomainRoleService` 实例写入 + `StaffAccountService.bindDomainMemberships`） | 依赖 P0-①（apply 涉及多域写） |
| **P1 批量停用** | `POST /staff/{staffId}/domain-members/batch-status` + F4.7 跨域入口 | 依赖 P0-①（逐域校验）；纯新 API，可并行 |
| **P2 域端微调展示** | F3.13/F3.3 模板来源徽标、锁定字段只读、漂移提示 | 依赖 P1 模板层 |
| **P3（可后置）** | 双轨治理决策落地（`role(scope=domain)`/`iam_role_binding` 冻结或废弃）；`immediate` 异步下发队列；模板版本历史 | 不阻塞 MVP |

---

## 6. 风险与边界

1. **跨域越权（R1）仍为最高前置**：平台批量/模板 API 是「跨域写」放大器；未修 US-S1-08 前上线 = 域管理员可借 `domain.role.*` 向任意域推角色。**P0 硬前置，不可后置**。
2. **模板漂移**：实例可改 ⇒ 漂移必然；以 `template_version` + 漂移状态列 + manual 同步时冲突摘要缓解；`none` 策略承认漂移（一次性模板）。
3. **覆盖冲突**：域覆盖（非锁定字段）与 `immediate` 推送冲突 → 记录并跳过被覆盖项，审计可查；锁定字段被域端修改 → 拒绝。
4. **角色上限冲突**：foundation-rules「每域自定义角色 ≤20」与模板推送冲突——apply 前校验目标域余量；满额域 → 部分成功（TR-04）并提示（注意：当前 `DomainRoleService` 未见数量校验实现，需在模板 apply 与域端创建双入口统一补）。
5. **批量操作审计粒度**：逐域写 `audit_log` 行（每行 `business_domain_id`），`detail` 记录请求域集与逐域结果；不做单事务回滚（TR-04 部分成功）。
6. **双轨治理未决**：IAM `role(scope=domain)` 与 `domain_role` 并存；模板模型落点 = `domain_role`（实例）+ `permission_item`（权限目录）。若产品希望平台角色页统一管理两套，需明确轨 B 处置（决策点）。
7. **明确不做/后置**：动态组规则引擎（Entra 式属性规则，P3+ 或不做）；跨域统一成员视图的实时同步（P3）；模板版本 diff 预览（P3）；`immediate` 异步推送队列（P3，先同步实现）。

---

## 7. 需要用户后续决策的点

1. **模板同步策略默认值**：建议默认 `immediate`（贴合「统一」主诉求），`manual` 作高级选项；`none` 是否提供？（Jira scheme 为即时生效先例）
2. **锁定字段白名单**：默认锁定「权限包」，是否加锁「名称」？「成员」是否允许域端自管（建议域端自管，锁定仅限定义层）？
3. **满额域冲突策略**：模板推送至已达 20 自定义角色上限的域 → 跳过并提示 vs 提高上限（建议先跳过，上限放宽另议）。
4. **双轨处置**：IAM `role(scope=domain)` 冻结/废弃，还是平台角色页（F4.9）改以模板为唯一入口？
5. **批量停用的域集粒度**：用户页面选择域集是否要按「组织/部门」预筛（复用 `platform_organization`/`staff_organization`），还是先手选域列表（建议先手选，预筛 P2）。
6. **批量操作确认门槛**：跨域停用是否要求 step-up 二次认证（参照域删除 F4.1 的先例，建议高危批量操作启用）。

## Caveats / 待确认

- 本次会话无 exa 搜索工具，行业对标用 webfetch 抓取 Atlassian（✅）与 Microsoft Learn（✅）原文；Salesforce Help 页面为 JS 渲染未能抓取（标 📚 常识）。
- 「每域自定义角色 ≤20」为文档口径（foundation-rules §3.2），`DomainRoleService`/`DomainRoleRepository` 未检索到数量校验实现，实现时需确认。
- `iam_role_binding` 为遗留表（FK 已卸，V20260719100446），是否仍参与鉴权未逐一核对 `IamRepository.findEffectiveGrants` 的 SQL；本设计不依赖它。
- 模板 apply 的实例创建拟复用 `DomainRoleService.createRole`（逐域事务），多域批量的事务边界（整批 vs 逐域）建议逐域（TR-04 部分成功）。
