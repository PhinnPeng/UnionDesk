# 集团统一角色与跨域批量权限管理（模板 + 实例 + 同步策略）

## Goal

解决企业集团管理痛点：平台统一管理用户与角色，支持「一次创建角色应用到多个业务域」「一次停用一个员工在多个业务域的权限」，同时保留各域精细化控制。实施前必须修复跨域越权安全债（US-S1-08 目标域校验）。

## 用户原始需求（2026-08-11 确认）

- R-A 平台统一管理用户（全局员工库 + 跨域视图）
- R-B 各业务域角色统一：平台一次性创建一个角色并应用到多个业务域
- R-C 一次性关闭一个员工在多个业务域的权限（批量停用）
- R-D 同时保留按域精细化控制（域内实例可微调，但受锁定约束）

## 已确认决策（Brainstorm 2026-08-11）

- D1 **模型 3：统一模板 + 域实例 + 覆盖 + 同步策略**（研究结论推荐，非共享角色模型）
  - 数据：新增 `role_template` / `role_template_permission` / `role_template_domain`；`domain_role` 增 `template_id / template_version / locked_fields`
  - 语义：模板 apply 到多个域 → 生成各域 `domain_role` 实例（与现有每域实例结构同构）
- D2 同步策略默认 **immediate（立即同步）**，后补手动/不同步选项
- D3 锁定字段白名单默认 **锁权限包**（名称/成员可微调）
- D4 满额域（≤20 自定义角色）冲突策略：**跳过 + 提示**
- D5 IAM `role(scope=domain)` 双轨：**冻结**（新角色走模板/域端，旧角色只读保留）；`domain_role` = 运行时业务角色、IAM domain 角色 = 控制台权限角色（S4 双轨关系按此文档化）
- D6 批量停用域集：先手选域（不做组织预筛）
- D7 跨域批量停用启用 **step-up 二次认证**（与域删除同级）；**接通真实令牌校验**（`LoginSessionService.validateStepUpToken`，含存量 `updatePlatformRoles` 一并修复，2026-08-12 验证确认）
- D8 **P0 前置安全债**：US-S1-08（目标域校验）+ 审计补齐，模板/批量 API 上线前必须完成（跨域写放大器）
- D9 职责矩阵（新目标态）：平台 = 统一管人 + 模板创建/下发/同步 + 跨域批量停用 + 审计监管；域 = 成员日常运营 + 实例微调（锁定字段除外）；冲突以「锁定字段 + 同步策略」裁决

## 批量 API 形态（设计骨架，待 design.md 细化）

- `POST /api/v1/iam/role-templates`（建模板）
- `POST /api/v1/iam/role-templates/{id}/apply`（body: `domain_ids[]`）
- `POST /api/v1/admin/staff/{staffId}/domain-members/batch-status`（body: `domain_ids[]`, status='disabled'，TR-04 部分成功语义；权限码 `platform.user.domain_batch_status`，2026-08-12 决策改名——全仓员工码统一 `platform.user.*`）

## 迁移路径（分阶段）

- P0（前置，独立子任务）：US-S1-08 目标域校验 + 审计补齐
- P1：模板层（表 + CRUD + apply）+ 批量停用 API
- P2：域端微调展示（模板来源 + 锁定字段）
- P3：后置项（双轨治理、异步队列、版本历史）

## 参考证据

- 研究文件：`research/group-role-management-model.md`（本任务目录）
- 现状：`docs/product/feature-list.md` §8 权限码对照表（F3.13/F4.9）、`docs/architecture/data-model.md`、`foundation-rules.md`
- 方案 D（平台只读）已作废，以本文档为准

## Acceptance Criteria

- [ ] AC1（P1-1）模板 CRUD：`role_template` / `role_template_permission` / `role_template_domain` 三表建表（Flyway）；创建模板含权限包 + `locked_fields` + `sync_strategy`
- [ ] AC2（P1-1）多域 apply：一次 apply 多个业务域 → 各域生成 `domain_role` 实例（`template_id`/`template_version`/`locked_fields` 回填）；满额域（≤20）跳过 + 中文提示，返回逐域结果（部分成功）
- [ ] AC3（P1-2）跨域批量停用：`batch-status` 接口对选中员工 × 域集一次性置 `disabled`，返回 `{success:[domainId...], failed:[{domainId,reason}...]}`；前置 **step-up 二次认证**（与域删除同级），**令牌经 `validateStepUpToken` 真实校验**，无效/过期/缺失 → 403 + 中文（含存量 `updatePlatformRoles` 同规修复）
- [ ] AC4（P0-①）目标域校验：A 域管理员调用 B 域资源（含模板/批量 API）→ 403 + 中文；平台管理员批量跨域正常通过（US-S1-08）
- [ ] AC5（P1-1）锁定字段：域端修改 `locked_fields` 内字段（默认权限包）→ 403 + 中文；非锁定字段（名称/成员）可微调
- [ ] AC6（P1-1）同步策略：`immediate` 模板权限变更自动下发各实例（被域覆盖项跳过并审计）；`manual` 域端展示「落后 N 版本」漂移提示（`template_version` 对比）
- [ ] AC7（P0-②/P1-2）审计：成员增删/改角色、域角色 CRUD、模板 apply/sync/unapply、跨域批量停用逐域落 `audit_log`；含操作入口端点列 `point`（platform/domain）可区分操作端

## Out of Scope

- 共享角色模型（模型 1）——已否决，采用模型 3（D1）
- 组织树预筛选域（D6 先手选域）；组织/部门模型对接
- `role(scope=domain)` 双轨治理落地（P3 后置，MVP 不做，轨 B 冻结 D5）
- `immediate` 异步队列、模板版本历史（P3 后置）
- ≤20 上限放宽（当前仅补校验，不调整上限）
