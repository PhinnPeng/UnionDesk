## Context

- 后端 `DomainMemberController` 已有 list / create / updateRoles / delete；`DomainMemberService` 已实现保护规则；列表 keyword 五字段已实现，缺 `created_from/to` 与 `created_at` 出参。
- 前端 `detail-members.tsx` 只读；`detail-customers.tsx` 为查询/批量/Steps Modal 参考。
- 权限码缺 `domain.member.update_status`；Flyway `V202606060001` 可能已在联调库执行。

## Goals / Non-Goals

**Goals**

- 对齐 backlog US-S2-03 + 用户修订：查询（含加入时间）、Steps 添加（平台员工 / 新建员工）、行内与批量启停/删除。
- 新建员工：域内 composite API，同步创建 `staff_account`（Scheme A 不变量）。
- 批量操作：前端循环单条 API（与客户 Tab 一致）。

**Non-Goals**

- 不改动 `DomainRoleController` 写接口。
- 不实现域内展示列编辑。
- 不新增 batch REST API；不修复客户 Tab 既有问题。

## Decisions

1. **staff-candidates**：`GET .../members/staff-candidates`，权限 `domain.member.create`。
2. **启停**：`PUT .../members/{memberId}/status`，权限 `domain.member.update_status`。
3. **新建员工**：`POST .../members/with-staff`，内部 `StaffAccountService.create`，权限 `domain.member.create`。
4. **列表时间**：`created_from` / `created_to` 过滤 `dm.created_at`。
5. **Flyway**：恢复 `V202606060001` 幂等脚本。
6. **前端**：单按钮 + Modal Steps；`Row`/`Col` + 必要时 `.module.less`。

## Risks / Trade-offs

- Flyway checksum 漂移 → 恢复同版本文件。
- 批量循环：保护规则导致部分失败 → 首错中断，用户逐条处理（与客户一致）。
