# 业务域端 P0：人员 + 通用 + 入域只读

## Goal

填实业务域端三个占位页，使 `domain_admin`（及被授予对应 `domain.*` 读/写码的用户）无需进入平台即可：完整管理人员、编辑本域基础信息、只读查看入域策略。

父任务：`07-27-business-console-full-mirror`。

## Background

- 路由/菜单/占位已在 M0 落地：`/domain/members`、`/domain/basic`、`/domain/onboarding`。
- 平台对照：`detail-members.tsx`（完整写）、`detail-baseinfo.tsx`（含启停/删除）、`detail-onboarding.tsx`（注册/邀请策略开关）。
- 会话活跃域 + 侧栏切域（含清页签）由父任务 R3 / `07-28` 覆盖；本子任务消费 `defaultBusinessDomainId`，不自建切域 UI。

## Confirmed Decisions（继承父任务 + 本子任务拍板）

| 决策 | 结论 |
|:---|:---|
| 页面形态 | 独立 `pages/domain/*`，不与平台 `detail-*.tsx` 共用实现文件 |
| domainId | 会话活跃域；禁止依赖平台 path `:domainId` 作为主来源 |
| 菜单显隐 | 按读码细粒度（B）：`domain.member.read` / `domain.general.read` / 入域读码 |
| 人员 | **完整写**对齐平台 |
| 通用设置 | 仅基础信息更新；**禁**启停、**禁**删除 |
| 入域 | **P0 只读**（方案 B）；写策略后续子任务 |

## Requirements

### R1 — 人员管理 `/domain/members`

- MUST 列表展示本域成员（筛选/分页对齐平台体验，中文文案）。
- MUST 支持：添加成员、编辑成员角色、启停成员、移除成员（权限码 `domain.member.create|update_roles|update_status|delete`）。
- MUST 只读入口依赖 `domain.member.read`；无读码不可见菜单/不可进页。
- MUST NOT 使用 `platform.domain.control.member.*` 作为业务端鉴权码。

### R2 — 通用设置 `/domain/basic`

- MUST 展示并可编辑本域基础信息（名称、logo、描述等，字段对齐平台基础信息区，`domain.general.update`）。
- MUST 提供只读查看（`domain.general.read`）。
- MUST NOT 提供删除业务域入口。
- MUST NOT 提供启停业务域入口（启停仅平台）。
- 域编码等只读字段行为对齐平台（不可改 code 则保持只读）。

### R3 — 入域管理 `/domain/onboarding`（只读）

- MUST 展示本域「客户自助注册」「邀请码入域」策略当前状态（对照平台 Tabs/开关语义）。
- MUST 以只读呈现（禁用 Switch 或纯文案）；MUST NOT 调用更新策略 API。
- 菜单可见依赖约定读码（见 design：`domain.invitation_code.read` 或与平台入域入口等价的业务读码；以实现时 design 为准）。
- 创建/删除邀请码、改策略 **Out of Scope（本子任务）**。

### R4 — 横切

- 三页 MUST 使用会话活跃 `domainId`；切域后随 auth 状态刷新数据（可依赖既有切域清页签 + 重导航）。
- 写按钮 MUST 用 `AuthGuarded` / `hasPermission` 绑对应写码。
- MUST NOT 回归平台 `/platform/domains/detail/:domainId` 既有行为。

### R5 — Out of Scope

- 客户/屏蔽词/参数/日志/域角色/运营概览 KPI
- 入域写能力、邀请码 CRUD
- 多域切换 UI 本体（`07-28`）
- 强制抽取双端共享 Panel

## Acceptance Criteria

- [ ] AC1：有 `domain.member.read` 可打开人员页；完整写路径在对应写码下可用；无读码不可见入口
- [ ] AC2：通用设置可保存基础信息；页面无启停、无删除
- [ ] AC3：入域页只读展示注册/邀请策略状态；无法改策略
- [ ] AC4：三页数据均绑定当前会话域；切到另一启用域后展示新域数据（配合既有切域）
- [ ] AC5：业务端权限码为 `domain.*`；平台域控制台成员/通用/入域无回归
- [ ] AC6：占位 Empty 文案从上述三页消失，主流程可用

## Notes

- 技术边界与文件清单见同目录 `design.md`；执行顺序见 `implement.md`。
- 父任务 PRD R1/R3/R4 与本文件冲突时，**以本子任务 Confirmed Decisions 为准**（入域只读、通用禁启停）。
