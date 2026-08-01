# State Management

> 前端状态存放与更新约定。

---

## Overview

**默认：组件本地 `useState`**。全局状态仅用于认证、用户权限、路由菜单、标签栏、UI 偏好。

状态库：**Zustand**（`src/store/`）。**无** 应用级 React Context 数据层（除 `login/form-mode-context.ts` 等局部 context）。

---

## Zustand Stores

| Store | 文件 | 持久化 | 内容 |
|-------|------|--------|------|
| `auth` | `store/auth.ts` | ✅ persist | token、login/logout |
| `user` | `store/user.ts` | ❌ | 用户信息、`actions[]` 权限码 |
| `access` | `store/access.ts` | ❌ | 动态路由/菜单 |
| `tabs` | `store/tabs.ts` | ❌ | 多页签状态 |
| `preferences` | `store/preferences/index.ts` | ✅ persist | 主题、语言、布局 |
| `global` | `store/global.ts` | ❌ | 杂项标志 |

```ts
import { create } from "zustand";
import { persist } from "zustand/middleware";
```

新功能 **不要** 轻易新增全局 store；先问是否必须跨路由共享。

---

## 页面级状态模式

平台 CRUD 页典型状态：

```tsx
const [loading, setLoading] = useState(false);
const [rows, setRows] = useState<Row[]>([]);
const [page, setPage] = useState(1);
const [modalOpen, setModalOpen] = useState(false);

const load = useCallback(async () => {
  setLoading(true);
  try {
    const res = await fetchXxx({ page, page_size: 20 });
    setRows(res.items);
  } finally {
    setLoading(false);
  }
}, [page]);

useEffect(() => { void load(); }, [load]);
```

**草稿/发布分离**（事项类型配置）：`draftSlots` + `baselineSlots` + `dirty` 比较（见 `attribute-tab.tsx`）。

---

## 服务器状态

- 默认：**命令式 fetch**（`@uniondesk/shared` 或 `#src/api`）
- React Query：仅角色菜单等少数场景；非默认选型
- 错误展示：`toErrorMessage(err)` + `message.error`（`App.useApp()`）

---

## 权限状态

权限列表存在 `user` store 的 `actions`。UI 门控用 `useAuth().hasPermission` 或 `<AuthGuarded auth="...">`，**不要**复制一份权限 state 到页面。

---

## 表单状态

- 简单表单：Ant Design `Form.useForm()`
- 复杂设计器：Formily（`components/formily-form-designer/`）
- 弹窗销毁：`destroyOnHidden` / `destroyOnClose` 避免脏数据

---

## 反模式

- ❌ 把单页列表数据放进 Zustand
- ❌ 用 Context 包裹整站业务数据
- ❌ 多个组件各维护一份相同列表的拷贝而不刷新
- ❌ 在 store 里直接 `fetch` 而不处理 loading/error 统一形状

---

## 参考文件

- `store/auth.ts`、`store/user.ts`
- `pages/platform/user/index.tsx`
- `pages/platform/domains/ticket-type-config/components/attribute-tab.tsx`（draft/baseline）
- `components/tanstack-query/index.tsx`
