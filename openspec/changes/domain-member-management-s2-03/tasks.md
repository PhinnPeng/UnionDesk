## 1. 后端 API

- [x] 1.1 `DomainMemberDtos` 增补 `StaffCandidateView`、`UpdateDomainMemberStatusRequest`、`CreateMemberWithStaffRequest`、`created_at`
- [x] 1.2 `DomainMemberService.listStaffCandidates` + `updateMemberStatus` + `createMemberWithStaff`（含保护规则）
- [x] 1.3 `DomainMemberController` 路由与 `PermissionCodes.DOMAIN_MEMBER_UPDATE_STATUS`
- [x] 1.4 `AdminPermissionCatalog` 注册新权限码
- [x] 1.5 单元测试：`DomainMemberControllerTests`、必要时 `DomainMemberServiceTests`

## 2. 数据库

- [x] 2.1 恢复/创建 `V202606060001__domain_member_permissions_and_menu.sql`（幂等）

## 3. Shared

- [x] 3.1 `DomainStaffCandidate` 类型
- [x] 3.2 `fetchDomainStaffCandidates`、`createDomainMember`、`createDomainMemberWithStaff`、`updateDomainMemberRoles`、`updateDomainMemberStatus`、`deleteDomainMember`

## 4. 前端

- [x] 4.1 `platform-domain-permissions.ts` 成员权限常量
- [x] 4.2 `detail-members.tsx` 可交互（添加/改角色/启停/移除/批量）
- [x] 4.3 `permission-code-labels.ts` 补 `update_status`

## 5. 验收与文档

- [x] 5.1 后端单测通过 + AdminWeb typecheck
- [x] 5.2 backlog / increment-plan / S2-closure-tracker → Done
