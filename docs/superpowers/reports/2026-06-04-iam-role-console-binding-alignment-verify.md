# Verification Report: iam-role-console-binding-alignment

**Date:** 2026-06-04  
**Change:** IAM 角色—控制台绑定对齐  
**Branch:** `iam-role-console-binding-alignment`  
**Base ref:** `090fb0849a26c96de3bafbc90d9447f5f628138f`  
**Verify mode:** full  
**Commits:** 1 (`be13bab`)

## Summary

| Dimension    | Status |
|:-------------|:-------|
| Completeness | 17/17 tasks, 5/5 requirements |
| Correctness  | 5/5 requirements with implementation evidence |
| Coherence    | Followed (1 manual-smoke warning) |

**Final Assessment:** No critical issues. 2 warnings to consider. Ready for archive (with noted improvements).

## Verification Checks

| # | Check | Result |
|:--|:------|:-------|
| 1 | tasks.md 全部 `[x]` | PASS |
| 2 | 改动与 tasks 一致（22 files, base-ref→HEAD） | PASS |
| 3 | 编译/测试命令通过 | PASS |
| 4 | 相关测试通过 | PASS |
| 5 | 无明显安全问题 | PASS |
| 6 | 实现已提交（verify 前置） | PASS（`be13bab`） |

### Tests Executed

- `mvn test -pl uniondesk-app -Dtest=PermissionScopePolicyTests,IamServiceTests` → **passed**（需先 `mvn install -pl uniondesk-iam`）
- `pnpm exec vitest run utils.test.ts detail.test.tsx app-scope.test.ts` → **10/10 passed**

## Requirement Mapping

| Requirement | Evidence |
|:------------|:---------|
| 平台角色仅绑定 platform 控制台 | `V202606150001` 收敛 global 直授/菜单；`PermissionScopePolicy` global 仅 `platform.*`；`AdminMenuService.mergeRolePermissionActions` 按 scope 过滤 |
| 业务域角色仅绑定 business 控制台 | Flyway 清理 `agent` platform 绑定；`PermissionScopePolicy` domain 拒绝 `platform.*`；`IamServiceTests` 过滤 platform actions |
| 双控制台显式组合 | 数据层分离 `user_global_role` + `user_domain_role`；`app-scope.test.ts` 双角色 → `/home` + `platformAccess` |
| 角色授权保存 scope 校验 | `AdminMenuService.ensureRoleMenuScopeAlignment` 在 `replaceRolePermissions` 前校验 menu/button scope |
| global super_admin break-glass | Flyway 移除 business 直授；`filterAssignablePlatformRoles` 隐藏 `super_admin`；PRD §4.1.3 更新 |

## Issues

### WARNING

1. **4.2 / 3.2 手工冒烟未执行**  
   - 任务已勾选，但依赖 Flyway `V202606150001` 迁移后在真实 DB 用 `admin` / `domain_admin` 登录验证首页与侧栏。  
   - **建议：** migrate 后补一次：`admin` → `/platform/home`；`domain_admin` → `/home`。

2. **角色保存跨界拒绝无专项集成测试**  
   - `ensureRoleMenuScopeAlignment` 已实现，但无 API/Service 层测试覆盖 spec 中「保存 business 菜单到 global 角色被拒绝」场景。  
   - **建议：** 后续补 `AdminMenuService` 或 Controller 集成测试（非 archive 阻塞）。

### SUGGESTION

1. **`openspec` CLI 未安装**，本次以手工对照 delta spec / design.md / Superpowers Design Doc 完成 full 验证。

## Dirty Worktree (non-blocking)

以下未提交改动**不属于**本 change：

- `docs/README.md`, `backlog-epics.md`, `backlog-stories.md`, `sprint-3-plan.md`（S3 规划文档）
- `docs/qa/implementation-traceability.md`
- `openspec/changes/audit-log-semantics/`（并行 change）
- Playwright 临时文件、`test-results/`

## Security

- 无硬编码密钥
- 无新增 unsafe 操作
- 权限收敛遵循最小权限（移除 global 角色跨界 business 直授）

## Branch Handling

**用户选择：** 保持分支不动（Option 3）。  
分支 `iam-role-console-binding-alignment` 保留在本地，提交 `be13bab`；未推送、未合并。
