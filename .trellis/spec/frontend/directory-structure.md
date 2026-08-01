# Directory Structure

> UnionDeskAdminWeb 前端目录组织（`UnionDeskWeb/apps/UnionDeskAdminWeb`）。

---

## Overview

React 19 + TypeScript + Vite + Ant Design 6 + Tailwind 4 + Less。  
页面按 **业务域/作用域** 分目录；共享组件在 `src/components/`；跨页工具在 `src/utils/`。

权威前端规范见根目录 `AGENTS.md` §2；架构说明见 `UnionDeskWeb/apps/UnionDeskAdminWeb/README.md`。

---

## `src/` 顶层

```text
src/
├── pages/          # 路由页面（按 platform / system / common 分组）
├── components/     # 可复用 UI（auth-guarded、table-search-form 等）
├── api/            # Admin 专用 HTTP 封装（requestBackendJson）
├── hooks/          # 应用级 hooks（use-auth、use-app-scope）
├── store/          # Zustand 全局状态
├── router/         # 路由、守卫、菜单
├── layout/         # 壳层（header、menu、tabbar）
├── utils/          # 通用工具（request、cn、tabbar）
├── locales/        # i18n
└── styles/         # 全局样式
```

---

## `pages/` 组织

| 目录 | 用途 |
|------|------|
| `pages/platform/` | 平台管理端功能 |
| `pages/system/` | 域内 RBAC（角色、菜单） |
| `pages/common/` | 跨作用域（如表单设计器） |
| `pages/login/`、`home/`、`exception/` | 基础页 |

### 标准列表页结构

```text
pages/platform/{feature}/
  index.tsx              # 默认导出页面
  utils.ts               # 行映射、筛选类型、权限常量（可选）
  components/
    search-panel.tsx     # 可选，封装 TableSearchForm
    detail.tsx           # 新建/编辑弹窗
  *.test.ts(x)           # 同目录测试
```

### 嵌套功能区（域详情、事项类型配置）

```text
pages/platform/domains/
  index.tsx
  platform-domain-permissions.ts
  detail/
    index.tsx
    components/detail-*.tsx
  ticket-type-config/
    index.tsx
    components/
```

隐藏路由（详情、配置流）注册在 `router/routes/core/platform-pages.ts`，`hideInMenu: true`。

---

## 导入别名

- `#src/...` → `src/...`（`tsconfig` paths: `"#*": ["./*"]`）
- `@uniondesk/shared` → 工作区共享类型与 API

```ts
import { AuthGuarded } from "#src/components/auth-guarded";
import type { TicketAttribute } from "@uniondesk/shared";
```

**禁止**过深相对路径（`../../../`）；业务代码统一 `#src`。

---

## API 文件分布

| 范围 | 位置 |
|------|------|
| Admin 专用 | `#src/api/platform/*`、`#src/api/system/*` |
| 跨应用共享 | `@uniondesk/shared`（domains、tickets、blockwords 等） |

---

## 样式文件

- 组件旁：`*.module.less`（CSS Modules）或同名 `*.less`（副作用 import）
- 页面级：`index.module.less`
- 全局布局：Tailwind 工具类 + Ant Design token（`var(--ant-color-*)`）

---

## 新增页面检查清单

1. 列表页是否用 `BasicContent` + `TableSearchForm` + `Card` 骨架（`AGENTS.md` §2.7）
2. 权限码是否集中定义（页顶或 `*-permissions.ts`）
3. 破坏性操作用 `ConfirmPopover`
4. 路由 path **不要**抽成常量文件（`AGENTS.md` §2.5）

---

## 参考页面

- 列表页：`pages/platform/domains/index.tsx`、`pages/platform/blockwords/index.tsx`
- 域详情 Tab：`pages/platform/domains/detail/components/detail-members.tsx`
- 菜单树表：`pages/platform/system/menu/index.tsx`
- 隐藏路由：`router/routes/core/platform-pages.ts`
