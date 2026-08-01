# Quality Guidelines

> 前端代码质量、Lint、测试与审查标准。

---

## Overview

Monorepo 根：`UnionDeskWeb/`。Admin 应用包名：`react-antd-admin`。

工具链：ESLint（`@antfu/eslint-config`）、TypeScript strict、Vitest + Testing Library。

---

## 验证命令

```powershell
cd UnionDeskWeb
pnpm run typecheck:admin     # tsc --noEmit
pnpm run lint:admin          # check:utf8 + eslint
pnpm --filter react-antd-admin test   # vitest
pnpm --filter react-antd-admin build  # 生产构建
```

Pre-commit：`lint-staged` → `eslint --fix`（`simple-git-hooks`）。

---

## ESLint 风格

- **缩进**：Tab
- **引号**：双引号
- **分号**：有
- `react-hooks/exhaustive-deps`：**关闭**（仍需人工保证依赖正确）

---

## 测试

- 框架：**Vitest** + **happy-dom** + **@testing-library/react**
- Setup：`src/setupTests.ts`（jest-dom matchers）
- 位置：与源码同目录 `*.test.ts` / `*.test.tsx`
- 侧重：utils、API 封装、权限/菜单生成、关键组件——**非** E2E

```powershell
pnpm --filter react-antd-admin test
```

示例：

- `pages/platform/user/index.test.tsx`
- `api/platform/iam.test.ts`

新功能：优先为 **纯函数**（utils、mappers、permissions）加测试；UI 冒烟可选。

---

## 代码审查清单

- [ ] `pnpm run typecheck:admin` 通过
- [ ] `pnpm run lint:admin` 无新增 error
- [ ] 列表页符合 `TableSearchForm` + `Card` 骨架
- [ ] 权限用 `AuthGuarded`；删除用 `ConfirmPopover`
- [ ] 中文 UI 文案；UTF-8 文件
- [ ] 未抽离单页专用组件/常量文件
- [ ] 修改 shared 包时两端 typecheck
- [ ] 样式：Ant Design v6 + Less，无多余 CSS 方案混用

---

## AGENTS.md 行为准则（摘要）

- 外科手术式修改：不顺手重构无关代码
- 简单性优先：不加未要求的功能/抽象
- 完成前自检：是否改了指令外文件
- 任务总结格式见 `AGENTS.md` §1.6

---

## 无障碍（当前实践）

- 项目 **无** 强制 a11y 测试套件
- 遵循 Ant Design 组件默认可访问性
- 图标按钮配合文字或 `aria-label`（Destructive 操作有确认文案）
- 新交互避免仅靠颜色传达状态（配合文字/Tag）

---

## 反模式

- ❌ 跳过 typecheck 交付
- ❌ 各页复制 QueryFilter 配置而不走 `TableSearchForm`
- ❌ 在组件内硬编码权限码字符串散落多处（应集中 `*-permissions.ts`）
- ❌ 引入新全局 CSS 框架

---

## 参考文件

- `UnionDeskWeb/package.json`（`typecheck:admin`、`lint:admin`）
- `apps/UnionDeskAdminWeb/package.json`
- `apps/UnionDeskAdminWeb/eslint.config.js`
- `AGENTS.md` §2、§2.6、§2.7
