# 业务域客户与入域页面完善

## Goal

在业务域控制台补齐「入域配置 / 入域管理」的写能力与邀请码 CRUD，使业务管理员无需进入平台域详情即可完成客户入域策略与邀请码运营。

本任务聚焦 **Admin 业务域端**（`/domain/customers/*`、`/domain/settings/onboarding`）。不包含 CustomerWeb 真实注册/邀请联调（可后续单独立项）。

## Background

- 客户列表页（`/domain/customers/list`）已具备创建 / 从员工导入 / 启停等写能力。
- 入域页（`pages/domain/onboarding`）目前策略开关只读，且无邀请码列表与创建/删除。
- 后端已提供：
  - `PUT /admin/domains/{id}` 更新 `registration_enabled` / `invitation_enabled`（`domain.general.update`）
  - `GET|POST|DELETE .../invitation-codes`（`domain.invitation_code.read|create|delete`）
- 平台侧 `detail-onboarding.tsx` 已实现可写策略开关，可作为行为参考。

## Requirements

### R1 入域策略可写

- 业务域入域页「客户自助注册」「邀请码入域」开关可写，调用既有 `updateAdminDomain`。
- 关闭策略时需二次确认；无 `domain.general.update` 时开关只读。
- 成功后本地状态同步，无需整页刷新。

### R2 邀请码列表与 CRUD

- 入域页增加「邀请码」Tab：分页列表展示码、渠道、过期、用量、状态。
- 具备 `domain.invitation_code.create` 时可创建（渠道、过期时间、最大使用次数；字段均可选）。
- 具备 `domain.invitation_code.delete` 时可删除（ConfirmPopover）。
- 无 `domain.invitation_code.read` 时隐藏邀请码 Tab；仍可查看策略 Tab（需 `domain.general.read` 或同等入域入口权限）。

### R3 共享 API 与权限常量

- `@uniondesk/shared` 补齐 create / delete 邀请码封装，并对列表项做 camelCase/snake_case 归一化。
- 前端 `domain-permissions.ts` 补齐 `DOMAIN_INVITATION_CODE_CREATE` / `DELETE`。

### R4 范围外（明确不做）

- 不改造 CustomerWeb / 客户门户真实入域 API。
- 不重构平台 `DomainOnboardingPanel`（可选后续复用，本任务以业务域页完整交付为准）。
- 不改动客户列表页既有 CRUD（已可用）。

## Constraints

- 沿用 Ant Design v6、现有 Card + Tabs 布局；列表操作对齐客户/屏蔽词页模式。
- 不新增路由常量文件；路径沿用现有菜单挂载。
- 外科手术式改动：不顺手改菜单 Flyway / 侧栏结构。

## Acceptance Criteria

- [x] 有 `domain.general.update` 的业务域管理员可开关注册/邀请策略，关闭有确认，成功有提示。
- [x] 有邀请码读权限可见邀请码列表；有 create/delete 可创建与删除并刷新列表。
- [x] shared 暴露 create/delete API，页面不直接拼裸 axios。
- [x] 无相关权限时 UI 只读或隐藏操作，不出现未捕获错误导致白屏。
- [x] 本任务不引入 CustomerWeb 改动。

## Notes

- 菜单「客户管理 → 入域配置」与「组织成员 → 入域管理」共用同一页面组件，行为一致即可。
