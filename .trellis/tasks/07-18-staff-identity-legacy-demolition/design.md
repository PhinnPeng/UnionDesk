# 设计：员工身份遗留表物理拆除

权威副本亦见：`docs/superpowers/specs/2026-07-18-staff-identity-legacy-demolition-design.md`

## 1. 目标态与边界

### 1.1 真相源

| 概念 | 唯一真相源 | 禁止 |
|:---|:---|:---|
| 自然人 | `identity_subject` | 用账号表冒充主体 |
| 员工账号 | `staff_account` | 读写 `user_account` |
| 平台角色 | `staff_account_platform_role` | `user_global_role` |
| 入域 | `domain_member` | 旧 user_id 入域 |
| 域角色 | `domain_member_role` | `user_domain_role` |
| 组织归属 | `staff_organization` | `user_organization` |

### 1.2 迁移策略（α）

```text
① 代码切齐（后端 + 前端，移除旧 users API）
② 联调库允许清空
③ Flyway DROP 旧表 + seed
④ 冒烟：登录 → 用户 CRUD → 入域角色 → 权限快照
```

无双写窗口。

## 2. API 映射与模块职责

### 2.1 契约

| 旧 | 新 |
|:---|:---|
| `GET /api/v1/iam/users` | `GET /api/v1/admin/staff` |
| `POST /api/v1/iam/users` | `POST /api/v1/admin/staff` |
| `PUT /api/v1/iam/users/{id}` | `PUT /api/v1/admin/staff/{staffId}` |
| `POST .../offboard` | `POST /api/v1/admin/staff/{id}/offboard`（补齐） |
| `GET .../offboard-pool` | `GET .../staff?status=offboarded` 或专用 pool |
| `POST .../restore` | `POST /api/v1/admin/staff/{id}/restore` |
| 平台角色 | `PUT/GET .../platform-roles` |

`IamController` 保留 menus/roles/permissions/me；删除 `/iam/users*`。

### 2.2 职责

- `StaffController`：平台员工唯一 HTTP 入口
- `StaffAccountService`：生命周期 + `domain_member(_role)` 绑定
- `PlatformRoleService`：`staff_account_platform_role`
- `IdentitySubjectService`：手机号 → subject
- `IamService`：移除 UserAccount CRUD 与 `user_domain_role` 写入

### 2.3 能力缺口（实现时补齐）

1. 离职语义：`offboarded` 与 `disabled` 分离；字段 `offboarded_at` / `by` / `reason`
2. `staff_organization` + 列表 `organizationId` 筛选
3. 前端 `iam.ts` 与页面适配
4. 展示 JOIN 统一 `LEFT JOIN staff_account`
5. `AdminPermissionCatalog` + Flyway 更新 path_pattern

## 3. 数据拆除、Seed、回滚

### 3.1 DROP 前

- DROP `business_domain` 对 `user_account` 的 FK；列语义改为 staff id
- 新建 `staff_organization`；DROP `user_organization`
- 清理残留 `trg_user_*` 触发器（若有）
- 更新 `iam_permission.path_pattern`

### 3.2 DROP 清单

新建：`staff_organization`；staff 离职字段（若缺）。

删除顺序：`user_organization` → `user_domain_role` → `user_global_role` → `user_account`。

### 3.3 Seed

1 个 subject + 1 个平台管理员 staff + `platform_admin` 绑定；（可选）测试域成员。

### 3.4 回滚

- 未 DROP：Git revert
- 已 DROP：重建库重跑迁移（不提供行级回滚）

## 4. 波次

| 波次 | 内容 |
|:---|:---|
| W1 | Staff offboard + staff_organization + 权限 path |
| W2 | 前端切 staff；删除 `/iam/users*` |
| W3 | JOIN 切源；扫清旧表引用 |
| W4 | Flyway DROP + seed；文档；冒烟 |

## 5. 风险

- `business_domain.created_by` 等历史 id 在清空重建后无意义；可接受
- Staff 列表内存分页：本轮不优化，保持现行为
- 遗漏 JOIN 导致运行期 SQL 失败：W3 用全仓 grep 门禁
