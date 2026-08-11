# Research: 双控制台域内权限管理设计评审（平台端 vs 业务域端重叠）

- **Query**: 「平台端 + 业务域端双控制台均管理域内员工/客户/角色权限」这一设计是否科学可行？有更优方案吗？
- **Scope**: mixed（内部代码/文档取证 + 外部行业对标）
- **Date**: 2026-08-11
- **取证基线**: 文档快照 2026-08-11（PRD/feature-list 状态基准）；代码以当前工作区 HEAD 为准

---

## 1. 现状取证（as-built）

### 1.1 产品文档口径

| 文档 | 位置 | 口径 |
|---|---|---|
| PRD §4.1.3 | `docs/product/prd.md:151-164` | 「平台角色 + 业务域角色」双层权限；单角色 MUST 仅绑一种控制台（`role.scope = global/domain`）；双控制台用户须显式组合绑定（`user_global_role` + `user_domain_role`） |
| foundation-rules §3.1 | `docs/product/foundation-rules.md:160-179` | 平台级（`platform_admin`：创建业务域、全局员工、系统安全与平台菜单）；业务域级（`super_admin`/`domain_admin`/`agent` + 每域≤20 自定义角色） |
| backlog-epics §8.0 | `docs/product/backlog-epics.md:176-187` | S1 主验收面 = 平台端；业务域管理端 UI 归 S2/E2；未定义双控制台对同一数据的重叠规则 |
| data-model §3.2-3.4 | `docs/architecture/data-model.md:54-79` | `domain_member`（员工入域关系）、`domain_member_role`（**域 RBAC 唯一写入路径**）、`staff_account_platform_role`、`role`（含 scope 列）、`iam_role_binding`（统一角色绑定，含 business_domain_id）、`iam_permission`（区分 platform/domain 范围） |

### 1.2 功能清单归属（feature-list.md，2026-08-11 快照）

| 数据面 | 平台端入口（可写） | 业务域端入口（可写） | 权限码（feature-list §8，`feature-list.md:317-372`） |
|---|---|---|---|
| 域成员 | 域详情「成员」Tab（US-S2-03，`feature-list.md:137` F4.1） | `/domain/settings/members`（F3.3，`feature-list.md:170`） | 平台 `platform.domain.control.member.*` vs 域 `domain.member.*` |
| 域客户 | 域详情「客户」Tab（US-S2-04，只读查看/启停/手动添加） | `/domain/customers/list`（F3.5，可编辑/重置密码/导入，`feature-list.md:172`） | 平台 `platform.domain.control.customer.*` vs 域 `domain.customer.*` |
| 域角色 | 域详情「角色」Tab（US-S2-02，**只读**，`feature-list.md:137`） | `/domain/settings/roles` + `/system/role`（F3.13，可写，`feature-list.md:180`） | 平台 `platform.domain.roles.*`（只读） vs 域 `domain.role.*` |
| 平台角色（scope=domain 定义） | `/platform/role`（F4.9，可创建 scope=domain 角色，`feature-list.md:145`） | `/system/role`（F3.13 同 API） | `platform.role.*` / `domain.role.*`（同 IAM API） |

### 1.3 后端实现取证（关键证据链）

1. **同一 API、双权限码（OR 语义）**：成员/客户写接口集中在 `/api/v1/admin/domains/{domainId}/members*`、`/customers*`（`DomainMemberController.java:32-121`、`DomainCustomerController.java:31-119`），注解同时挂 `platform.domain.control.member.*` 与 `domain.member.*` 两族码；`RequirePermissionInterceptor.java:38` 用 `hasAnyPermission`（**任一命中即放行**）。两个控制台调用的是**同一批共享 API**（前端 `@uniondesk/shared` 同一函数，如 `fetchDomainMembersPage`）。
2. **客户编辑/重置密码仅域码**：`PUT /customers/{id}`、`PUT /customers/{id}/password` 只挂 `domain.customer.update/reset_password`（`DomainCustomerController.java:104-119`）→ 平台详情客户 Tab 天然只读资料（与 US-S2-04 一致）。
3. **域角色写路径仅域码 + 平台只读专用 Controller**：`DomainRoleController.java:29-85`（`domain.role.*` 可写）；平台只读走独立 `PlatformDomainRoleController.java:16-38`（`platform.domain.roles.*`）。
4. **角色存在双轨表**：域内角色存 `domain_role` / `domain_role_permission` / `domain_member_role`（`DomainRoleMapper.xml`、`DomainMemberRoleMapper.xml`，每域预置 super_admin/domain_admin/agent + 自定义≤20）；同时 IAM 另有 `role`（scope=domain）+ `iam_role_permission` + `iam_admin_menu`（business）+ `iam_role_binding` 体系，平台角色页（`IamController.java:221-227`，`platform.role.create`）可创建 scope=domain 角色定义、`/system/menu`（F3.14）域端可改 business 菜单。**两套"域角色"的关系在任何文档中均未显式定义**（feature-list §8 F3.13 备注仅写「代码（IAM 域级 API）」）。
5. **权限生效不校验目标域（US-S1-08 未落地）**：拦截器调用 `hasAnyPermission(context, codes)` → `IamService.java:77-96` 中 `targetBusinessDomainId = null` → `PermissionScopePolicy.isPermissionEffective`（`PermissionScopePolicy.java:36-60`）对任意 URL 中的 domainId 返回 true（`targetBusinessDomainId == null || equals`）；service 层 `requireDomain(domainId)`（`DomainMemberService.java:335-337`）只校验域存在、不校验操作者入域关系。**A 域 domain_admin 持 `domain.member.*` 可操作 B 域成员**（inventory §4.5 明确 US-S1-08 = Todo）。
6. **保护规则在共享 service 层、与操作入口无关**：最后 `domain_admin`/`super_admin` 保护、每域唯一 super_admin（`DomainMemberService.java:204-229`）对两端操作同等生效（含平台管理员也不能移除最后 super_admin）。
7. **域端可创建全局员工**：`POST /members/with-staff`（`DomainMemberController.java:77-87`）经 `StaffAccountService.create` 直插 `staff_account`（`DomainMemberService.java:110-133`）——**无需 `platform.user.create`**，域管理员即可向平台员工库写入新员工。
8. **审计覆盖缺口**：`operation_log` 写入仅存在于 `DomainService`（域生命周期）与 `AuditLogSemanticsListener`（`AuditLogSemanticsListener.java:34-80`：仅「角色权限变更」「域成员状态变更」两类事件）。成员增删/改角色、客户增删启停、域角色 CRUD、邀请码操作**均不落 operation_log**。`AuditLogWritePo.java` 字段：businessDomainId / operatorSubjectId / operatorActorType / target / action / detail / result / requestId —— **无 console/入口维度**，日志本身无法区分「平台端操作」还是「域端操作」（仅能靠 operator 身份反查角色快照）。

### 1.4 前端双入口取证

- 平台域详情：`detail-members.tsx:789-869`（可写，码 = `platform.domain.control.member.*`，注意 `platform-domain-permissions.ts:61-65` 将 `DOMAIN_MEMBER_*` 作为别名指向平台码）、`detail-customers.tsx:664-849`、`detail-roles.tsx:139-188`（只读）。
- 域端：`/domain/settings/members`（`domain/members/index.tsx`，码 `domain.member.*`）、`/domain/customers/list`、`/domain/settings/roles`（`domain/roles/index.tsx` → 调 `fetchDomainRoles` 即 `/roles`）、`/system/role`（F3.13，IAM `/api/v1/iam/roles`）。
- 路由门控：`router/routes/modules/domain.ts:81-200` 全部挂 `domain.*` 码。

### 1.5 结论性事实

双控制台重叠是**结构性重叠**：同源数据（`domain_member`/`domain_customer`/`domain_role`）+ 同一 API + 同一 service，仅入口 UI 与权限码不同（OR 放行）。角色面额外存在「domain_role 表」与「IAM role(scope=domain)」双轨。

---

## 2. 重叠风险分析（Top 风险，含证据）

### R1【最高】跨域越权：域作用域权限不校验目标域
- 证据：`RequirePermissionInterceptor.java:38` → `IamService.hasAnyPermission(..., null)`（`IamService.java:55-57`、`77-96`）；`PermissionScopePolicy.isPermissionEffective` 对 `targetBusinessDomainId == null` 恒通过（`PermissionScopePolicy.java:57`）；`DomainMemberService.requireDomain` 仅查域存在（`DomainMemberService.java:335-337`）。US-S1-08 状态 Todo（`implementation-inventory.md:120`、`backlog-stories.md:223-227`）。
- 影响：持有任一 `domain.*` 写码（如 A 域 domain_admin）可对 B 域成员/客户/角色调用同一 API 写入。**双入口 API 复用使该缺口同时暴露给两个控制台**。

### R2 成员/客户双写并发无冲突控制
- 证据：改角色 = `replaceMemberRoles` 直接 delete+insert（`DomainMemberService.java:238-247`），启停/改角色均无 version/乐观锁（对比工单有 TR-04 version 部分成功机制）；两端操作者并发时 last-write-wins，无冲突提示。
- 影响：平台管理员与域管理员同时调整同一成员角色/状态，可能互相覆盖（保护规则只在"移除最后管理员"等硬约束上兜底）。

### R3 角色双轨未治理（语义撕裂）
- 证据：`domain_role` 族（DomainRoleMapper.xml）vs `role(scope=domain)`+`iam_role_permission`+`iam_admin_menu(business)`（`IamController.java:221-266`、F3.13/F3.14/F4.9）；平台详情角色 Tab 只读（PlatformDomainRoleController）而平台角色页却可建 scope=domain 角色定义——「看得见一套、建得了另一套」。
- 影响：域角色"谁在管、管哪套、两者什么关系"无权威口径，Flyway/文档均未定义二者映射（feature-list §8 备注自认待确认）。

### R4 审计追溯缺口
- 证据：`AuditLogSemanticsListener.java:34-80` 仅覆盖 2 类事件；成员/客户/域角色写操作无 operation_log 写入；`AuditLogWritePo` 无 console/入口字段。
- 影响：双入口的「哪一端改了什么」无法从日志还原；与 foundation-rules §3.5「管理操作、权限变更写入 audit_log 不可删除」不符（部分管理操作未写）。

### R5 权限边界模糊点
- 平台管理员：可改任意域成员/客户（`platform.domain.control.*` 全局生效），但不能写域角色实例（无 `domain.role.*`）→ 中间态；
- 域管理员：可经 `with-staff` 创建全局员工账号（`DomainMemberService.java:110-133`），绕开平台员工入职治理（组织归属等）——属于平台"员工库"边界的隐性旁路；
- 域内最后 super_admin 保护对平台管理员同样生效（service 层共享），阻断"平台替域兜底"场景（需域内自行转让）。

### R6 职责/语义混淆（产品层）
- 平台「员工」vs 域「成员」同一实体（staff_account/domain_member），文档口径分处 PRD §5.3/§5.4 与 F3.3/F4.7，代码无区分；US-S2-02/03/04 与 F3.3/F3.5/F3.13 构成对同一批数据的双入口写面，产品未定义「默认谁管、冲突谁赢、平台介入条件」。

---

## 3. 行业对标

| # | 来源（已抓取原文 ✅ / 常识性惯例 📚） | 要点 | 对 UnionDesk 的映射 |
|---|---|---|---|
| 1 | NIST RBAC / ANSI INCITS 359-2012（csrc.nist.gov RBAC Overview + FAQ，✅ 抓取） | RBAC 四组件（Core/Hierarchical/SSD/DSD）；角色是权限集合、用户仅经角色获得权限；**标准管理功能规范同时支持集中管理与委托（分散）管理两种管理形态**；模型允许在访问时附加额外约束（如基于关系的约束） | UnionDesk 已实现 Core RBAC + 类 SSD 约束（最后管理员保护、每域唯一 super_admin、scope 校验 US-S3-00）；「双入口重叠」本身不违反 RBAC，但**"目标域校验"属于标准允许的访问时附加约束，当前缺失**（R1） |
| 2 | GitHub Enterprise / Organization 角色体系（docs.github.com，✅ 抓取两页） | ① Enterprise owner **默认不进入组织设置/内容**，须加入该组织才获得组织级权限；② 组织 owner = 租户内委托管理员，管理成员/角色/仓库；③ Enterprise 自定义角色可做细粒度委托（如仅"查看组织审计日志"）；④ 组织与 enterprise 各有独立审计日志 | 业界主流 = **平台（provider）默认只读监管 + 租户（域）自治**；平台需要干预时通过"加入"或"自定义委托角色"，而非默认双写。对应本文方案 D |
| 3 | ARBAC / RBAC96 管理委托模型（Sandhu 等 1996-1999，📚 学术惯例，未抓取原文） | 管理角色（admin roles）与范围（ranges）支持将管理权委托给下层管理员，是分散管理的理论基础 | 「平台保留 break-glass 管理角色、日常委托域管理员」有成熟理论支撑 |
| 4 | 多租户 SaaS 惯例：Salesforce Profile/Permission Set、企业微信/钉钉集团 vs 子公司管理员、AWS SaaS 租户隔离（📚 行业惯例，未抓取原文） | Provider admin 管租户生命周期/全局策略/计费；Tenant admin 管租户内用户/角色/配置；**平台一般不直接改租户内部数据**，仅保留监管与紧急介入（break-glass），且介入须审计 | 与方案 D 一致；平台端写入口应定位为"异常介入"，默认收敛 |

**关键判据（重叠何时可接受）**：同源数据（避免双副本漂移）+ 权限码隔离（避免越权）+ 完整审计（可追溯）+ 明确的委托模型（谁是日常管理者、平台何时介入）。现状满足前两条，**审计与委托模型两条不满足**（R3/R4/R6）。

---

## 4. 替代方案对比

| 方案 | 产品合理性 | 实现成本（对照现有代码/Story） | 风险 | 迁移路径 |
|---|---|---|---|---|
| **A 现状**（双入口分权、同源数据） | 中：跨域治理便利，但日常双写入口语义模糊、职责重叠 | 0（已实现） | 高：R1 跨域越权、R4 审计缺口、R3 双轨 | — |
| **B 平台收敛**（平台域详情只读/监管 + 员工库；域内成员/客户/角色域端自管） | 高：符合域自治定位（foundation-rules §3.1、PRD §5.3） | 中：`detail-members.tsx`/`detail-customers.tsx` 写按钮收敛（复用 `detail-roles.tsx` 只读模式）；Flyway 收敛默认角色绑定；后端 API 可保留 | 低；与 US-S2-03/04 已签 off 的写能力口径冲突，需文档回退说明 | 最短：前端只读化 + 默认权限收敛 |
| **C 域端收敛**（全部平台管，域端只读） | 低：与"域自治"产品定位直接冲突（PRD §5.3 域管理后台 = 域管理员配置本域人员） | 高：域端 F3.3/F3.5/F3.13 已实现可写，全部回退 | 高：域管理员失去自治能力 | 不推荐 |
| **D 分层委托（推荐）**：平台 = 员工库 + 平台角色 + 审计监管 + break-glass；域内成员/客户/角色域端全权；平台域详情 Tab 只读或仅高危操作；`platform.domain.control.*` 保留但默认不授予 `platform_admin` | 高：与 GitHub Enterprise/多租户 SaaS 惯例一致；重叠降为"监管只读 + 紧急介入" | 中：骨架已存在（平台角色只读已实现、权限码已存在）→ ① 修复 US-S1-08（`IamService.hasAnyPermission` 已带 `targetBusinessDomainId` 参数，扩展 Controller/切面传 URL domainId）② 补齐审计事件（扩展 `AuditLogSemanticsListener`）③ Flyway 收敛默认授权 ④ 前端写按钮收敛 ⑤ 文档口径对齐 | 低 | 分 3 步：先修 R1+R4（安全债）→ 再收敛默认权限 → 最后前端只读化 |
| **E 增强形态**（委托范围配置、角色继承/权限叠加、乐观锁） | 高（作为 D 的补充） | 高（新机制） | 中 | 列为 P1 演进，不阻塞 MVP |

---

## 5. 结论建议

### 5.1 现状判定：**框架科学、实现欠账、语义需收敛**

- **科学的部分**：同源数据 + 权限码隔离 + 共享 service 层保护规则，符合 RBAC 分层管理（集中/委托并存合法）；"角色—控制台绑定"（US-S3-00 scope 校验）与最后管理员保护属正确约束。
- **不科学/欠账的部分**（按严重度）：
  1. **R1 跨域拦截缺失**（US-S1-08 未落地）——域作用域权限可被用于任意域，这是安全漏洞级缺陷，与双入口设计无直接关系但被其放大；
  2. **R4 审计缺口**——多数双入口写操作不落 operation_log，且日志无 console 维度，"双端可追溯"名不副实；
  3. **R3 角色双轨**——domain_role 与 IAM role(scope=domain) 两套体系并行且关系未定义；
  4. **R6 产品语义**——"平台日常可写成员/客户"与"域自治"定位冲突，业界惯例是平台默认只读监管。

### 5.2 推荐：**方案 D（分层委托）为演进目标，方案 A 为过渡载体**

理由：D 不推翻现有架构（API/表/权限码全部复用），只需做"收敛"而非"重构"；与 GitHub Enterprise 与多租户 SaaS 惯例一致；消除 R2/R3/R4/R6 的大部分暴露面，R1 无论选哪个方案都必须修。

**最低成本路径（按序）**：
1. 修复 US-S1-08：域作用域权限按 `{domainId}` 目标域校验（`IamService.hasPermissionForDomains` 已存在；给 `DomainMemberController/DomainCustomerController/DomainRoleController` 增加目标域校验或复用 service 层校验）；
2. 补齐审计：成员/客户/域角色写操作发布事件 → `AuditLogSemanticsListener` 落 operation_log（复用 `AuditActionCodes`）；
3. 收敛默认授权：Flyway 将 `platform.domain.control.member/customer` 写码从 `platform_admin` 默认绑定中移除（保留码定义与 API），仅授给 break-glass 角色；
4. 平台域详情 Tab 写按钮只读化/隐藏（对齐 `detail-roles.tsx` 模式）；
5. 文档口径对齐（见 5.3）。

### 5.3 若维持现状（方案 A）须补充的文档清单

| 文件 | 条目 | 内容 |
|---|---|---|
| `docs/product/foundation-rules.md` | §3.1 新增 FR-07 | 双控制台写入口边界：成员/客户/角色日常管理以域端为准；平台域详情写权限定位为监管/紧急介入（break-glass），默认不授予 `platform_admin`；两端操作同源数据，冲突以 service 层保护规则为准 |
| `docs/product/prd.md` | §4.1.3 增补 | 「成员/客户/角色」读写职责矩阵（平台：只读 + 异常介入；域端：日常读写）；明确 `platform.domain.control.*` vs `domain.*` 的授予建议 |
| `docs/product/feature-list.md` | F3.3/F3.5/F3.13/F4.1 备注 + §8 | 标注「与平台域详情 Tab 同源双入口（API 复用，权限码 OR）」；§8 补充 `domain_role`（域内角色表）与 `role(scope=domain)`（IAM）双轨关系说明 |
| `docs/product/backlog-epics.md` | §8.0 增补 | 「重叠可接受条件」：同源数据 + 权限码隔离 + 审计可追溯 + 委托模型；写明 US-S1-08 为前置安全依赖 |
| `docs/architecture/data-model.md` | §3.3-3.4 | 明确 domain_role 与 role(scope=domain) 的关系与各自管理入口 |
| `backlog-stories.md` | US-S1-08 | 从 Stretch 提级为安全修复 Story（若维持现状为最高优先） |

---

## Caveats / 待确认

- 审计覆盖断言基于代码检索（`AuditLogWriter` 引用仅出现在 `DomainService` 与 `AuditLogSemanticsListener`）；不排除其他写入路径（如 AOP/切面，本仓库未发现 `@Aspect`）。
- `domain_role` 与 `role(scope=domain)` 双轨的**产品意图**未在任何文档定义，二者是否应合并/映射需产品决策（代码层面确为两套表、两套 API）。
- `operation_log` 是否另有 client_code 类字段未逐一核对 Flyway DDL（`AuditLogWritePo` 为准，无 console 维度）。
- 外部对标中 ARBAC 与多租户 SaaS 惯例为学术/行业常识性来源（未抓取原文）；NIST RBAC 与 GitHub 官方文档已抓取原文。
