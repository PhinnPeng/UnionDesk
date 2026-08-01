# Frontend Development Guidelines

> UnionDeskAdminWeb 前端开发规范索引。

---

## Overview

本目录约定来自 `AGENTS.md` §2 与 AdminWeb 代码库实际模式（2026-07-10 Bootstrap）。

---

## Guidelines Index

| Guide | Description | Status |
|-------|-------------|--------|
| [Directory Structure](./directory-structure.md) | pages/components/api 组织 | ✅ Filled |
| [Component Guidelines](./component-guidelines.md) | Ant Design v6、列表页骨架 | ✅ Filled |
| [Hook Guidelines](./hook-guidelines.md) | use-* 命名与放置 | ✅ Filled |
| [State Management](./state-management.md) | Zustand + 本地 state | ✅ Filled |
| [Quality Guidelines](./quality-guidelines.md) | lint、typecheck、测试 | ✅ Filled |
| [Type Safety](./type-safety.md) | shared 类型、#src 别名 | ✅ Filled |

---

## Pre-Development Checklist

开始写前端代码前：

- [ ] 阅读 `AGENTS.md` §2（组件库、样式、TSX 结构、列表页布局）
- [ ] 阅读 [Directory Structure](./directory-structure.md) 确定页面路径
- [ ] 列表页对照 [Component Guidelines](./component-guidelines.md) §列表页标准骨架
- [ ] 确认类型来自 `@uniondesk/shared` 或页面 `utils.ts`（见 [Type Safety](./type-safety.md)）
- [ ] 权限码集中定义，UI 用 `AuthGuarded`

---

## Quality Check

完成实现后：

- [ ] `pnpm run typecheck:admin` 通过
- [ ] `pnpm run lint:admin` 无新增 error
- [ ] 破坏性操作用 `ConfirmPopover`
- [ ] 未抽离单页专用组件/路由常量
- [ ] 中文 UI 文案

---

## 权威参考

- `AGENTS.md` §2 — 前端主规范
- `UnionDeskWeb/apps/UnionDeskAdminWeb/README.md`
- 列表页范例：`pages/platform/domains/index.tsx`
