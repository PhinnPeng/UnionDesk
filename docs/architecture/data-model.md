# 数据库设计文档（UnionDesk）

| 版本 | 日期 | 说明 |
|---|---|---|
| V1.4 | 2026-04-30 | 同步平台管理端、登录验证码开关、离职字段与平台权限模型 |

---

## 1. 设计目标

> **文档定位（L4）**：逻辑实体、命名与隔离约定；与 [`product/foundation-rules.md`](../product/foundation-rules.md)（L3）同步。  
> **与 PRD**：[`product/prd.md`](../product/prd.md) §5 为概念简表；物理变更按 Sprint 登记 [`database-increment-plan.md`](./database-increment-plan.md)（L5），DDL 见 Flyway（L7）。  
> 下文表结构描述**以 Flyway 已落库为准**（含过渡期 `user_account` 等）；与 L3 目标态不一致时，以 L3 + Backlog 为准并登记偏差。

1. 支持多业务域隔离，统一使用 `business_domain_id` 作为域范围字段。
2. 支持平台管理端与业务端共用同一套 IAM、登录与审计体系。
3. 支持工单动态字段和业务域配置扩展。
4. 保证关键操作可追溯，核心管理动作都能落到审计日志。
5. MVP 保持单库可维护，后续模块可继续按 Flyway 追加迁移。

---

## 2. 设计约定

### 2.1 命名规范

- 表名使用小写下划线。
- 主键统一使用 `bigint unsigned`。
- 统一保留 `created_at`、`updated_at`，软删除场景再加 `deleted_at`。
- 需要表示生命周期状态的字段，优先使用 `status` / `employment_status` / `session_status` 这类枚举字符串。

### 2.2 类型规范

- 字符集统一 `utf8mb4`。
- 时间统一 `datetime(3)`。
- 动态扩展数据使用 `json`。

### 2.3 一致性约束

- 避免依赖 `NULL` 语义做唯一性判断。
- 平台级权限和业务域权限分开存放，但可以共用同一套角色与绑定机制。
- 审计字段优先保留，业务写操作尽量可追溯。

---

## 3. 核心实体

### 3.1 登录与会话

- `auth_login_config`：登录配置表，包含密码/用户名/邮箱/手机号开关、会话 TTL、最大会话数、验证码开关和提示文案。
- `auth_login_session`：登录会话表，记录会话状态、过期时间、刷新令牌摘要、客户端信息。
- `login_log`：统一登录日志表（`V202605230001`/`V202605230002`），记录登录成功/失败、失败原因、终端信息及 `client_code`/`sid`/`event_type` 等字段；原 `auth_login_log` 已废弃，应用仅写入本表。

### 3.2 用户与角色

- `user_account`：用户主表，当前需要保留登录名、手机号、邮箱、昵称、头像、状态、最近登录信息和离职信息。
- `platform_organization`：平台内部组织树，和 `business_domain` 分开维护，用于承接平台部门、负责人和组织排序。
- `role`：角色定义表，当前角色包含 `customer`、`agent`、`domain_admin`、`super_admin`、`platform_admin`、`security_auditor`；目标态中 `platform_admin` 为平台级最高权限，`super_admin` 作为业务域最高角色使用。
- `user_global_role`：全局角色绑定表。
- `user_domain_role`：业务域角色绑定表。
- `customer_business_domain_access`：客户业务域可见/申请关系表。

### 3.3 权限与平台菜单

- `iam_permission`：权限元数据表，区分 `platform` / `domain` 范围。
- `iam_role_permission`：角色权限关系表。
- `iam_role_binding`：统一角色绑定表，用于表达全局角色和业务域角色的生效范围。
- `iam_admin_menu`：Admin 菜单树，目录 / 菜单 / 按钮同表。
- `iam_admin_role_menu_relation`：Admin 角色菜单关系表。

### 3.4 业务域与工单

- `business_domain`：业务域主表。
- `consultation_session`：咨询会话主表。
- `consultation_message`：咨询消息明细。
- `consultation_ticket_link`：咨询转工单映射。
- `ticket_type`：工单类型。
- `custom_field_config`：动态字段配置。
- `ticket`：工单主表。
- `ticket_reply`：工单回复。
- `ticket_event_log`：工单事件流。

### 3.5 反馈、通知与审计

- `feedback`：反馈/建议生命周期（**MVP 不使用**；反馈/建议经 `ticket` + `ticket_type` 预置交付，见 [`backlog-epics.md`](../product/backlog-epics.md) §1.3）。
- `notification_template`：通知模板，支持全局或业务域级配置。
- `operation_log`：管理操作审计日志。

---

## 4. 关键索引

1. `user_account`：`username`、`mobile`、`email`、`status`、`employment_status`
2. `platform_organization`：`parent_id, order_no, id`、`status`
3. `auth_login_session`：`user_id, session_status, expires_at`
4. `login_log`：`sid, created_at`、`subject_id, created_at`、`event_type, created_at`、`client_code, created_at`
5. `iam_permission`：`permission_scope, status`、`http_method, path_pattern`
6. `iam_role_binding`：`user_id, status`、`role_id`、`business_domain_id`
7. `iam_admin_menu`：`parent_id, order_no, id`、`node_type, status`、`route_path` 唯一、`permission_code` 唯一
8. `custom_field_config`：`business_domain_id, ticket_type_scope_id, field_key`
9. `ticket`：`business_domain_id, status, priority, created_at`
10. `consultation_session`：`business_domain_id, session_status, updated_at`
11. `operation_log`：`business_domain_id, module, created_at`、`operator_user_id, created_at`、`request_id`

---

## 5. 当前实现对齐说明

1. 当前前端路由名使用单数形式 `/system/user`、`/system/role`、`/system/menu`、`/system/dept`。
2. 数据库和历史迁移里保留了部分旧的菜单种子命名和预留项，后续需要统一口径。
3. 当前前端入口可见性仍按管理员角色快照控制，但数据库里已经准备好 `platform.*` 权限模型。
4. `system/user` 和 `system/dept` 目前仍是骨架页，文档不要把它们写成成品页。
5. `platform_organization` 已补正式表结构并提供平台组织只读接口，当前还没有把 `user_account` 和组织归属关系打通。
6. 导入导出、公告、日志、屏蔽词、知识库目前还没有正式表结构，后续再按模块追加。

---

## 6. 初始化与迁移

- Flyway 迁移脚本维护当前 schema 最终态，只允许追加新版本，不允许修改已发布脚本。
- 旧版 DDL 文件已删除，当前 schema 最终态由 Flyway 迁移脚本维护。
- 如果需要调整菜单或权限口径，优先在文档里写清楚现状，再补迁移脚本。

---

## 7. 与前端和文档的同步规则

- 平台管理端设计文档负责描述入口、模式切换和模块边界。
- `README.md` 负责列出文档索引和当前同步状态。
- Flyway 迁移脚本负责最终态 DDL，`数据库设计.md` 负责解释为什么是这个最终态。
- 有实现、有迁移、无文档时要补文档；有文档、无实现时要明确标注为预留。
