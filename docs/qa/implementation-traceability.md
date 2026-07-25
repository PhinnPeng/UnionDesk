# 实现可追溯性（S1 / S2 收口）

| 文档版本 | 日期 | 说明 |
|:---|:---|:---|
| 1.1 | 2026-05-26 | Sprint 1 已签 off（见 sprint-1-plan §11） |
| 1.2 | 2026-06-15 | Sprint 2 已签 off（见 sprint-2-plan §10） |
| 1.3 | 2026-06-17 | Sprint 3 进度：US-S3-00 Done |
| 1.4 | 2026-07-19 | 员工身份遗留表物理拆除关闭 |

> 权威 Story 见 [`../product/backlog-stories.md`](../product/backlog-stories.md)；实现清单见 [`../product/implementation-inventory.md`](../product/implementation-inventory.md)。

---

## 1. Sprint 1 结论（已关闭）

- **签 off**：[`sprint-1-plan.md`](../product/sprint-1-plan.md) **§11**（2026-05-26）
- **Committed + 随主路径 Story**（S1-00a/b、US-S1-01/02/03/06/07/09）：**Done**
- **S1 暂缓**（不纳入签 off）：US-S1-04、US-S1-05、US-S1-08

---

## 2. Sprint 2 结论（已关闭）

- **签 off**：[`sprint-2-plan.md`](../product/sprint-2-plan.md) **§10**（2026-06-15）
- **Committed Story**（US-S2-UX-01、US-S2-01～06、US-S2-E2-00）：**Done**
- **Stretch 未纳入**：US-S2-E2-01、US-S1-08、US-S1-04/05
- **Flyway 上限**（编码完成时）：`V202606140001`（E2-00 business 菜单收敛）

---

## 3. Sprint 3 进度（编码中）

- **计划**：[`sprint-3-plan.md`](../product/sprint-3-plan.md) **§1–§2**（2026-06-17）
- **Done**：**US-S3-00**（IAM 控制台绑定，`V202606150001`）
- **待编码**：US-S3-01～04
- **北极星验收**：单域 **FR-06** 闭环（客户提单 → 客服处理 → 客户可查）
- **Stretch 未纳入**：§2.1（SLA UI、审计语义、US-S1-08 等）

---

## 4. 已知偏差与后续项

| 项 | 计划/Story | 现状 | 处置 |
|:---|:---|:---|:---|
| 员工身份遗留表双路径 | Trellis `07-18-staff-identity-legacy-demolition` | 已切齐 `/admin/staff`；DROP `user_account` 等四表；seed 平台管理员 | **已关闭**（`V20260719100445` / `V20260719100446`） |
| 客户注册完整 AC | US-S1-04 | 后端 `register` 存在；S1 不验收双字段/邀请完整 AC | **S3** US-S3-02 |
| CustomerWeb 真实注册 | US-S1-05 | 客户端仍可能走 demo 路径 | **S3** US-S3-02/03 |
| 跨域访问拒绝 FR-02 | US-S1-08 | 未系统化单测/联调 | **暂缓** → 后续迭代 |
| 日志页双入口 | US-S1-09 | `platform/log/*` 与 `platform/audit-logs/` Tabs 重叠 | **S3 Stretch** US-S3-UX-03（inventory §5） |
| 客户手动添加后初始密码 | US-S1-06 | 随机密码 + `must_change_password`，无管理端通知/重置 UI | 产品 backlog 后续 Story |
| Flyway squash rebaseline | sprint-1-plan §7 | S1 末未执行 | 待菜单稳定后评估 |
| 已删域直链详情 | US-S2-01 AC4 | `getDomain` 对已删域门控未齐 | **S2+ 延后** |
| business `/system/user` 等 | US-S2-E2-00 | `/system/menu`、`/system/role` 可打开；user/dept 仍占位 | S3 Stretch / S4+ |
| 工单类型设计 UI | US-S2-E2-01 | `detail-tickets.tsx` Empty | **S3** US-S3-01 |
| E2-00 浏览器 E2E | US-S2-E2-00 | 单测覆盖；未记录 domain_admin 全链路浏览器冒烟 | 联调时补验 |

---

## 5. 权限与菜单（US-S1-07 / US-S1-06）

- 客户管理权限码：`platform.domain.customer.read/create/update`（Flyway `V202605330002`、`V202605330003`）
- 快照 `actions` 合并 `iam_role_permission`（`AdminMenuService.mergeRolePermissionActions`）
- 业务域详情「客户管理」Tab：侧栏按 `platform.domain.customer.read` 显隐

---

## 6. 变更记录

| 日期 | 摘要 |
|:---|:---|
| 2026-05-26 | 初版：S1 DoD 可追溯登记；联调库/health 复验记录 |
| 2026-06-15 | S2 签 off；§2 S2 结论；S2 已知偏差登记 |
| 2026-06-16 | S3 规划草案；§3；偏差项指向 US-S3-xx |
| 2026-06-17 | US-S3-00 Done；§3 进度更新；Flyway `V202606150001` |
| 2026-07-19 | 员工身份遗留表物理拆除关闭；§4 登记；Flyway `V20260719100445`/`V20260719100446` |
