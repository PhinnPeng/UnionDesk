# Design — 客户授权链修复（三层 + 架构红线）

> 依据：prd.md（R1–R4、AC1–AC7）+ 勘察证据（selectEffectiveGrants 无客户分支 / shared 码策略缺失 / service 归属缺口）。
> 依赖：`08-12-p0-cross-domain-security` 的 `domainIdParam` 逐域校验已就位（拦截器已支持按路径变量域校验）。

## 1. 架构红线（先立规则，R4）

**数据范围一律用「码后缀 + 路径分工 + service 过滤」三层表达，禁止同一端点按身份动态过滤。**

- 码后缀语义：`.self` = 仅本人数据（如 `ticket.view.self`）；`.domain_all` = 域内全部（如 `ticket.view.domain_all`、未来 `case.view.domain_all`），**不限定角色**——客户持有即客户可看域内全部
- 路径分工：客户入口 `/api/v1/domains/{id}/.../my/**` 与员工入口 `/api/v1/admin/domains/{id}/...` 分离，各挂对应码
- 新模块（案例库等）按此模式扩展：新码 `case.view.domain_all` 授 customer/agent/domain_admin + 按 `business_domain_id` 过滤 + 逐域校验，零机制改动
- 禁止：同一端点运行时按 role 切换过滤条件（避免引入 DataScope 框架）

## 2. 修复层一：授权查询客户分支（R1）

**`IamPermissionMapper.xml selectEffectiveGrants` 增加第三个 UNION 分支**（与 `domain_member` 分支同构）：

```sql
UNION ALL
SELECT
    'domain' AS role_level,
    'domain' AS binding_scope,
    dc.business_domain_id,
    p.permission_scope
FROM customer_account ca
JOIN domain_customer dc ON dc.customer_account_id = ca.id
    AND dc.status = 'active' AND dc.deleted_at IS NULL
JOIN role r ON r.code = 'customer' AND r.scope = 'domain'
JOIN iam_role_permission rp ON rp.role_id = r.id
JOIN iam_permission p ON p.id = rp.permission_id
WHERE ca.id = #{userId}
  AND p.status = 1
  AND p.code IN (codes)
```

- 角色绑定落点：**customer 角色（scope=domain）静态授予**（种子已授 self 码），客户入域即生效——不需要客户级角色绑定表（MVP 客户仅 customer 角色；未来客户细分角色再引入 `domain_customer_role` 或扩展列，本任务不建）
- `RoleMapper.selectUserRoleCodesByClientOther`（:148-169）：增加客户分支（`customer_account → domain_customer` 解析角色码，登录响应角色不为空）
- 判定链验证：`PermissionScopePolicy.isPermissionEffective` 对 domain scope 码 + 域角色 + target=domainId → 同域 true（P0 拦截器已按 `domainIdParam` 传 target）→ 客户提单/my 工单放行

## 3. 修复层二：shared 码策略档（R2）

**`PermissionScopePolicy.canRoleOwnPermission`** 增加 shared 档：

```java
// 域角色：可拥有 domain 码（现有）+ shared 码（新增）
if (!ROLE_LEVEL_DOMAIN.equals(normalizedRoleLevel)) { return false; }
if (PERMISSION_SCOPE_DOMAIN.equals(normalizedPermissionScope)) {
    return normalizedCode == null || !normalizedCode.startsWith("platform.");
}
if (PERMISSION_SCOPE_SHARED.equals(normalizedPermissionScope)) {
    return normalizedCode == null || !normalizedCode.startsWith("platform.");
}
return false;
```

- `isPermissionEffective`：shared 码走与 domain 码相同的「域角色 + 同域绑定」分支（现有代码域码分支 `ROLE_LEVEL_DOMAIN.equals(roleLevel) && ROLE_LEVEL_DOMAIN.equals(bindingScope)` 已覆盖 shared 授予——验证 grant.roleLevel 为 domain、binding 为 domain 即可，无需新分支；**实现时用测试确认**，如不覆盖则补 shared 分支）
- global 角色（super_admin）不因 shared 档获得更多权限（global 分支保持仅 platform 码）

## 4. 修复层三：service 归属校验补缺（R3）

| 方法（TicketService / AttachmentService） | 缺口 | 修复 |
|:---|:---|:---|
| `getCustomerTicketDetail` → `getTicketDetail`（:374-382） | 无归属校验 | 加 `requireCustomer(context)` + `ticket.customerId == context.userId()` 否则 403（与 `withdrawCustomerTicket` :313-315 同规） |
| `replyCustomerTicket` → `replyTicket`（:257-307） | 无归属校验 | 客户路径（sender_type=customer）强制 `ticket.customerId == context.userId()` |
| `createCustomerTicket`（:117） | 不校验客户入域 | 加 `domainCustomerService` 存在校验（`domain_customer` active）：未入域 → 403 + 中文（FR-05） |
| `AttachmentService.resolveDownloadAccess`（:149-155） | 无归属/域校验 | 按附件关联（工单/上传者）校验：客户仅能下载其工单附件（或本人上传）；员工按域 |

- 错误语义：403 + 中文（`ErrorCodes.FORBIDDEN.message()`），与 FR-01/FR-05 一致
- 注意：`getTicketDetail`/`replyTicket` 是双端共用方法——**按入口路径拆分或加参数区分**：客户入口强制归属，员工入口维持现状（域校验已由拦截器保证）。推荐：控制器层分开（`getCustomerTicketDetail` 加校验后调共用方法；`replyCustomerTicket` 同理），避免在共用方法内按 role 动态判断（红线）

## 5. 兼容性与回滚

- R1/R2 为**权限判定放宽**（客户从恒 403 → 按域可用），员工/平台路径判定不变（客户分支仅客户账号命中；shared 档仅影响 shared 码，员工域码/平台码判定不变）
- R3 为**收紧**（客户数据范围强制归属）——修复 1/2 上线前必须完成，否则同域越权暴露
- 回滚：R1/R2 单点（SQL 分支/策略方法）；R3 随 ticket service 可单独回滚
- 既有测试影响：`TicketLifecycleIntegrationTest`（5 用例，此前 403/断言失败）修复后应全绿；`SlaRuleCrudIntegrationTest`（3 用例，global 角色访问域码端点）**不在本任务范围**（测试身份缺陷，另行处理）

## 6. 风险与对策

| 风险 | 对策 |
|:---|:---|
| 客户分支 SQL 与员工分支 userId 空间冲突 | 客户/员工账号 ID 空间分离（customer_account vs staff_account），UNION 分支各自 WHERE 明确表，测试覆盖双端登录 |
| shared 档放宽误伤员工（agent 获得 self 码效果） | 授权仍由角色种子决定：agent 无 self 码（本任务不改种子）；super_admin 走 global 分支不受影响 |
| R3 漏改某客户方法 | 全量 grep 客户路径端点（`/domains/{id}/` 无 `/admin/` 前缀）逐一核对归属校验 |
| 修复后客户功能回归 | 客户端冒烟：注册→登录→提单→我的工单→回复→撤回→站内信→附件 |
