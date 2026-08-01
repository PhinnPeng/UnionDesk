# 业务域端 P1：客户 + 屏蔽词 + 参数

## Goal

填实业务域端三个占位页，使具备对应 `domain.*` 权限的用户可在业务端完成本域客户管理、屏蔽词库与参数配置，无需进入平台域控制台。

父任务：`07-27-business-console-full-mirror`。前置：`07-31-business-console-p0-members-basic-onboarding`。

## Confirmed Decisions

| 决策 | 结论 |
|:---|:---|
| 批次 | 客户管理 + 屏蔽词库 + 参数配置 |
| 页面 | 独立 `pages/domain/*`，不与平台 `detail-*.tsx` 共用实现文件 |
| domainId | 会话活跃域（同 P0） |
| 菜单显隐 | 按读码细粒度（父任务方案 B） |
| 客户 | **完整写**对齐平台：`domain.customer.read/create/update_status` |
| 屏蔽词 | **完整 CRUD**：`domain.blocked_word.read/create/delete` |
| 参数 | 对照 `DomainConfigPanel`；`domain.config.read/update` |

## Requirements

### R1 — `/domain/customers`

- MUST 列表/筛选/分页对齐平台客户管理体验（中文）。
- MUST 支持添加客户、启停状态（写码门控）。
- MUST 只读依赖 `domain.customer.read`。

### R2 — `/domain/blockwords`

- MUST 列表/搜索/新增（含批量若平台有）/删除对齐平台屏蔽词面板。
- MUST 使用 `domain.blocked_word.*`，不得用平台 `platform.domain.control.blocked_word.*` 作为业务端鉴权。

### R3 — `/domain/config`

- MUST 对本会话域展示并可更新域参数 KV（能力对齐平台 `DomainConfigPanel`）。
- MUST 读/写分别绑 `domain.config.read` / `domain.config.update`。
- 允许在业务页内**调用**已有 `DomainConfigPanel`（传入 session `domainId` + 业务权限），或独立实现同等行为；不得把业务页做成仅 Empty。

### R4 — 横切

- 三页 MUST 订阅会话域；切域后数据刷新。
- 后端若仅认平台码，MUST 并列补 `domain.*`（外科手术）。
- MUST NOT 回归平台 detail 对应 Tab。

### R5 — Out of Scope

- 日志、域角色、运营概览 KPI、通知、入域写增强
- 强制抽共享 Panel 库（参数页复用现有 Panel 除外）

## Acceptance Criteria

- [ ] AC1：有 `domain.customer.read` 可开客户页；create/update_status 在对应码下可用
- [ ] AC2：屏蔽词可读可增可删（对应码）；无读码不可见
- [ ] AC3：参数页可读可改本域配置（对应码）
- [ ] AC4：三页绑定会话域；切域后数据属新域
- [ ] AC5：业务端仅 `domain.*`；平台 detail 客户/屏蔽词/参数无回归
- [ ] AC6：三页 Empty「功能开发中」消失
