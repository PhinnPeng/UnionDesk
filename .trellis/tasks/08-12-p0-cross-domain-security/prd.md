# P0 安全债：US-S1-08 目标域校验与审计补齐

> 父任务：`08-11-group-role-management`（集团统一角色与跨域批量权限管理）的**硬前置**（D8）。
> 背景：模板/批量 API 是**跨域写放大器**，未修复前不开放（design.md §2/§8）。

## Goal

消除跨域越权漏洞（US-S1-08，2026-08-11 提级 P0）并补齐审计，为集团统一角色/跨域批量停用提供安全地基：**A 域管理员仅能操作自己授权域集内的资源，任何跨域写拒绝；所有权限敏感操作逐域落审计、可区分操作端**。

## 现状与证据

- 拦截器不校验 URL 目标域：`RequirePermissionInterceptor.java:38`（research/dual-console-permission-design-review.md R1）
- service 层仅查域存在，不校验操作人授权域集；`IamService.hasPermissionForDomains(operator, code, domainIds)` **已具备参数能力，未启用**
- US-S1-08 AC：A 域身份访问 B 域数据返回 403 或空集（docs/product/backlog-stories.md §US-S1-08）
- 审计缺口（R4）：成员增删/改角色、客户操作、域角色 CRUD 不落 `audit_log`；`audit_log` 无 console 维度，无法区分哪端操作

## Requirements

- R1（目标域校验）URL/请求上下文解析目标 `businessDomainId` → 操作前逐域校验 `hasPermissionForDomains`；未授权 → 403 + 中文
- R2（平台豁免）平台管理员（global super_admin / platform_admin 按现有授权语义）批量跨域操作正常通过；豁免路径必须显式声明
- R3（审计补齐）成员增删/改角色、域角色 CRUD 落 `audit_log`；`audit_log` 增**操作入口端点列 `point`**（值 platform/domain，可空默认 null 兼容存量 `business_domain_id=0L` 写入，2026-08-12 定名）；批量操作逐域写 N 行
- R4（回归安全）既有单域操作行为不变（鉴权语义仅强化，不收紧合法路径）
- R5（step-up 真实校验，2026-08-12 Q2 决策纳入本子任务）`LoginSessionService.validateStepUpToken` 接通存量 `StaffController.updatePlatformRoles` 与新 `batch-status` 端点；无效/过期/缺失 → 403 + 中文

## Acceptance Criteria

- [ ] AC1 A 域 domain_admin 调用 B 域资源（读/写）→ 403 + 中文提示；读取型接口按 FR-02 语义拒绝或返回空集
- [ ] AC2 平台管理员批量跨域（平台码路径）→ 正常通过，无回归
- [ ] AC3 成员增删/改角色、域角色 CRUD 均落 `audit_log`（含操作入口维度列，platform/domain 可区分）
- [ ] AC4 既有单域操作全部通过回归用例（登录/工单/客户/成员等冒烟）
- [ ] AC5 模板/批量 API 在 P0-② 完成后才被允许开放（父任务 P1 门禁）
- [ ] AC6 step-up 令牌真实校验生效：缺失/无效/过期 → 403 + 中文；有效令牌放行（updatePlatformRoles + batch-status 两端点四态测试通过）

## Out of Scope

- 模板层 / 批量停用 API 本身（父任务 P1）
- 角色双轨治理（父任务 P3）
- 组织树 / 其他鉴权模型重构

## 参考证据

- 父任务：`.trellis/tasks/08-11-group-role-management/{prd,design}.md`
- `research/dual-console-permission-design-review.md`（R1/R3/R4 证据与 file:line 锚点）
- `docs/product/foundation-rules.md` FR-02；`docs/product/backlog-stories.md` US-S1-08
