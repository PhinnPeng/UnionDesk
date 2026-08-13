# Sprint 4–8 路线图 — 系统主要功能路径实现（轻量 Scrum）

| 文档版本 | 日期 | 周期 | 说明 |
|:---|:---|:---|:---|
| 1.0 | 2026-08-13 | 周 Sprint（1 周） | 基于 08-13 功能缺口盘点（feature-list 54 条：31 已实现/13 部分/7 规划中/3 占位）制定 |

> **状态**：待启动。S3 未签 off（US-S3-02/03 承接中、US-S3-04 未承接）；本文档将 S3 余量并入 S4/S5 收口。
> **执行方式**：轻量 Scrum（时间盒 + 范围承诺 + 签 off，裁剪仪式）。每任务严格 Trellis：brainstorm → prd/design/implement → start → implement → check → 提交 → 归档。
> **已完成前置**（2026-08-12/13）：P0 安全债（08-12-p0-cross-domain-security 已归档）、客户授权链（08-13-customer-permission-chain，提单/工单/站内信/附件客户可用）。

---

## 0. 总览

| Sprint | 时间 | 目标 | 任务数 | 签 off 标准 |
|:---|:---|:---|:---|:---|
| S3.5 | 8/14–8/16 | 收尾 + 地基自洽 | 3 | 平台首页提交归档；测试全绿；Flyway 空库全量通过 |
| S4 | 8/17–8/23 | 客户端主路径全通 | 3 | F1.1/F1.5/F1.7 归档；客户全链路冒烟 |
| S5 | 8/24–8/30 | 管理端 E3 签 off | 3 | US-S3-04 归档；SLA 域端页/安全告警可用 |
| S6 | 8/31–9/6 | 平台端清收 + 集团模板层 | 5 | 导入导出/组织合并/仪表盘/模板清理归档；role_template 落地 |
| S7 | 9/7–9/13 | Epic 落地 + 跨域批量停用 | 3 | SLA 预警 UI；批量停用（step-up）可用 |
| S8 | 9/14–9/20 | 文档治理 + 全量验收 | 2 | PRD 修正；feature-list/backlog 同步；验收测试固化 |

---

## 1. S3.5（8/14–8/16）— 收尾 + 地基自洽

**Sprint 目标**：① 平台首页菜单上移交付；② 测试身份缺陷清零；③ Flyway 迁移链 fresh-install 自洽。

| # | 任务（slug） | 内容 | 优先级 | 依赖 | 验收要点 |
|:---|:---|:---|:---|:---|:---|
| 1 | `platform-home-overview-promote`（已有任务） | 批 0：提交迁移 V20260813120000 + 归档 | P3 | — | 提交 + 归档 |
| 2 | `test-identity-fix`（新建） | 批 4：SlaRuleCrud(3)/TicketLifecycle 员工流(4) 测试身份修复（改 agent/domain_admin 登录）+ 断言信封 data.* 收敛 + backlog 状态回写（US-S1-08 已完成等） | P1 | — | 全量测试全绿（预存失败清零） |
| 3 | `flyway-chain-consistency`（新建） | 批 4：current/ 迁移链 fresh-install 自洽（V202606060001 CHECK 冲突、V20260719100446 漏卸 FK） | **P0** | — | 空库 Flyway 全量通过 + validate |

## 2. S4（8/17–8/23）— 客户端主路径全通

**Sprint 目标**：客户端从注册到评价的完整主路径真实可用（F1.7 → F1.1 → F1.5）。

| # | 任务（slug） | 内容 | 优先级 | 依赖 | 验收要点 |
|:---|:---|:---|:---|:---|:---|
| 4 | `customer-register-api`（已有任务） | F1.7 注册真实 API（POST /api/v1/auth/register 前端接入，去 mock `cust-at`）+ F4.20 邀请码入域（US-S3-02/US-S1-04/05） | **P0** | 客户授权链 ✅ | 注册/入域真接口；DR-01/02 校验 |
| 5 | `ticket-dynamic-form`（已有任务） | F1.1 提单动态表单渲染（管理端 Formily 配置 → 客户端按配置渲染动态字段）+ attachmentIds 真传 | **P0** | 客户授权链 ✅ | 动态字段按配置渲染；附件上传可用 |
| 6 | `satisfaction-survey`（已有任务） | F1.5 满意度评价全链路：评价模型/API + 客户端入口（工单详情）+ 通知中心评价入口 + 埋点 | **P0** | 任务 4/5 | 评价闭环（工单关闭后可评）+ 埋点 |

**签 off**：三个任务归档；客户冒烟（注册 → 入域 → 提单（动态表单）→ 我的工单 → 回复 → 工单关闭 → 评价）。

## 3. S5（8/24–8/30）— 管理端 E3 签 off

**Sprint 目标**：员工端工单队列与处理闭环（US-S3-04），E3 全部 Committed 签 off；域端 SLA/通知、平台安全告警落地。

| # | 任务（slug） | 内容 | 优先级 | 依赖 | 验收要点 |
|:---|:---|:---|:---|:---|:---|
| 7 | `staff-ticket-queue`（新建） | F2.1 员工端工单队列与详情处理：业务域端 `/ticket-queue`（非 hideInMenu 演示页）+ 认领/分派/回复/关闭/合并全链路（ticket.* 11 码已注册）+ agent 角色联调 | **P0** | S4 任务 5（动态表单） | agent 全链路可用；E3 签 off |
| 8 | `domain-sla-page`（新建） | F3.2 SLA 域端独立页 `/domain/settings/sla` + F3.11 通知配置实现（语义澄清：SLA 规则 vs 通知模板） | P1 | — | 域端 SLA 规则/通知模板可用 |
| 9 | `security-alerts-center`（新建） | F4.4 安全告警中心独立页 `/platform/security-alerts` + 密码强度/登录锁定/IP 白名单 | P1 | — | 告警页 + 安全策略生效 |

## 4. S6（8/31–9/6）— 平台端清收 + 集团模板层

**Sprint 目标**：平台端决策已定清收项落地；集团角色模板层（P1-1）交付。

| # | 任务（slug） | 内容 | 优先级 | 依赖 | 验收要点 |
|:---|:---|:---|:---|:---|:---|
| 10 | `user-import-export`（新建） | F4.18 导入导出并入用户管理页工具栏 + F4.3 永久删除前端入口（后端已就绪） | P1 | — | 工具栏 + API 联调 |
| 11 | `org-config-merge`（新建） | F4.21 组织配置与 F4.8 合并（决策已定） | P3 | — | 页面合并 |
| 12 | `template-pages-cleanup`（新建） | F3.15/F3.16/F4.22 模板遗留页清理（异常页/公开页保留）+ CustomerWeb settings 死目录 | P3 | — | 清理完成、无残留路由 |
| 13 | `dashboard-real-aggregation`（新建） | F4.6 平台首页真实聚合（去 DemoDataService mock，count(*) 等） | P2 | — | 真实数据 + 空态 |
| 14 | `group-role-management` P1-1（已有任务） | 模板层：role_template 三表 Flyway + 模板 CRUD/apply/sync/unapply/bind-members + F4.9「模板」Tab | P2 | P0 安全债 ✅ | AC1/AC2/AC5/AC6 |

## 5. S7（9/7–9/13）— Epic 落地 + 跨域批量停用

**Sprint 目标**：E4 SLA 预警 UI；E5 在线咨询规划+客户端落地；集团跨域批量停用（P1-2）。

| # | 任务（slug） | 内容 | 优先级 | 依赖 | 验收要点 |
|:---|:---|:---|:---|:---|:---|
| 15 | `sla-ui-epic`（新建，brainstorm 先行） | E4：F2.2 SLA 感知与高亮（工单列表/详情 SLA 状态与预警 UI，引擎已实现 F4.15） | P2 | S5 任务 7 | 预警展示 |
| 16 | `online-consultation`（新建，brainstorm 先行） | E5：F1.2 客户端在线咨询 + F2.3 咨询工作台（转工单带客户信息+会话摘要；建议拆客户端/工作台两子任务） | P2 | S5 任务 7 | 咨询会话闭环 |
| 17 | `group-role-management` P1-2/P2 | 跨域批量停用（batch-status + step-up UI + F4.7 入口）+ 域端模板徽标/锁定/漂移 | P2 | S6 任务 14 | AC3/AC7；P2 展示 |

## 6. S8（9/14–9/20）— 文档治理 + 全量验收

**Sprint 目标**：文档一致性 + 双端验收固化。

| # | 任务（slug） | 内容 | 优先级 | 依赖 | 验收要点 |
|:---|:---|:---|:---|:---|:---|
| 18 | `prd-doc-fixes`（已有任务） | PRD 文档修正（§4.4 自洽检验、权限码升格）+ feature-list/backlog 状态同步 | P1 | 贯穿 | 文档一致 |
| 19 | 双端验收固化 | 客户端/管理端端到端验收测试固化（FR-01~06 场景化） | P2 | S3.5–S7 | 验收脚本全绿 |

---

## 7. 执行约定

- **任务创建**：每任务开始按 Trellis 流程 `task.py create` → brainstorm 收敛 → start；S3.5 的 2 个新任务（test-identity-fix / flyway-chain-consistency）可立即立项
- **并行约束**：S4 三任务可并行；S5 依赖 S4-5；S7 依赖 S5-7；S6-14 依赖已完成的 P0 安全债（可提前）
- **签 off 回写**：每 Sprint 结束更新本文件状态列 + feature-list.md 状态列 + backlog（US-S1-08 已完成等）
- **范围变更**：E4/E5 任务需 brainstorm 后细化范围；若 Sprint 内发现超范围需求，移入后续 Sprint 而非挤占
