# Product Backlog — User Stories

| 文档版本 | 日期 | 说明 |
|:---|:---|:---|
| 1.3 | 2026-05-24 | S0 含 US-S0-07 基线快照；S1 平台端 |

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

## Sprint 1 — 员工平台端（E1，`/platform/`）

> **Sprint 执行计划**：[`sprint-1-plan.md`](./sprint-1-plan.md)（Committed 顺序、P0 工程、DoD）。

### S1-00 联调工程（P0，非功能 Story）

> 不计入 E1 业务 SP 合计；验收见 sprint-1-plan §4～§5。评审通过后先于 US-S1-03 编码。

#### S1-00a JRebel Maven 热更新

- **Sprint**: S1 | **SP**: 1 | **状态**: Todo
- **AC**:
  1. `UnionDesk/pom.xml` 含 `jrebel-maven-plugin` 1.2.1，`process-resources` 生成 `rebel.xml`
  2. 文档化无 IDEA 启动：`mvnw jrebel:generate` / `spring-boot:run` + JRebel agentpath
  3. 修改 `@RestController` 后联调可热更（Flyway/DDL 变更除外）
- **DB 增量**: 无

#### S1-00b 消除 Security 默认密码日志

- **Sprint**: S1 | **SP**: 0.5 | **状态**: Todo
- **AC**:
  1. 启动日志无 `Using generated security password`
  2. JWT 登录、403 中文行为不变；`JwtAuthenticationFilterTests` 通过
- **DB 增量**: 无

### Sprint 1 范围说明

- **S1 Epic E1 = 员工平台管理端**（PRD §3.4）：路由以 **`/platform/`** 为前缀；菜单 `iam_admin_menu.scope = platform`；典型角色 `platform_admin`。
- **业务域管理端**（PRD §3.3，根级非 `/platform/`，`scope = business`）**不在 S1 Committed**，归 **S2 / Epic E2**（见 [`backlog-epics.md`](./backlog-epics.md)）。
- 同一 `UnionDeskAdminWeb` 内可存在双 scope 菜单，但 **S1 UI 收口对象 = 平台端**；`/system/*` 等 business 骨架页成品化不计入 S1 完成度。
- **FR-03**（[`foundation-rules.md`](./foundation-rules.md) §5.1）：无按钮权限 → 按钮不可见；若仍调用 API → **403 + 中文**。

| Story | 交付面 | 标注 |
|:---|:---|:---|
| US-S1-01～03、07、09 | 平台端 `/platform/*` | S1 主路径 |
| US-S1-04 | 后端 API | 跨端/API |
| US-S1-05 | CustomerWeb | 跨端/客户端 |
| US-S1-06 | API + 平台域上下文 | 跨端/API，平台侧触发 |
| US-S1-08 | 后端鉴权 | 横切 |

### US-S1-01 平台登录与动态菜单

- **Epic**: E1 | **Sprint**: S1 | **SP**: 3 | **状态**: Partial
- **角色**: 平台管理员
- **故事**: 作为平台管理员，我希望登录后看到与我权限匹配的菜单，以便进入各管理模块。
- **AC**:
  1. 登录成功进入 `/platform/home`
  2. 菜单来自 `iam_admin_menu`，与 Flyway 种子一致（V202605220001 五模块）
  3. 无权限菜单不可见（FR-03：菜单可见性部分）
- **规则**: FR-03（菜单）；API 403 + 中文见 FR-01 / FR-03
- **备注**: Auth + generateRoutesFromBackend 已有；本 Story 不单独验收按钮级 FR-03

### US-S1-02 创建业务域与 bootstrap

- **Epic**: E1 | **Sprint**: S1 | **SP**: 5 | **状态**: Partial
- **AC**:
  1. `POST /api/v1/admin/domains` 创建域
  2. 创建者获得域内 super_admin（DomainBootstrapService）
  3. 管理端列表/详情可访问
- **备注**: 核心路径可用；创建/编辑仍用 `registration_policy` 单字段，双字段见 US-S1-03（联调库已 V202605250001，Git HEAD 代码未对齐）

### US-S1-03 入域双配置 CRUD

- **Epic**: E1 | **Sprint**: S1 | **SP**: 3 | **状态**: Partial
- **AC**:
  1. API 返回 `registration_enabled` / `invitation_enabled`
  2. 管理端创建/编辑/详情展示双配置
  3. 库中无 `registration_policy` 列
- **规则**: DR-01、DR-02（API 层）
- **DB 增量**: V202605250001（迁移脚本可能未入库；Git HEAD 仍可能为 `registration_policy`）
- **备注**: 与 [`implementation-inventory.md`](./implementation-inventory.md) §3–§4 同步校正

### US-S1-04 客户注册 API

- **Epic**: E1 | **Sprint**: S1 | **SP**: 3 | **状态**: Partial
- **AC**:
  1. 注册 API 校验双字段，中文错误提示
  2. 邀请码创建/使用校验 `invitation_enabled`
- **规则**: DR-01、DR-02
- **备注**: 后端待与 S1-03 双字段对齐；CustomerWeb 仍为 demo → US-S1-05

### US-S1-05 CustomerWeb 接真实注册/入域 API

- **Epic**: E1 | **Sprint**: S1 | **SP**: 5 | **状态**: Todo
- **角色**: 客户
- **故事**: 作为客户，我希望在客户端用真实 API 注册并加入业务域，而不是 demo portal。
- **AC**:
  1. 登录/注册页调用后端 `/api/v1/auth/register`
  2. 域下拉仅展示 `registration_enabled=allowed`
  3. 错误提示为中文
- **规则**: DR-01

### US-S1-06 域内客户手动添加

- **Epic**: E1 | **Sprint**: S1 | **SP**: 2 | **状态**: Partial
- **AC**:
  1. 有权限管理员可添加客户，状态 active
  2. 不受注册/邀请开关限制
- **规则**: DR-04

### US-S1-07 IAM 角色/权限/按钮

- **Epic**: E1 | **Sprint**: S1 | **SP**: 5 | **状态**: Partial
- **AC**:
  1. 角色/权限/菜单管理页可演示
  2. 未授权 API 403 + 中文（FR-01）
  3. 未授权按钮不可见（FR-03）；若仍调用对应 API → 403 + 中文
- **规则**: FR-01、FR-03

### US-S1-08 跨域访问拒绝

- **Epic**: E1 | **Sprint**: S1 | **SP**: 3 | **状态**: Todo
- **AC**:
  1. A 域身份访问 B 域数据返回 403 或空集
- **规则**: FR-02

### US-S1-09 登录日志与操作日志（管理端）

- **Epic**: E1 / E5 | **Sprint**: S1 | **SP**: 3 | **状态**: Partial
- **AC**:
  1. `/platform/log/login-log` 列表可读
  2. `/platform/log/operation-log` 列表可读
- **备注**: 菜单 V202605210001

---

## Sprint 2 及以后（占位，无 Committed Story）

> **不在当前两步（S0 收口 → S1 Commit）承诺内。** 下轮规划时再拆 Story、SP 与 AC。Epic 方向见 [`backlog-epics.md`](./backlog-epics.md) v1.2。

| Epic | 方向 |
|:---|:---|
| E2 | 员工业务域端完善（根级非 `/platform/`；PRD §3.3；工单类型/域内人员等） |
| E3 | 工单最小闭环（北极星主路径；反馈/建议 = ticket_type 预置） |
| E4 | SLA v1 |
| E5 | 在线咨询 |

> **US-S1-05**（CustomerWeb）在 S1 仍 Committed；非 `/platform/` 能力，完整产品归属 **E3**，不阻塞 E1 平台端收口。

---

## 维护

- 每 Sprint 开始：从 Todo 中选取 ≈13 SP 写入 sprint 计划
- Story 完成：更新状态，并在 increment-plan 登记 Flyway 版本
- 与代码偏差：登记 `qa/implementation-traceability.md`（待建）
