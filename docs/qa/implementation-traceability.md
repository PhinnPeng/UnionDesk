# 实现可追溯性（S1 / S2 收口）

| 文档版本 | 日期 | 说明 |
|:---|:---|:---|
| 1.1 | 2026-05-26 | Sprint 1 已签 off（见 sprint-1-plan §11） |
| 1.2 | 2026-06-15 | Sprint 2 已签 off（见 sprint-2-plan §10） |

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

## 3. 已知偏差与后续项

| 项 | 计划/Story | 现状 | 处置 |
|:---|:---|:---|:---|
| 客户注册完整 AC | US-S1-04 | 后端 `register` 存在；S1 不验收双字段/邀请完整 AC | **暂缓** → E3 |
| CustomerWeb 真实注册 | US-S1-05 | 客户端仍可能走 demo 路径 | **暂缓** → E3 |
| 跨域访问拒绝 FR-02 | US-S1-08 | 未系统化单测/联调 | **暂缓** → 后续迭代 |
| 日志页双入口 | US-S1-09 | `platform/log/*` 与 `platform/audit-logs/` Tabs 重叠 | S3 收敛（inventory §5） |
| 客户手动添加后初始密码 | US-S1-06 | 随机密码 + `must_change_password`，无管理端通知/重置 UI | 产品 backlog 后续 Story |
| Flyway squash rebaseline | sprint-1-plan §7 | S1 末未执行 | 待菜单稳定后评估 |
| 已删域直链详情 | US-S2-01 AC4 | `getDomain` 对已删域门控未齐 | **S2+ 延后** |
| business `/system/user` 等 | US-S2-E2-00 | `/system/menu`、`/system/role` 可打开；user/dept 仍占位 | Stretch / S3+ |
| E2-00 浏览器 E2E | US-S2-E2-00 | 单测覆盖；未记录 domain_admin 全链路浏览器冒烟 | 联调时补验 |

---

## 4. 权限与菜单（US-S1-07 / US-S1-06）

- 客户管理权限码：`platform.domain.customer.read/create/update`（Flyway `V202605330002`、`V202605330003`）
- 快照 `actions` 合并 `iam_role_permission`（`AdminMenuService.mergeRolePermissionActions`）
- 业务域详情「客户管理」Tab：侧栏按 `platform.domain.customer.read` 显隐

---

## 5. 变更记录

| 日期 | 摘要 |
|:---|:---|
| 2026-05-26 | 初版：S1 DoD 可追溯登记；联调库/health 复验记录 |
| 2026-06-15 | S2 签 off；§2 S2 结论；S2 已知偏差登记 |
