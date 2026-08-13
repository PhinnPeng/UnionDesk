# Implement — 客户授权链修复

> 执行顺序：R2（策略）→ R1（授权查询）→ R3（归属校验，须在 1/2 前或同步完成）→ 测试。R3 必须在 R1/R2 生效前完成，避免同域越权窗口。

## 执行清单

### 第 1 步：R2 shared 码策略（PermissionScopePolicy）

1. `PermissionScopePolicy.canRoleOwnPermission` 增加 shared 档（域角色可持有 shared 码，码不以 `platform.` 开头）→ 验证 [检查] `PermissionScopePolicyTests`（新增：域角色可持有 shared 码、global 角色仍不可、domain 码行为不变）
2. 确认 `isPermissionEffective` 对 shared 码授予（roleLevel=domain, bindingScope=domain, permissionScope=shared）走现有域码分支生效；若不覆盖则补 shared 分支 + 测试

### 第 2 步：R1 授权查询客户分支

3. `IamPermissionMapper.xml selectEffectiveGrants` 增加客户 UNION 分支（customer_account → domain_customer active → role customer(scope=domain) → iam_role_permission → iam_permission）→ 验证 [检查] 单测/集成：客户账号查码返回授权行（含 business_domain_id）
4. `RoleMapper.selectUserRoleCodesByClientOther` 增加客户角色解析分支（登录响应角色非空）→ 验证 [检查] 客户登录接口返回角色 customer

### 第 3 步：R3 service 归属校验补缺（与 1/2 同批完成）

5. `TicketService`：`getCustomerTicketDetail`/`replyCustomerTicket` 强制 `customerId == context.userId()`（控制器层分开调用，共用方法内不做按 role 动态判断）→ 验证 [检查] 跨用户访问 → 403/404
6. `createCustomerTicket`：校验 `domain_customer(active)` 存在（未入域 → 403 + 中文 FR-05）→ 验证 [检查] 未入域客户提单 → 403
7. `AttachmentService`：下载校验归属（客户仅本人上传/其工单附件；员工按域）→ 验证 [检查] 他人附件 → 拒绝

### 第 4 步：测试与回归

8. 修复/新增测试：
   - `TicketLifecycleIntegrationTest`（5 用例）→ 应全绿（此前 403）
   - 新增客户全链路集成测试（注册→登录→提单 201→我的工单列表/详情→回复→撤回→跨用户 403→未入域 403）
   - `PermissionScopePolicyTests` + `IamPermissionMapper` 客户分支测试
9. 回归：员工侧（DomainMember/DomainRole/SLA/审计既有测试）+ 平台侧 + P0 安全债测试（DomainScopedPermissionIntegrationTest 等）全部通过
10. 冒烟：客户端（CustomerWeb）注册→登录→提单→我的工单→回复→站内信→附件；管理端工单队列/处理正常

## 验证命令

```bash
cd F:/WorkSpace/UnionDesk/UnionDesk
./mvnw.cmd -q test -pl uniondesk-app -am -Dtest=<新/受影响测试类> -Dsurefire.failIfNoSpecifiedTests=false
# 全量回归
./mvnw.cmd -q test -pl uniondesk-app -am
```

## 风险文件 / 回滚点

| 文件 | 风险 | 对策 |
|:---|:---|:---|
| `IamPermissionMapper.xml` | 客户分支 SQL 影响员工授权查询 | UNION 分支独立 WHERE；既有员工测试全量回归 |
| `PermissionScopePolicy.java` | shared 档放宽 | 仅 shared scope 码受影响；域码/平台码判定不变；单测覆盖 |
| `TicketService.java` | 共用方法被双端调用 | 控制器层分开（客户入口加强校验），共用方法不动 |
| 种子授权 | customer 角色码不足 | 本任务不改种子（已授 self 码）；若发现缺失按现状补充并记录 |

## start 前检查

- [ ] prd.md（R1–R4 / AC1–AC7）/ design.md / implement.md 齐全
- [ ] implement.jsonl / check.jsonl 含真实 spec/research 条目
- [ ] 依赖 `08-12-p0-cross-domain-security` 的 domainIdParam 已落地（已完成）
- [ ] 用户已批准方案（2026-08-12 确认）
