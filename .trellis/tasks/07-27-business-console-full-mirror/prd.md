# 业务域端完整镜像平台域控制台

## Goal

在 AdminWeb **业务域端**（根级非 `/platform/`、`iam_admin_menu.scope=business`）落地与 **平台内业务域控制台**（`/platform/domains/detail/:domainId`）功能完整对齐的域治理能力，使 `domain_admin` 无需进入平台端即可完成本域配置与人员管理。

## Background

- 平台侧域控制台（US-S2-01～06 + 事项配置）已成熟，含约 12 个 Tab 模块。
- 业务域端目前仅 US-S2-E2-00 最小可达（`/home`、`/system/menu|role`）；PRD §5.3 / Epic E2 的域配置与人员能力尚未在 business 菜单落地。
- 不新建独立 DomainWeb；继续复用 `UnionDeskAdminWeb` 双控制台模型。

## Confirmed Decisions

| 决策 | 结论 |
|:---|:---|
| 范围策略 | **C：完整镜像** — 对齐平台域控制台全部模块（含通知占位） |
| 前端落地 | **独立页面** — 各模块独立 page 文件与路由，便于后续个性化；**不**抽共享 Panel 强制双端复用 |
| 执行方式 | Trellis 父任务 + 可独立验收的子任务 |
| 应用边界 | 仍在 `UnionDeskAdminWeb` 内；不做独立 DomainWeb |

## Requirements

### R1 — 功能镜像清单

业务域端 MUST 提供与平台域控制台对应的下列模块（独立页面）：

| 模块 | 平台对照 | 业务端差异约束 |
|:---|:---|:---|
| 概览 | `overview` | 仅本域数据；无跨域治理入口 |
| 通用设置 | `basic` | **禁止**删除业务域 |
| 人员管理 | `members` | 权限码使用 `domain.member.*` |
| 角色管理 | `roles` | 权限码使用 `domain.role.*`；能力至少不低于平台只读；写能力按 PRD F3.3 另开子要求 |
| 客户管理 | `customers` | `domain.customer.*` |
| 入域管理 | `onboarding` | 本域邀请码/注册策略 |
| 事项配置 | `ticket_config` | 类型/属性/状态；表单/流程设计器入口可达 |
| 屏蔽词库 | `blockwords` | 本域词库 |
| 通知配置 | `notifications` | 允许与平台一致的占位页，但须有独立路由与菜单 |
| 参数配置 | `config` | 本域 KV |
| 操作日志 | `audit_logs` | 本域只读 |
| 登录日志 | `login_logs` | 本域只读 |

### R2 — 独立页面与路由

- 每个模块 MUST 有独立页面文件（如 `pages/domain/...`），不得与平台 `detail-*.tsx` 共用同一实现文件。
- 允许参考/复制平台实现作为起点，但业务端页面后续可独立演进。
- 路由 MUST 为根级非 `/platform/` 前缀；菜单 `scope=business`。

### R3 — 域上下文

- 业务端页面 MUST 绑定当前员工会话的活跃业务域（`UserContext.businessDomainId` / 等价快照字段），不得要求用户从跨域列表进入。
- 多域成员的切换策略在 Open Question 中确认后写入本 PRD。

### R4 — 权限隔离

- 业务端按钮/菜单 MUST 使用 `domain.*` 权限码，不得污染纯 business 快照为 `platform.*`（对齐 `business-console-minimum-reach`）。
- 平台端现有 `platform.domain.control.*` 路径与权限 MUST 保持可用、行为不回归。

### R5 — 非目标（Out of Scope）

- 新建 `UnionDeskDomainWeb` 或其它独立前端应用
- 跨域业务域列表/创建/删除（属平台治理）
- 全局事项字典、全局屏蔽词、离职池、平台组织 IAM
- 工单作业台 / 咨询工作台（Epic E3/E5）
- 强制抽取双端共享 Panel 组件库（本任务明确不做）

## Acceptance Criteria

- [ ] AC1：`domain_admin` 登录后侧栏可见业务域治理菜单组，且不含 `/platform/domains` 等平台模块
- [ ] AC2：R1 清单中每个模块均有独立路由页面可打开（通知可为占位 Empty，但不可缺路由）
- [ ] AC3：人员/客户/入域/事项配置/屏蔽词/参数/日志的主流程与平台域控制台行为一致（本域数据、中文提示、权限显隐）
- [ ] AC4：通用设置可编辑本域基础信息，但无删除域入口
- [ ] AC5：权限快照 actions 使用 `domain.*`，不含因本功能引入的 `platform.domain.control.*` 按钮码
- [ ] AC6：平台 `/platform/domains/detail/:domainId` 既有功能无回归
- [ ] AC7：父任务下各子任务均可独立验收；父任务最终做一次端到端集成核对

## Open Questions

1. **导航形态**：业务端是「侧栏多菜单项 + 各模块独立路由」，还是「单一域控制台壳 + 左侧 Tab（内容为独立 page 文件）」？（待确认）
2. **多域切换**：员工属多个域时，活跃域如何切换？（待确认；需先查现有会话是否已支持）
3. **角色写能力**：镜像阶段角色页是否保持平台只读，还是直接按 PRD F3.3 开放自定义角色？（待确认）

## Technical Notes

- 后端优先复用 `/api/v1/admin/domains/{domainId}/…` 既有 Controller/Service；鉴权补充/对齐 `domain.*`。
- 前端复制平台面板为独立实现时，须替换权限码与 `domainId` 来源（会话活跃域，非 path 参数为主）。
- 复杂任务：本父任务需 `design.md` + `implement.md`；实现落在子任务。

## Child Task Map（规划中）

| 建议子任务 | 交付 | 对应 AC |
|:---|:---|:---|
| 基建：路由/菜单/`domain.*` 权限/活跃域上下文 | 可导航空壳 + 权限种子 | AC1, AC5 |
| 概览 + 通用 + 入域 + 参数 | 4 页 | AC2–4 |
| 人员 + 客户 + 角色 | 3 页 | AC2–3 |
| 事项配置（含设计器入口） | 1 页 + 子路由 | AC2–3 |
| 屏蔽词 + 双日志 | 3 页 | AC2–3 |
| 通知占位 | 1 页 | AC2 |
| 父任务集成验收 | 回归平台 + E2E | AC6–7 |
