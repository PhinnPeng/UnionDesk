# Type Safety

> TypeScript 类型组织与导入约定。

---

## Overview

- `strict: true`（`tsconfig.json`）
- 开发时检查：`vite-plugin-checker` + `pnpm typecheck`（`tsc --noEmit`）
- 类型来源：**@uniondesk/shared**（跨应用）+ 页面本地类型（单页 `utils.ts`）

---

## 导入顺序（观察到的惯例）

```tsx
import type { TicketAttribute } from "@uniondesk/shared";
import type { TableColumnsType } from "antd";

import { AuthGuarded } from "#src/components/auth-guarded";
import { resolveAttributeTypeKey } from "#src/pages/platform/ticket-config/attributes/components/attribute-utils";

import { Button, Table } from "antd";
import { useMemo, useState } from "react";

import { AttributeSlotEditModal } from "./attribute-slot-edit-modal";
```

1. `import type` 外部包
2. `import type` `#src`
3. `import` `@uniondesk/shared`
4. `import` `#src`
5. 图标、`antd`、`react`
6. 相对路径 `./`

---

## 类型存放位置

| 类型 | 位置 |
|------|------|
| 后端 DTO 镜像、跨应用实体 | `packages/shared/src/types.ts` 及子模块 export |
| 单页表格行、筛选表单 | `pages/platform/{feature}/utils.ts` |
| 组件 Props | 组件文件顶部 `interface XxxProps` |
| API 响应窄化 | shared 内 `fetch*` 返回类型 + `toErrorMessage` |

**不要**为仅用一次的 Props 建独立 `types.ts` 文件。

---

## `@uniondesk/shared` 用法

```ts
import type { TicketAttribute, TicketAttributeSlotConfig } from "@uniondesk/shared";
import { fetchAdminDomainsPage, toErrorMessage } from "@uniondesk/shared";
```

Vite 别名指向源码：`packages/shared/src/index.ts`。  
Admin 专用 endpoint 类型留在 `#src/api/platform/*`。

---

## `#src` 别名

```json
// tsconfig.json
"paths": { "#*": ["./*"] }
```

一律使用 `#src/components/...`，避免 `../../../`。

---

## 实用模式

### 判别联合 / 类型守卫

```ts
export function isDividerRow(row: DisplayRow): row is DividerRow {
  return "type" in row && row.type === "divider";
}
```

### 局部扩展共享类型

```ts
export interface SlotRow extends TicketAttributeSlot {
  dragId: string;
}
```

### JSON 字段

slot `default_value` 等 JSON 字符串：用专用解析函数 + 本地 `type SlotDefaultValue`（见 `attribute-default-value.ts`），避免 `as any`。

---

## 校验

```powershell
cd UnionDeskWeb
pnpm run typecheck:admin    # 或 pnpm --filter react-antd-admin typecheck
```

提交前必须通过；修改 `packages/shared` 后需同时跑 admin typecheck。

---

## 反模式

- ❌ `any` / `@ts-ignore` 绕过（除非有注释说明的边界）
- ❌ 重复定义与 shared 相同的 DTO
- ❌ 在 `.tsx` 用 `interface` 描述 API 响应而不与 shared 同步
- ❌ 路由 path 抽成 const 类型联合（项目禁止 path 常量化，`AGENTS.md` §2.5）

---

## 参考文件

- `packages/shared/src/types.ts`
- `pages/platform/domains/ticket-type-config/components/attribute-default-value.ts`
- `pages/platform/domains/ticket-type-config/components/attribute-slot-table.tsx`
- `apps/UnionDeskAdminWeb/tsconfig.json`
