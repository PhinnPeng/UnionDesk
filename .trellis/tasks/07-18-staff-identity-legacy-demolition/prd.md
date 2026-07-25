# 员工身份遗留表物理拆除

## Goal

拆除遗留身份表双路径，使平台员工生命周期与域角色绑定仅以目标态表为准，消除因 `user_account` / `user_*_role` 与 `staff_account` / `domain_member_role` 并存导致的返工。

## Background

- 登录已走 `staff_account` / `customer_account`；平台用户 CRUD 仍偏 `user_account`；`POST /iam/users` 与 `/admin/staff` 冲突。
- 文档 L4 曾写「停写旧表」；本轮产品决策升级为**物理拆除**（可清空联调库重建）。
- 策略：技术债优先；范围仅身份 + 域/平台角色绑定；采用「代码切齐 → 一刀 DROP + seed」。

## Confirmed Decisions

| 决策 | 选择 |
|:---|:---|
| 总体策略 | 技术债拆除优先（非口径冻结优先 / 非仅 FR-06） |
| 范围 | 身份表 + 域角色单写（不含工单状态机、双控制台大搬迁） |
| 旧表处置 | 物理 DROP，非长期只读兼容 |
| 数据 | 联调库可清空重建，不做无损 ETL |
| 落地路径 | 先方案后迁目标态：代码切齐 → DROP + seed（无双写窗口） |

## Requirements

### R1 目标态真相源

- 自然人：`identity_subject`
- 员工账号：`staff_account`
- 平台角色绑定：`staff_account_platform_role`
- 入域：`domain_member`
- 域角色：`domain_member_role`
- 组织归属：`staff_organization`（新建，替代 `user_organization`）

### R2 API

- 平台员工唯一入口：`/api/v1/admin/staff*`
- 删除 `/api/v1/iam/users*`（不做长期 410）
- 补齐离职：`offboard` + 离职池（或等价 `status=offboarded` 列表）
- 平台角色继续走 `PUT/GET .../platform-roles`
- `IamController` 保留 menus/roles/permissions/me 等非用户表能力

### R3 前端

- AdminWeb 用户列表 / 创建编辑 / 离职池改调 staff API
- shared DTO 可适配，避免页面无意义大改

### R4 数据与引用扫清

- DROP：`user_organization`、`user_domain_role`、`user_global_role`、`user_account`
- 解绑 `business_domain.created_by/updated_by` 对 `user_account` 的 FK；语义改为 `staff_account.id`
- 审计/登录日志/域/ticket 等 JOIN 切到 `staff_account`
- 应用代码与 current mapper 无旧表引用（archive 历史迁移除外）
- 权限 path_pattern 从 `/iam/users*` 改为 `/admin/staff*`

### R5 Seed 与文档

- Seed：至少 1 个平台管理员（subject + staff + platform_admin 角色）
- 更新 `docs/architecture/data-model.md` §3.3；`docs/qa/implementation-traceability.md` 登记关闭

## Out of Scope

- 工单状态机配置接线运行时
- 平台端 vs 业务域端路由大搬迁
- `customer_account` 模型改造
- Auth `subjectId` 深度修复（可 follow-up）
- 生产无损迁移 / 历史 ID 映射
- 物理删除员工账号 API（前端本无入口）

## Acceptance Criteria

- [ ] AC1：应用代码 / current mapper 无 `user_account`、`user_domain_role`、`user_global_role`、`user_organization` 引用（archive 除外）
- [ ] AC2：`/api/v1/admin/staff*` 可完成列表、创建、更新、停用、离职、恢复、平台角色绑定、组织筛选
- [ ] AC3：`/api/v1/iam/users*` 不存在（404）
- [ ] AC4：AdminWeb 用户页与离职池走 staff API，创建员工成功
- [ ] AC5：权限快照域角色仅来自 `domain_member_role`
- [ ] AC6：staff 登录成功；离职后会话按现有 auth 版本策略失效
- [ ] AC7：空库 Flyway migrate 成功；上述路径冒烟通过
- [ ] AC8：`data-model.md` 过渡期表述收口；traceability 登记本项关闭

## Open Questions

（无阻塞项；实现细节见 `design.md` / `implement.md`）
