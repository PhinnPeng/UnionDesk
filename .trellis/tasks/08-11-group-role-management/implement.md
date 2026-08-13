# Implement — 集团统一角色与跨域批量权限管理

> 执行依据：`prd.md`（R-A~R-D、D1–D9、AC1–AC7）+ `design.md`（§3 数据模型 / §5 API / §8 阶段）。
> 前置硬约束（D8）：**P0 安全债（US-S1-08 目标域校验）完成前，模板/批量 API 不开放**。

## 阶段总览

| 阶段 | 内容 | 依赖 | AC |
|:---|:---|:---|:---|
| **P0-① 安全债** | US-S1-08 目标域校验（拦截器 + service 逐域鉴权） | — | AC4 |
| **P0-② 审计补齐** | 审计事件 + 操作入口端点列 `point`；step-up 真实校验接通（validateStepUpToken，含存量 updatePlatformRoles） | — | AC7 |
| **P1-1 模板层** | 三表 Flyway + 模板 CRUD/apply/sync/unapply/bind-members + F4.9 模板 Tab | P0-① | AC1/AC2/AC5/AC6 |
| **P1-2 批量停用** | batch-status API + F4.7 跨域入口（step-up） | P0-① | AC3/AC7 |
| **P2 域端展示** | F3.13/F3.3 模板徽标、锁定只读、漂移提示 | P1-1 | AC5/AC6 |
| **P3 后置** | 双轨治理、异步队列、版本历史 | — | 不阻塞 MVP |

> P0 两子项独立成任务（`08-11-...` 子任务），P1–P3 在本任务推进；P0 完成前 P1 仅可做设计/建表准备，不开放 API 与 UI 入口。

## 执行清单

### P0-① 目标域校验（子任务，US-S1-08）

1. 定位 `RequirePermissionInterceptor` 与 service 层 `requireDomain` 调用链 → 验证 [检查] `UnionDesk/` 后端包结构（spec: backend/directory-structure.md）
2. 实现：URL 目标域解析（路径/参数）→ `IamService.hasPermissionForDomains(operator, code, domainIds)` 校验 → 拒绝返回 403 + 中文（spec: backend/error-handling.md）
3. 平台管理员批量跨域豁免路径确认（platform.* 码 + global 角色）
4. 验收：A 域管理员操作 B 域 → 403；平台管理员跨域通过；既有单域操作回归不破坏

### P0-② 审计补齐（子任务）

5. `AuditLogSemanticsListener` + `AuditActionCodes` 扩展：成员增删/改角色、域角色 CRUD、模板操作、批量停用 → 验证 [检查] spec: backend/logging-guidelines.md（审计 vs 日志）
6. `audit_log` 增**操作入口端点列 `point`**（值 platform/domain，可空默认 null 兼容存量 `business_domain_id=0L` 写入；2026-08-12 定名），批量操作逐域写 N 行
7. **step-up 真实校验接通**：`LoginSessionService.validateStepUpToken` 接入 `StaffController.updatePlatformRoles`（存量）+ 新 `batch-status` 端点；无效/过期/缺失 → 403 + 中文；补测试（缺失/无效/过期/有效四态）

### P1-1 模板层（本任务）

7. Flyway 新版本号：`role_template` / `role_template_permission` / `role_template_domain` 建表 + `domain_role` 增列（spec: backend/database-guidelines.md：无 FK、命名规范）
8. Service：模板 CRUD + apply（逐域事务 TR-04 部分成功，复用 `DomainRoleService.createRole`）+ sync/unapply + bind-members（复用 `StaffAccountService.bindDomainMemberships`）；满额域校验（≤20）统一补在 apply 与域端创建双入口
9. 权限码：`platform.role_template.{read,create,update,delete,apply,sync}`、`platform.user.domain_batch_status`（PermissionCodes.java + AdminPermissionCatalog + 前端 platform-domain-permissions.ts 三处同步）
10. 前端 F4.9 平台角色页「模板」Tab：列表/创建/推送选域 Modal/同步/漂移状态列（spec: frontend/component-guidelines.md；列表页骨架见 AGENTS.md §2.7）

### P1-2 批量停用（本任务）

11. API `POST /api/v1/admin/staff/{staffId}/domain-members/batch-status`：逐域事务、部分成功语义、逐域审计
12. step-up 二次认证前置：复用 `POST /auth/step-up` + StepUpModal + `X-UD-Step-Up-Token`；**接通 `validateStepUpToken` 真实校验**（新 batch-status + 存量 StaffController.updatePlatformRoles 同规）
13. 前端 F4.7 用户管理/F4.3 离职池：行内「跨域批量停用」入口（复用 `detail-members.tsx` rowSelection 模式）

### P2 域端展示

14. F3.13 域端角色页 + F3.3：模板来源/版本徽标、锁定字段只读禁用、manual 漂移提示（非锁定可编辑，后端校验锁定）

### P3 后置（不阻塞）

15. 双轨治理文档化落地；immediate 异步队列；模板版本历史

## 验证命令

```bash
# 后端
cd UnionDesk && mvn -q compile                                  # 编译
mvn -q test -Dtest=<新测试类>                                   # 目标域校验/锁定字段/批量语义单测
# 前端
cd UnionDeskWeb && pnpm -C apps/UnionDeskAdminWeb typecheck      # 管理端类型检查
# 冒烟（人工）
#   A 域管理员操作 B 域 → 403 中文提示；模板 apply 多域 → 各域实例生成；批量停用 → step-up 确认 + 部分成功摘要
# 质量门
python ./.trellis/scripts/task.py validate <task>               # jsonl 上下文校验
```

## 风险文件 / 回滚点

| 文件/位置 | 风险 | 对策 |
|:---|:---|:---|
| `RequirePermissionInterceptor` / IAM service | 鉴权行为变化波及全部既有接口 | P0-① 独立可回滚（拦截器开关）；回归单域操作全量用例 |
| `DomainRoleService` / `StaffAccountService` | 复用点耦合 | 模板逻辑独立新类，不侵入既有方法；apply 逐域事务 |
| `audit_log` 表结构（console 列） | 既有审计写入兼容 | 新列可空默认值；listener 兼容旧事件 |
| Flyway 迁移 | 三表 + 增列 | 反向迁移脚本随任务提交；非破坏（可空） |

## start 前检查（follow-up checklist）

- [ ] prd.md 已收敛（AC1–AC7 + Out of Scope），design.md / implement.md 齐全
- [ ] implement.jsonl / check.jsonl 含真实 spec/research 条目（非种子行）
- [ ] P0 子任务已立项并关联（add-subtask）
- [ ] 最终规划总结已获用户明确批准
