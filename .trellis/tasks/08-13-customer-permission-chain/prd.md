# 客户授权链修复：客户提单/工单/站内信全链路打通

> 2026-08-12 立项（P0，独立于 P0 安全债并行推进）。评审依据：`research/dual-console-permission-design-review.md`、三份勘察报告（共享 API 架构评审 / 数据范围与端隔离验证）。

## Goal

打通客户（CustomerWeb，角色 customer）的授权链路：**客户提单、我的工单（列表/详情/回复/撤回）、站内信、附件上传下载从全线 403 变为可用**，同时保证数据范围（客户仅查自己）与域隔离（未入域 403）正确。确立架构红线：数据范围一律用「码后缀 + 路径分工」表达，禁止同一端点按身份动态过滤。

## 现状与根因（证据已闭合）

- **授权查询断裂（根因 1）**：`IamPermissionMapper.xml selectEffectiveGrants`（:26-59）仅两个 UNION 分支（staff 平台角色 / staff 域成员），**无客户分支** → 客户账号在任何 `@RequirePermission` 端点恒 403。`RoleMapper.selectUserRoleCodesByClientOther`（:148-169）同样只查员工表 → 登录角色解析也只给兜底 "customer"，无授权行
- **shared 码策略缺失（根因 2）**：`PermissionScopePolicy.canRoleOwnPermission`（:24-40）只认 platform/domain 两档；`ticket.create/view.self/reply.self/withdraw.self`、`inbox.*`、`attachment.*` 共 16 个 shared 码（`AdminPermissionCatalog` scope="shared"）**任何角色不可拥有** → 即使授权查询修复也会被策略拒绝
- **service 归属校验缺口（根因 3，修复 1/2 后会暴露的同域越权）**：`TicketService.getTicketDetail`/`replyTicket` 无归属校验（对比 `withdrawCustomerTicket` 有 `customerId() != context.userId()` :313-315）；`createCustomerTicket` 不校验客户是否属于该业务域（仅 `loadDomain()` 域存在性 :117）；`AttachmentService.resolveDownloadAccess` 无归属/域校验
- 客户角色种子授权已存在（`iam_role_permission`：ticket.create/view.self/reply.self/withdraw.self、attachment.*、inbox.*、domain.read），**缺的是解析到客户头上的查询路径**
- `customer_account`（rebaseline:269）+ `domain_customer`（:313，客户入域关系，status/source/activated_at 齐全）是客户模型主表；`iam_role_binding`/`customer_business_domain_access` 为废弃表（FK 指向已拆除的 `user_account`），**不采用**

## Requirements

- R1（授权查询）`selectEffectiveGrants` 增加**客户分支**：`customer_account → domain_customer(active) → customer 角色 → iam_role_permission → iam_permission`，按 `business_domain_id` 出域维度授权行（与员工 `domain_member` 分支同构）；`RoleMapper` 客户角色解析同步
- R2（shared 码策略）`PermissionScopePolicy.canRoleOwnPermission` 增加 shared 档：**域角色可持有 shared 码**（码不以 `platform.` 开头）；`isPermissionEffective` 对 shared 码走与 domain 码相同的「域绑定生效」判定（同域 true，异域 false）
- R3（service 归属校验补缺）：
  - `TicketService.getCustomerTicketDetail`/`replyCustomerTicket`：强制 `customerId == context.userId()`（与撤回同规）
  - `createCustomerTicket`：校验当前客户在该 `business_domain_id` 存在 `domain_customer(active)`（FR-05：未入域 → 403 + 中文）
  - `AttachmentService`：下载/上传校验归属或域维度（按工单/客户关联）
- R4（架构红线，写入 design）数据范围一律用「码后缀（`.self`/`.domain_all`）+ 路径分工 + service 过滤」表达；**禁止同一端点按身份动态过滤**；新模块（如案例库 `case.view.domain_all` 授 customer）按此模式扩展

## Acceptance Criteria

- [ ] AC1 客户提单：`POST /domains/{id}/tickets` → 201（此前 403）；未入域客户 → 403 + 中文（FR-05）
- [ ] AC2 我的工单：列表/详情/回复/撤回全通；详情/回复**仅本人数据**（跨用户访问 → 403 或 404）；列表只含本人工单
- [ ] AC3 站内信/附件：`inbox.read/mark_read`、`attachment.upload/download` 客户可用；附件下载无越权（他人附件 → 拒绝）
- [ ] AC4 客户数据范围：customer 无 `view.domain_all` 码 → 调 `/admin/**` 工单端点仍 403（规则 2 不回退）
- [ ] AC5 员工侧回归：agent/domain_admin 工单队列/处理、域成员/角色管理不受影响（既有测试全绿）
- [ ] AC6 跨域与未入域：客户访问未授权域端点 → 403（FR-02/FR-05）；`TicketLifecycleIntegrationTest`（5 用例）修复后全绿
- [ ] AC7 红线落地：design 记录数据范围架构红线；无新增"按身份动态过滤"端点

## Out of Scope

- 案例库等未来共享模块（机制已支持，按 R4 模式扩展）
- 端级路径白名单（Q2-B，暂缓评估）
- 数据权限框架/DataScope 插件（红线禁止）
- `iam_role_binding` 复活或迁移（废弃表，不采用）
- 员工端 shared 码授予调整（agent 保持无 self 码的现状，如需另行决策）

## 参考证据

- `UnionDesk/uniondesk-iam/src/main/resources/mapper/iam/IamPermissionMapper.xml`（selectEffectiveGrants :26-59）
- `UnionDesk/uniondesk-iam/src/main/java/com/uniondesk/iam/core/PermissionScopePolicy.java`（:24-40）
- `UnionDesk/uniondesk-ticket/src/main/java/com/uniondesk/ticket/core/TicketService.java`（:117/:257-307/:310-315/:404-407）
- `UnionDesk/uniondesk-app/src/main/resources/db/migration/current/V202605200002__rebaseline_current_schema.sql`（customer_account :269 / domain_customer :313 / iam_role_permission 种子 :1629）
- `docs/product/foundation-rules.md`（FR-02/FR-05，§3.3 数据范围）
- 关联：`08-12-p0-cross-domain-security`（逐域校验已落地，本任务依赖其 domainIdParam 机制）
