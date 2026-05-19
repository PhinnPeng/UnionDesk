# AdminWeb 前端 TS 告警修复清单

## 目标
清理 UnionDeskWeb 管理端当前的 TypeScript 告警，确保相关 `typecheck` 通过，不引入额外功能改动。

## 开发清单
1. [完成] 识别 TS 告警来源 -> 验证 `pnpm --dir UnionDeskWeb/apps/UnionDeskAdminWeb typecheck`
2. [完成] 修复类型导入、泛型与 JSX 兼容声明 -> 验证同一条 `typecheck` 通过
3. [完成] 回归检查仓库级前端类型 -> 验证 `pnpm --dir UnionDeskWeb typecheck`

## 结果
- AdminWeb `typecheck` 已通过
- 仓库级 `typecheck` 已通过
