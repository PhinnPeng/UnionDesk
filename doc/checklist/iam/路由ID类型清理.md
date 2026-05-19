# 路由 ID 类型清理

## 目标
清理管理端路由 `id` 字段的 TypeScript 报错，确保后端菜单转换、动态路由补齐和测试代码共享同一类型契约。

## 开发清单
- [x] 复现 `AppRouteRecordRaw` 缺少 `id` 字段导致的类型检查失败
- [x] 将路由 `id` 明确纳入管理端路由类型定义
- [x] 收窄后端菜单 ID 测试断言到实际生成的路由节点
- [x] 运行管理端类型检查确认 TS 报错已清理

## 验证
- [x] `pnpm typecheck`
- [x] `pnpm test --run src/api/user/utils.test.ts src/router/utils/generate-routes-from-backend.test.ts src/router/utils/generate-user-menus.test.ts`
