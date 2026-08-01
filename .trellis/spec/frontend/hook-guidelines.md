# Hook Guidelines

> 自定义 Hook 命名与使用约定。

---

## Overview

项目 **没有** 统一的 data-fetching hook 层（非 React Query 默认）。大多数页面用 `useState` + `useCallback` + `useEffect` 直接请求 API。

Hook 按作用域分：**应用级**（`src/hooks/`）与 **组件级**（`components/*/hooks/`）。

---

## 目录与命名

| 位置 | 命名 | 示例 |
|------|------|------|
| `src/hooks/use-{name}/index.ts` | `use{Name}` | `useAuth`, `useAppScope` |
| `components/*/hooks/use-{name}/` | 组件专用 | `use-table-scroll` |
| `src/layout/**/hooks/` | 布局专用 | `use-layout-menu` |

文件夹 **kebab-case**；导出函数 **camelCase** 且以 `use` 开头。

---

## 应用级 Hook 职责

| Hook | 文件 | 职责 |
|------|------|------|
| `useAuth` | `hooks/use-auth/index.ts` | `hasPermission(code)`，读 user store |
| `useAppScope` | `hooks/use-app-scope/index.ts` | 从 pathname 解析 platform/domain |
| `usePreferences` | `hooks/use-preferences/index.ts` | 主题、布局偏好 |
| `useCurrentRoute` | `hooks/use-current-route/index.ts` | 当前路由元数据 |

```ts
// use-auth/index.ts 模式
export function useAuth() {
  const actions = useUserStore(state => state.actions);
  const hasPermission = (code: string) => actions.includes(code);
  return { hasPermission };
}
```

---

## 页面内逻辑

列表页数据加载、分页、弹窗状态 **留在页面组件**，不抽 hook，除非：

- 同一逻辑被 **≥2 个页面** 复用，或
- 逻辑超过 ~80 行且边界清晰

反例：为单个 `attribute-tab.tsx` 再建 `use-attribute-tab.ts`（违反 `AGENTS.md` §2.5）。

---

## React Query

Provider：`components/tanstack-query/index.tsx`。  
**仅少数页面**使用（如 `pages/system/role/index.tsx` 菜单树）。新平台 CRUD 页默认 **不用** React Query，除非团队明确迁移。

---

## Hook 编写规则

- 返回值用对象 `{ data, loading, refresh }`，避免多返回值 positional
- 依赖数组如实填写；项目 ESLint 关闭了 `exhaustive-deps`，但仍应手动保证正确性
- 不在 hook 内硬编码 API base URL；走 `#src/api` 或 `@uniondesk/shared`
- 副作用（`useEffect`）清理订阅/定时器

---

## 反模式

- ❌ `useXxx` 内直接操作 DOM（除 ref 聚焦等窄场景）
- ❌ 为单次使用的 `useMemo` 包装再抽 hook
- ❌ hook 文件 export 非 hook 函数作为主 API

---

## 参考文件

- `src/hooks/use-auth/index.ts`
- `src/hooks/use-app-scope/index.ts`
- `components/basic-table/hooks/use-table-scroll/index.ts`
- `pages/platform/user/index.tsx`（页面内 state 模式）
