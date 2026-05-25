# Product Backlog — User Stories

| 文档版本 | 日期 | 说明 |
|:---|:---|:---|
| 1.2 | 2026-05-24 | S0 收口；S1 Committed；S2+ 占位（E2 业务域端 / E3 工单）；状态与 Git HEAD 对齐 |

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

---

## Sprint 1 — 管理端 Walking Skeleton（E1）

### US-S1-01 平台登录与动态菜单

- **Epic**: E1 | **Sprint**: S1 | **SP**: 3 | **状态**: Partial
- **角色**: 平台管理员
- **故事**: 作为平台管理员，我希望登录后看到与我权限匹配的菜单，以便进入各管理模块。
- **AC**:
  1. 登录成功进入 `/platform/home`
  2. 菜单来自 `iam_admin_menu`，与 Flyway 种子一致（V202605220001 五模块）
  3. 无权限菜单不可见（FR-03 部分）
- **规则**: FR-01、FR-03
- **备注**: Auth + generateRoutesFromBackend 已有

### US-S1-02 创建业务域与 bootstrap

- **Epic**: E1 | **Sprint**: S1 | **SP**: 5 | **状态**: Done
- **AC**:
  1. `POST /api/v1/admin/domains` 创建域
  2. 创建者获得域内 super_admin（DomainBootstrapService）
  3. 管理端列表/详情可访问
- **备注**: DomainService、domains 页面

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
  3. 未授权按钮不可见（FR-03）
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
