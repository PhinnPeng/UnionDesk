# Component Guidelines

> React 组件编写约定（AdminWeb）。

---

## Overview

- UI 库：**Ant Design v6**（必须）
- 布局壳：`BasicContent`
- 权限：`AuthGuarded`
- 列表筛选：`TableSearchForm`
- 危险操作：`ConfirmPopover`
- 消息/对话框：`App.useApp()`（需 `components/antd-app` 包裹）

详细结构顺序见 `AGENTS.md` §2.4。

---

## 文件结构（TSX）

自上而下：

1. `import` / `import type`
2. `interface` / `type`（Props）
3. 文件内 `const`（**不含**路由 path 常量）
4. 私有辅助 `function`
5. 组件主体：`useState` → `useRef` → `useEffect` → `handleXxx` → `useMemo`/`useCallback` → `return`
6. `export`

同一文件内**私有子组件**放主组件之前。仅单页使用的 UI **不**拆独立文件（`AGENTS.md` §2.5）。

---

## Props 约定

- Props 用 `interface XxxProps` 命名
- 可选能力用 `?`；默认值在解构或参数列表
- 回调命名：`onXxx`；处理函数：`handleXxx`

```tsx
interface AttributeSlotTableProps {
  loading: boolean;
  canUpdate: boolean;
  onConfigChange: (slotId: string, patch: Partial<TicketAttributeSlotConfig>) => void;
}
```

---

## Ant Design 用法

```tsx
import { Button, Card, Table, Form, Switch } from "antd";
import { App } from "antd";

function MyPage() {
  const { message } = App.useApp();
  // ...
}
```

- Pro 组件：`@ant-design/pro-components`（`QueryFilter`、`ProTable`）
- 项目封装：`BasicTable`（ProTable 包装）、`TableSearchForm`（QueryFilter 包装）
- 图标：`@ant-design/icons`

---

## 列表页标准骨架

```tsx
<BasicContent>
  <div className="flex flex-col gap-4">
    <Card bordered={false} title={<> <SearchOutlined /> 筛选条件 </>}>
      <TableSearchForm onFinish={...} onReset={...}>
        <Form.Item name="keyword" label="关键字">
          <Input allowClear />
        </Form.Item>
      </TableSearchForm>
    </Card>
    <Card bordered={false} title="用户列表" extra={(
      <AuthGuarded auth="platform.user.create" fallback={null}>
        <Button type="primary">添加</Button>
      </AuthGuarded>
    )}>
      <Table ... />
    </Card>
  </div>
</BasicContent>
```

域详情 Tab 内可用 `div` 替代 `BasicContent`，保持相同 `flex flex-col gap-4` 结构。

---

## 样式优先级（AGENTS.md §2.1）

1. Ant Design `classNames` / `styles`
2. `ConfigProvider` theme token
3. Less 文件（超过一行样式时优先抽离）
4. 单行布局可用 Tailwind / 内联 `style`

```tsx
import "./attribute-slot-table.less";
import styles from "./detail-onboarding.module.less";
```

Less 内使用 `var(--ant-color-border-secondary)` 等变量。

---

## 权限与操作

```tsx
<AuthGuarded auth={PLATFORM_DOMAIN_CONTROL_TICKET_TYPE_UPDATE} fallback={null}>
  <Button onClick={...}>添加属性</Button>
</AuthGuarded>

<ConfirmPopover title="确认拔出该属性？" onConfirm={() => onRemove(id)}>
  <Button type="link" danger>删除</Button>
</ConfirmPopover>
```

---

## 文案与注释

- 界面文案、表单 label、错误提示：**中文**
- 文件编码：**UTF-8**

---

## 反模式

- ❌ 各页自写查询/重置按钮布局（应用 `TableSearchForm`）
- ❌ 用 `message.success` 静态方法而不用 `App.useApp()`
- ❌ 为只用一次的弹窗单独建 `components/` 文件
- ❌ 混用 CSS-in-JS 与新 Less（历史遗留除外）
- ❌ 路由 path 抽成 `ROUTES` 常量

---

## 参考文件

- `components/auth-guarded/index.tsx`
- `components/table-search-form/index.tsx`
- `components/confirm-popover/index.tsx`
- `pages/platform/domains/ticket-type-config/components/attribute-slot-table.tsx`
- `AGENTS.md` §2.4、§2.7
