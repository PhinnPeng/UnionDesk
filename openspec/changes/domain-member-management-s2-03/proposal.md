## Why

US-S2-03 要求平台管理员在业务域详情「员工管理」Tab 完成成员全生命周期操作（添加、改角色、启停、移除），但当前 `detail-members.tsx` 仅只读列表；后端缺 `staff-candidates` 与 `PUT .../status`；`shared` 未封装写操作 API；`domain.member.update_status` 权限与菜单按钮未落 Flyway。

## What Changes

- 后端：`DomainMemberService` / `DomainMemberController` 增补 `listStaffCandidates`、`updateMemberStatus`；复用已有 create / updateRoles / delete 与保护规则。
- Shared：封装成员 CRUD + 启停 + 候选员工查询 API。
- Flyway：幂等补齐 `domain.member.*`（含 `update_status`）权限码与 `PLATFORM-DOMAIN-MEMBERS` 控制台按钮。
- 前端：`detail-members.tsx` 升级为可交互（对齐 `detail-customers.tsx` 交互模式）。
- 文档：backlog / increment-plan / S2-closure-tracker 收口。

## Capabilities

### New Capabilities

- `domain-member-management`（平台控制台域内员工管理 UI + API 缺口补齐）

### Modified Capabilities

- （无 main spec 破坏性变更；`domain-membership` delta 补充管理场景）

## Impact

- **后端**：~8 Java 文件；1 Flyway 脚本（或恢复已执行版本文件以通过校验）
- **前端**：`detail-members.tsx`、`platform-domain-permissions.ts`
- **shared**：`api.ts`、`types.ts`
- **非目标**：域角色 CRUD（US-S2-02）、员工平台账号管理、域内展示列编辑
