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
  <Button type="link" size="small" danger icon={<DeleteOutlined />} />
</ConfirmPopover>
```

---

## 操作列图标语言（Table 操作列统一规范）

数据表格操作列统一使用**纯图标按钮**模式（先例：`pages/domain/customers/index.tsx`）。

### 图标词典（动作 → 图标，全仓唯一映射）

| 动作 | 图标 |
|---|---|
| 编辑 | `EditOutlined` |
| 查看/详情/权限 | `EyeOutlined` |
| 删除/移除/拔出 | `DeleteOutlined`（danger） |
| 禁用 / 启用 | `StopOutlined` / `PlayCircleOutlined` |
| 重置密码 | `KeyOutlined` |
| 新增下级/添加 | `PlusOutlined` |
| 更多（下拉 trigger） | `EllipsisOutlined`（禁止 `MoreOutlined`） |
| 属性/设置 | `SettingOutlined` |
| 工作流 | `NodeIndexOutlined` |
| 复制 | `CopyOutlined` |
| 离职 | `UserDeleteOutlined` |
| 回退/恢复/拔出 | `RollbackOutlined` |

### 交互规范

- 操作列图标按钮**必须**包裹 `Tooltip`（中文提示）；Modal 内小表格空间受限可省略
- 主操作区 ≤3 个图标；低频/危险/次要动作收进「更多」`Dropdown`（`trigger={["click"]}`，菜单项带图标，删除项 danger）
- 破坏性操作必须 `ConfirmPopover`（下拉菜单内改用 `modal.confirm`）二次确认
- 菜单项权限用 `useAuth().hasPermission(...)` 条件渲染；主区图标按钮用 `AuthGuarded` 包裹
- 操作列宽度按图标数量收缩（3 图标 ~120，单图标 ~80）

```tsx
<Tooltip title="编辑">
  <Button type="link" size="small" icon={<EditOutlined />} />
</Tooltip>
<Dropdown
  trigger={["click"]}
  menu={{
    items: [
      ...(canDelete
        ? [{ key: "delete", label: "删除", icon: <DeleteOutlined />, danger: true, onClick: () => onDelete(row) }]
        : []),
    ],
  }}
>
  <Tooltip title="更多">
    <Button type="link" size="small" icon={<EllipsisOutlined />} />
  </Tooltip>
</Dropdown>
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
