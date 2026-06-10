# Product Backlog — User Stories

| 文档版本 | 日期 | 说明 |
|:---|:---|:---|
| 1.8 | 2026-05-26 | S2 Story 细化：软删/权限码（01/02/05/06/E2-00）；US-S2-02 改为只读「角色管理」 |

> Epic 见 [`backlog-epics.md`](./backlog-epics.md)。DB 增量见 [`../architecture/database-increment-plan.md`](../architecture/database-increment-plan.md)。  
> **迭代任务源**：本文档（AGENTS.md 约定）。

---

## 估算基准

| SP | 含义 |
|:---|:---|
| 1 | 文档 / 配置，≤ 半天 |
| 2 | 单模块或单 API + 单测 |
| 3 | 前后端联调一条路径 |
| 5 | 跨层特性（DB + API + UI + 测试） |
| 8 | 多模块 / 状态机级 |
| 13 | 须拆 Epic，禁止直接进 Sprint |

---

## Sprint 0 — 管理端奠基（E0）

### US-S0-01 管理端实现盘点

- **Epic**: E0 | **Sprint**: S0 | **SP**: 2 | **状态**: Done
- **角色**: 项目经理 / 开发
- **故事**: 作为维护者，我希望有一份管理端菜单/路由/API/规则对照表，以便识别骨架缺口。
- **AC**:
  1. 存在 [`implementation-inventory.md`](./implementation-inventory.md)
  2. 覆盖平台首页、组织、业务域、IAM、日志审计模块
  3. 每项标注 Done / Partial / Todo
- **DB 增量**: 无

### US-S0-02 Backlog 骨架

- **Epic**: E0 | **Sprint**: S0 | **SP**: 2 | **状态**: Done
- **AC**:
  1. `backlog-epics.md` 与本文档存在
  2. S0–S2 Story 有 SP、AC、Epic
  3. 已实现能力标记 Done/Partial 并备注代码位置（以 Git HEAD + 联调库为准）
- **DB 增量**: 无

### US-S0-03 文档权威链

- **Epic**: E0 | **Sprint**: S0 | **SP**: 2 | **状态**: Done
- **AC**:
  1. [`docs/README.md`](../README.md) L5/L6 指向有效文件
  2. 根 [`README.md`](../../README.md) 权威链指向 `docs/`
  3. 注明开发环境已部署、勿重复 docker-compose
- **DB 增量**: 无

### US-S0-04 联调环境说明

- **Epic**: E0 | **Sprint**: S0 | **SP**: 1 | **状态**: Done
- **AC**:
  1. [`sprint-0-plan.md`](./sprint-0-plan.md) §联调环境含 MySQL/Redis/MinIO/后端/AdminWeb
  2. 库名与 `application.yml` 一致（uniondesk）
  3. 敏感凭据不提交 Git 明文
- **DB 增量**: 无

### US-S0-05 increment-plan 与口径对齐

- **Epic**: E0 | **Sprint**: S0 | **SP**: 1 | **状态**: Done
- **AC**:
  1. [`database-increment-plan.md`](../architecture/database-increment-plan.md) 含 L3/L4/L5/L7 分工
  2. 已登记 V202605250001 等历史增量
  3. foundation §2.2.4 / §4.2、data-model §1 定位已对齐
- **DB 增量**: 无（文档）

### US-S0-06 外部依赖 ADR

- **Epic**: E0 | **Sprint**: S0 | **SP**: 2 | **状态**: Done
- **AC**:
  1. [`adr-external-dependencies.md`](../architecture/adr-external-dependencies.md) 含 Formily / MinIO / SMTP 决策
  2. 每项有 MVP 默认方案与降级路径
- **DB 增量**: 无

### US-S0-07 数据库基线快照与迁移备份

- **Epic**: E0 | **Sprint**: S0 | **SP**: 2 | **状态**: Done
- **角色**: 开发 / DBA
- **故事**: 作为维护者，我希望以联调库当前状态为权威基线并保留迁移前备份，以便迁移出问题时可恢复，且不必重排 Flyway 历史。
- **AC**:
  1. **迁移前备份**：文档化并在执行联调库 Flyway/DDL 前运行备份；产出路径 `UnionDesk/backups/uniondesk_YYYYMMDD_HHmmss.sql`，且目录已加入 `.gitignore`。
  2. **参考快照**：从联调库导出 schema + 稳定种子至 **非 Flyway 路径** `docs/architecture/reference-schema/uniondesk_baseline_YYYYMMDD.sql`；文件头注明来源库、Flyway 版本、volatile 表排除说明。
  3. **版本对齐**：联调库 `flyway_schema_history` 最大 version **≥ `202605250001`**，且与 `current/` 脚本一致（无 orphan / missing）。
  4. **drift 登记**：[`database-increment-plan.md`](../architecture/database-increment-plan.md) §2 与快照一致；若有 drift 写入备注。
  5. **明确不做**：不 `flyway:clean` 联调库；不移动 `current/` → `archive/`；不修改已执行迁移的版本号或内容。
- **DB 增量**: 无（只读导出 + 文档）
- **规则**: 备份文件、含凭据脚本 **禁止 commit**
- **备注**: 全库备份 `UnionDesk/backups/uniondesk_*.sql`（不入 Git）；参考快照 [`reference-schema/uniondesk_baseline_20260525.sql`](../architecture/reference-schema/uniondesk_baseline_20260525.sql)；脚本 `backup-db` / `export-baseline-reference`（`.ps1` + `.sh`）

---

## Sprint 1 — 员工平台端（E1，`/platform/`）— **已签 off（2026-05-26）**

> **Sprint 执行计划**：[`sprint-1-plan.md`](./sprint-1-plan.md)（Committed 顺序、P0 工程、DoD、**§11 签 off**）。  
> **暂缓 Story**（不纳入 S1 完成度）：US-S1-04、US-S1-05、US-S1-08。

### S1-00 联调工程（P0，非功能 Story）

> 不计入 E1 业务 SP 合计；验收见 sprint-1-plan §4～§5。评审通过后先于 US-S1-03 编码。

#### S1-00a JRebel Maven 热更新

- **Sprint**: S1 | **SP**: 1 | **状态**: Done
- **AC**:
  1. `UnionDesk/pom.xml` 含 `jrebel-maven-plugin` 1.2.1，`process-resources` 生成 `rebel.xml`
  2. 文档化无 IDEA 启动：`mvnw jrebel:generate` / `spring-boot:run` + JRebel agentpath
  3. 修改 `@RestController` 后联调可热更（Flyway/DDL 变更除外）
- **DB 增量**: 无
- **备注**: commit `3ebfc60`；Agent 2026.2.1 @ `C:\jrebel`；`scripts/run-with-jrebel.ps1`

#### S1-00b 消除 Security 默认密码日志

- **Sprint**: S1 | **SP**: 0.5 | **状态**: Done
- **AC**:
  1. 启动日志无 `Using generated security password`
  2. JWT 登录、403 中文行为不变；`JwtAuthenticationFilterTests` 通过
- **DB 增量**: 无

### Sprint 1 范围说明

- **S1 Epic E1 = 员工平台管理端**（PRD §3.4）：路由以 **`/platform/`** 为前缀；菜单 `iam_admin_menu.scope = platform`；典型角色 `platform_admin`。
- **业务域管理端**（PRD §3.3，根级非 `/platform/`，`scope = business`）**不在 S1 Committed**，归 **S2 / Epic E2**（见 [`backlog-epics.md`](./backlog-epics.md)）。
- 同一 `UnionDeskAdminWeb` 内可存在双 scope 菜单，但 **S1 UI 收口对象 = 平台端**；`/system/*` 等 business 骨架页成品化不计入 S1 完成度。
- **FR-03**（[`foundation-rules.md`](./foundation-rules.md) §5.1）：无按钮权限 → 按钮不可见；若仍调用 API → **403 + 中文**。

| Story | 交付面 | S1 状态 | 标注 |
|:---|:---|:---|:---|
| US-S1-01 | 平台端 `/platform/*` | Done | 随主路径验收 |
| US-S1-02 | 平台端 `/platform/*` | Done | 随主路径验收 |
| US-S1-03 | 平台端 `/platform/*` | Done | Committed |
| US-S1-07 | 平台端 `/platform/*` | Done | Committed |
| US-S1-09 | 平台端 `/platform/*` | Done | 随主路径验收 |
| US-S1-06 | API + 平台域上下文 | Done | 详情「客户管理」Tab + manual/from-staff（2026-05-30） |
| US-S1-04 | 后端 API | Todo | **S1 暂缓**（见 Story 备注） |
| US-S1-05 | CustomerWeb | Todo | **S1 暂缓**（见 Story 备注） |
| US-S1-08 | 后端鉴权 | Todo | **S1 暂缓**（见 Story 备注） |

### US-S1-01 平台登录与动态菜单

- **Epic**: E1 | **Sprint**: S1 | **SP**: 3 | **状态**: Done
- **角色**: 平台管理员
- **故事**: 作为平台管理员，我希望登录后看到与我权限匹配的菜单，以便进入各管理模块。
- **AC**:
  1. 登录成功进入 `/platform/home`
  2. 菜单来自 `iam_admin_menu`，与 Flyway 种子一致（V202605220001 五模块）
  3. 无权限菜单不可见（FR-03：菜单可见性部分）
- **规则**: FR-03（菜单）；API 403 + 中文见 FR-01 / FR-03
- **备注**: `resolveHomePathFromMenus` + `platformAccess` 优先；动态路由 `generateRoutesFromBackend`；按钮级 FR-03 随 **US-S1-07** 验收（2026-05-30 closure-tracker §3）

### US-S1-02 创建业务域与 bootstrap

- **Epic**: E1 | **Sprint**: S1 | **SP**: 5 | **状态**: Done
- **AC**:
  1. `POST /api/v1/admin/domains` 创建域
  2. 创建者获得域内 `super_admin`（产品称「所有人」；每域唯一；`DomainBootstrapService`）
  3. 管理端列表/详情可访问
- **DB 增量**: V202605320001（super_admin 展示名、全量 `permission_item`、`iam_role_binding` backfill）
- **备注**: 列表 `pages/platform/domains/index.tsx`、详情 `detail/index.tsx` 可访问；bootstrap 含 `domain_member_role` + `user_domain_role` + 域 scope `iam_role_binding`；双字段 UI 见 **US-S1-03**（2026-05-30）

### US-S1-03 入域双配置 CRUD

- **Epic**: E1 | **Sprint**: S1 | **SP**: 3 | **状态**: Done
- **AC**:
  1. API 返回 `registration_enabled` / `invitation_enabled`
  2. 管理端创建/编辑/详情展示双配置
  3. 库中无 `registration_policy` 列
- **规则**: DR-01、DR-02（API 层）
- **DB 增量**: V202605250001（已入库；联调库列态待 MySQL 复验）
- **备注**: AdminWeb：`domains-modal` 向导 + `detail-onboarding`；与 [`implementation-inventory.md`](./implementation-inventory.md) §3 已同步（2026-05-30）

### US-S1-04 客户注册 API

- **Epic**: E1（产品归属 **E3** 客户端入域）| **Sprint**: S1（**暂缓，不纳入 S1 Committed 签 off**）| **SP**: 3 | **状态**: Todo
- **AC**:
  1. 注册 API 校验双字段，中文错误提示
  2. 邀请码创建/使用校验 `invitation_enabled`
- **规则**: DR-01、DR-02
- **备注**（2026-05-30）：团队决定 **S1 不验收** 本 Story 完整 AC；双字段基础已随 US-S1-03（`DomainAccessPolicy`）对齐；与 **US-S1-05** 一并后续 Sprint（建议 E3）再排；不阻塞 S1 平台端签 off。

### US-S1-05 CustomerWeb 接真实注册/入域 API

- **Epic**: E1（产品归属 **E3** 客户端）| **Sprint**: S1（**暂缓，不纳入 S1 Committed 签 off**）| **SP**: 5 | **状态**: Todo
- **角色**: 客户
- **故事**: 作为客户，我希望在客户端用真实 API 注册并加入业务域，而不是 demo portal。
- **AC**:
  1. 登录/注册页调用后端 `/api/v1/auth/register`
  2. 域下拉仅展示 `registration_enabled=allowed`
  3. 错误提示为中文
- **规则**: DR-01
- **备注**（2026-05-26）：团队决定 **S1 不处理 CustomerWeb**；Story 保留 backlog，后续 Sprint（建议 E3）再排；与 **US-S1-04** 一并处理客户端入域路径，不阻塞 S1 平台端签 off。

### US-S1-06 域内客户手动添加

- **Epic**: E1 | **Sprint**: S1 | **SP**: 2 | **状态**: Done
- **AC**:
  1. 有权限管理员可添加客户，状态 active
  2. 不受注册/邀请开关限制
- **规则**: DR-04
- **备注**（2026-05-30，对齐 [`implementation-inventory.md`](./implementation-inventory.md) §3）：
  - **Done（后端）**：`POST /api/v1/admin/domains/{id}/customers`（`DomainCustomerController`）；鉴权 `domain.customer.create`；`DomainCustomerService.addCustomer` 固定 `status=active`、`source=manual`；**不**校验 `registration_enabled` / `invitation_enabled`（DR-04）。
  - **Done（前端 / shared）**：[`detail-customers.tsx`](UnionDeskWeb/apps/UnionDeskAdminWeb/src/pages/platform/domains/detail/components/detail-customers.tsx) 对齐 HTML 演示；权限码 `platform.domain.customer.*`；`POST manual` / `from-staff`；Flyway `V202605330002`。

### US-S1-07 IAM 角色/权限/按钮

- **Epic**: E1 | **Sprint**: S1 | **SP**: 5 | **状态**: Done
- **AC**:
  1. 角色/权限/菜单管理页可演示
  2. 未授权 API 403 + 中文（FR-01）
  3. 未授权按钮不可见（FR-03）；若仍调用对应 API → 403 + 中文
- **规则**: FR-01、FR-03
- **备注**: 平台端 `/platform/system/menu`、`/platform/role`（重定向自 `/platform/permission`）；`AuthGuarded` + `iam_admin_menu` 按钮权限；权限码 catalog 见 Flyway `V202605310002`（2026-05-30 closure-tracker §2）

### US-S1-08 跨域访问拒绝

- **Epic**: E1 | **Sprint**: S1（**暂缓，不纳入 S1 Committed 签 off**）| **SP**: 3 | **状态**: Todo
- **AC**:
  1. A 域身份访问 B 域数据返回 403 或空集
- **规则**: FR-02
- **备注**（2026-05-26）：团队决定 **S1 不做** 本 Story，后续迭代再处理；`implementation-inventory` §4.5 现状仍有效，签 off 不阻塞。

### US-S1-09 登录日志与操作日志（管理端）

- **Epic**: E1 / E5 | **Sprint**: S1 | **SP**: 3 | **状态**: Done
- **AC**:
  1. `/platform/log/login-log` 列表可读
  2. `/platform/log/operation-log` 列表可读
- **备注**: 菜单 V202605210001；独立页与 `pages/platform/audit-logs/` Tabs 页功能重叠（inventory §5 建议 S2 收敛）；列表分页与筛选可用（2026-05-30）

---

## Sprint 2 — E2 业务域端 + 平台域详情深化 + 体验（Committed）

> **Sprint 执行计划**：[`sprint-2-plan.md`](./sprint-2-plan.md)（§2 主表，约 **25 SP**）。  
> **范围**：**E2**（US-S2-E2-00）+ **平台端业务域详情超额**（US-S2-01～06，路径 `/platform/domains/*`）+ **US-S2-UX-01**（E6，纳入 S2 签 off）。  
> **合计约 22 SP**（US-S2-02 只读降为 2 SP）。

### Committed 汇总

| ID | SP | 类型 | 状态 |
|:---|:---|:---|:---|
| US-S2-UX-01 | 2 | E6 横切 | Todo |
| US-S2-01 | 3 | 平台域超额 | Todo |
| US-S2-02 | 2 | 平台域超额 | Todo |
| US-S2-03 | 5 | 平台域超额 | Done |
| US-S2-04 | 2 | 平台域超额 | Done |
| US-S2-05 | 3 | 平台域超额 | Done |
| US-S2-06 | 2 | 平台域超额 | Done |
| US-S2-E2-00 | 3 | E2 主路径 | Todo |

**Stretch（不纳入 S2 签 off）**：US-S2-E2-01 工单类型设计；US-S1-08 跨域拦截。

### US-S2-UX-01 登录滑块验证体验优化

- **Epic**: E6（横切 / 认证体验）| **Sprint**: S2 | **SP**: 2 | **状态**: Todo
- **角色**: 员工 / 客户（登录页）
- **故事**: 作为登录用户，我希望滑块验证按住有清晰反馈、滑到终点可自然松手，以便完成验证时不困惑。
- **AC**:
  1. **按住反馈**：指针按下滑块按钮时，按钮视觉放大；松开且未进入成功态时恢复默认尺寸。
  2. **终点松手**：滑动至最右侧（≥95%）后松开，无「粘住 / 无法脱手」感；松手后进入校验或成功态过渡自然，无多余回弹到起点（除非校验失败）。
  3. **顿挫感**：拖动过程中滑块 `left` 无多余 CSS 过渡；终点松手到成功 / 失败反馈之间无明显卡顿（允许校验 loading，需有即时视觉反馈）。
  4. **范围**：改动 shared [`SliderCaptcha`](../../UnionDeskWeb/packages/shared/src/components/SliderCaptcha/) + AdminWeb [`LoginCaptcha`](../../UnionDeskWeb/apps/UnionDeskAdminWeb/src/pages/login/components/login-captcha.tsx) 联调通过；`typecheck` 通过。
- **规则**: 无（纯前端交互）
- **DB 增量**: 无
- **备注**: 规格见 [`sprint-2-plan.md`](./sprint-2-plan.md) §4；**S2 Committed**；CustomerWeb 当前未接入

### US-S2-01 业务域基础信息与安全删除

- **Epic**: E1（平台域超额）| **Sprint**: S2 | **SP**: 3 | **状态**: Todo
- **角色**: 平台管理员
- **故事**: 作为平台管理员，我需要在业务域详情中查看并更新基础信息，并能通过安全流程软删除业务域，且已删域不在列表出现。
- **AC**:
  1. 详情「基础信息」Tab 正确回显 `code`/`name`/`logo`/`description`/`portal` 等；保存后列表与详情一致。
  2. 软删除前弹窗要求输入与 **`domain.code` 完全一致** 的确认文案；通过后进入 **Step-up** 二次认证再调用删除 API。
  3. 业务域列表 **不展示** `deleted_at IS NOT NULL` 的记录。
  4. ~~已删域访问详情 URL 时跳转列表或展示不可访问~~ — **延后**（S2+；依赖 `getDomain` 对已删域行为与详情门控，基础能力未齐，不阻塞 S2-01 其余 AC）。
  5. **删除写入**：`DELETE` 执行时更新 **`updated_at`**、**`updated_by`**（与操作人一致）；设置 **`deleted_at`**；**不新增** `deleted` 布尔列——是否已删以 **`deleted_at IS NOT NULL`** 为准；**不修改** `status`（`status` 仅表示启用/禁用）。
  6. **权限**（控制台，Flyway `202605330004`～`202605330005` + `RequirePermission` + 菜单按钮）：
     - 进入控制台 / 详情 GET：`platform.domain.control.entry`
     - 概览 Tab：`platform.domain.control.overview`
     - 更新基础信息（PUT）：`platform.domain.control.general.update`
     - 删除业务域（DELETE）：`platform.domain.control.general.delete`
     - 列表「进入控制台」按钮（UI）：`platform.domain.control.read`
     - 无权限时删除/更新入口 disabled + 提示（非隐藏）
- **规则**: `code` 创建后不可改；**`status`** 表示启用/禁用；**`deleted_at`** 表示删除（对用户文案称「删除」，无 `deleted` 列）。
- **DB 增量**: 权限迁移见 Flyway `202605330004`、`202605330005`；**无** `deleted` 列
- **备注**: `detail-baseinfo.tsx`、`detail-header.tsx`、`DomainService.deleteDomain`

### US-S2-02 角色管理（只读）

- **Epic**: E1（平台域超额）| **Sprint**: S2 | **SP**: 2 | **状态**: Todo
- **角色**: 平台管理员
- **故事**: 作为平台管理员，我需要在业务域详情中查看域内角色列表与权限信息，**本次不提供**创建、编辑、删除。
- **AC**:
  1. 详情侧栏与 Tab 文案统一为 **「角色管理」**（非「角色权限」等混用）。
  2. 展示域角色列表（含预设/自定义标识、编码、名称等）；**无** 创建/编辑/删除/权限树保存入口（移除或隐藏「功能开发中」类误导按钮）。
  3. 可选：只读查看某角色已分配权限（调用 `GET .../roles/{id}/permissions`），**不可** `PUT` 保存。
  4. **权限码**（`AuthGuarded` / 后端）：**`platform.domain.roles.*`**（至少 `platform.domain.roles.read`）；与旧 `domain.role.*` 迁移策略在 Flyway 中一并处理。
- **规则**: 角色变更仍通过其它流程（本 Story **不做** CRUD）。
- **DB 增量**: `platform.domain.roles.*` 权限码 + 菜单按钮（Flyway）
- **备注**: `detail-roles.tsx`；后端 `DomainRoleController` 写接口本 Story **不验收**

### US-S2-03 域内员工管理

- **Epic**: E1（平台域超额）| **Sprint**: S2 | **SP**: 5 | **状态**: Done
- **角色**: 平台管理员
- **故事**: 作为平台管理员，我需要在业务域详情中添加员工、调整角色，并禁用或启用域内成员。
- **AC**:
  1. 「员工管理」Tab 支持从平台员工（`staff_account`）选择添加成员并分配角色。
  2. 支持修改成员角色、移除成员（软删）；遵守最后 `domain_admin` / `super_admin` 保护规则。
  3. 支持 **禁用/启用** 成员（新增 `PUT .../members/{memberId}/status` 或等价 API，`disabled`/`active`）。
  4. `shared` 封装成员相关 API；权限 Flyway 含 `domain.member.*` 按钮。
- **规则**: 同一员工在同一域不可重复添加。
- **DB 增量**: 无表变更（`domain_member.status` 已存在）；权限码 Flyway 按需
- **备注**: `detail-members.tsx` 可交互；Flyway `202606060001`；`PUT .../members/{id}/status`、`POST .../with-staff`、`GET .../staff-candidates`

### US-S2-04 域内客户管理完善

- **Epic**: E1（平台域超额）| **Sprint**: S2 | **SP**: 2 | **状态**: Done
- **角色**: 平台管理员
- **故事**: 作为平台管理员，我需要在业务域详情中完善客户管理的日常操作体验。
- **AC**:
  1. 在 US-S1-06 已有列表/手动添加/员工导入/批量启停基础上，支持单条客户资料**只读查看**（`GET .../customers/{id}` + 只读 Modal）；**不提供**资料编辑。
  2. 筛选、空态、错误提示为中文；权限与 **`platform.domain.control.customer.*`** 一致（自 `platform.domain.customer.*` 迁移）。
  3. **不包含** 客户自助注册 API（US-S1-04 仍延后）。
- **规则**: 客户登录标识规则不变（foundation-rules）。
- **DB 增量**: Flyway `V202606070002`（权限码 rename + catalog `PLATFORM-DOMAIN-CONTROL-CUSTOMER`）
- **备注**: `detail-customers.tsx`；`fetchDomainCustomer`；禁用 `ConfirmPopover` 二次确认

### US-S2-05 双层屏蔽词库（平台全局 + 域内）

- **Epic**: E1（平台域超额）| **Sprint**: S2 | **SP**: 3 | **状态**: Done
- **角色**: 平台管理员
- **故事**: 作为平台管理员，我需要维护平台级全局屏蔽词与各业务域屏蔽词库。
- **AC**:
  1. **平台全局**：管理入口（如 `/platform/blockwords`）与 API；词库跨域生效（`business_domain_id` 为空或等价，见 increment-plan）。
  2. **业务域域内**：详情「屏蔽词库」Tab 维护该域词条（增删查、重复提示、空态等）。
  3. **权限分离**：
     - 平台全局：**`platform.blocked_word.*`**（如 `.read` / `.create` / `.delete`）
     - 业务域（平台端域详情内）：**`platform.domain.blocked_word.*`**
  4. Flyway 登记上述权限码与菜单按钮；`AuthGuarded` / `RequirePermission` 与码一致。
- **规则**: 词条去首尾空格；禁止空词。
- **DB 增量**: `blocked_word` 扩展 + 权限码 Flyway — 见 increment-plan §3
- **备注**: 迁移自旧 `domain.blocked_word.*`；域内 `detail-blockwords.tsx`

### US-S2-06 域内业务日志

- **Epic**: E1（平台域超额）| **Sprint**: S2 | **SP**: 2 | **状态**: Done
- **角色**: 平台管理员
- **故事**: 作为平台管理员，我需要在业务域详情中查看该域的操作日志与登录日志。
- **AC**:
  1. 域详情侧栏独立入口「操作日志」「登录日志」（非页内 Tabs）。
  2. 支持分页与时间/结果/关键词筛选；列与平台 [`audit-logs`](../../UnionDeskWeb/apps/UnionDeskAdminWeb/src/pages/platform/audit-logs) 对齐（无业务域列）。
  3. 调用域级 `GET .../domains/{domainId}/audit-logs` / `login-logs` API。
  4. **权限**：`platform.domain.control.audit_log.read` / `platform.domain.control.login_log.read`；平台页 `platform.log.*`。
- **规则**: 审计日志不可删。
- **DB 增量**: Flyway `202606090001`（权限迁移 + 域详情 catalog）
- **备注**: `detail-audit-logs.tsx` / `detail-login-logs.tsx`

### US-S2-E2-00 业务域端最小可达

- **Epic**: E2 | **Sprint**: S2 | **SP**: 3 | **状态**: Todo
- **角色**: 具备 business scope 的域内员工
- **故事**: 作为域内员工，我登录后应进入业务域端首页，并能通过菜单访问至少一个系统管理页面。
- **AC**:
  1. 仅 **business** 权限、无平台权限的账号：登录后默认进入域内首页（如 `/home` 或菜单首项），**非** `/platform/home`。
  2. 动态菜单与 `iam_admin_menu.scope=business` 一致；至少 **1** 个 `pages/system/*` 页面可打开（非 Empty 占位）。
  3. 与平台端菜单隔离：business 树中无 `/platform/` 模块。
  4. **平台权限判定（前端）**：权限快照 `actions` 中若存在任意以 **`platform.`** 开头的权限码，则视为具备 **平台权限**（`platformAccess = true`），首页解析与 [`resolve-home-path.ts`](../../UnionDeskWeb/apps/UnionDeskAdminWeb/src/router/extra-info/resolve-home-path.ts) 优先 `/platform/home`；与后端 `platformAccess` 字段对齐或以前端规则为准（实现时二选一文档化）。
- **规则**: 双控制台边界见 backlog-epics §8.0。
- **DB 增量**: 按需菜单 Flyway；可选扩展 snapshot 中 `platformAccess` 推导逻辑
- **备注**: inventory §7；工单类型等归 **US-S2-E2-01（Stretch）**

---

## Sprint 3 及以后（占位）

> Epic 方向见 [`backlog-epics.md`](./backlog-epics.md)。E3–E5 Story 下轮规划时再拆 SP 与 AC。

> **US-S1-04**（客户注册 API）、**US-S1-05**（CustomerWeb）：**S1 暂缓**（US-S1-04 决策 2026-05-30；US-S1-05 决策 2026-05-26）；建议后续 Sprint（E3）再排；不阻塞 E1 平台端收口。

---

## 维护

- 每 Sprint 开始：从 Todo 中选取 ≈13 SP 写入 sprint 计划
- Story 完成：更新状态，并在 increment-plan 登记 Flyway 版本
- 与代码偏差：登记 [`qa/implementation-traceability.md`](../qa/implementation-traceability.md)
