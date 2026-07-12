# Design — 事项类型配置页布局优化

## 1. 目标布局

```text
ticket-type-config-page (flex column, full height)
├── TicketTypeConfigPageHeader (sticky)
│   ├── 面包屑行
│   └── 标题+Tab 合体卡片（白底 elevation）
└── ticket-type-config-page__body
    └── AttributeTab (platform)
        ├── 滚动区 (flex-1, gap-16, padding 16px 24px)
        │   ├── Card「筛选条件」
        │   │   └── TableSearchForm → keyword
        │   └── Card「属性列表」 extra=[属性排序, 添加属性]
        │       └── AttributeSlotTable
        └── Footer sticky [暂存, 发布]
```

父级 `TicketConfigShell` 不变；本页 Body 使用 `gap` + Card 双层，对齐 `domains/index.tsx` / `blockwords/index.tsx`。

---

## 2. Header

### 2.1 面包屑 hover 统一

**文件**：`ticket-type-config-header.less`

所有可点击项（含 `__breadcrumb-back`）共用：

```less
padding: 2px 6px;
border-radius: 4px;
transition: color 0.2s, background 0.2s;

&:hover {
  color: var(--ant-color-primary);
  background: var(--ant-color-fill-quaternary);
}
```

返回箭头保留 `margin-right`，尺寸可与文字行高对齐（约 28px 触控高）。

### 2.2 事项类型块（悬浮感）

**文件**：`ticket-type-config-header.less`

将 `__title-card` 从 `fill-quaternary` 改为与 `index.module.less` 中 `detailBody` 一致的 elevation：

```less
background: var(--ant-color-bg-container);
border: 1px solid var(--ant-color-border-secondary);
border-radius: 8px;
box-shadow: 0 1px 2px rgb(0 0 0 / 4%);
```

Header 外层背景可保持 `bg-container` 或与 body 区略区分（`fill-quaternary` 仅作 header 底，title-card 白底浮于其上）。

### 2.3 Tab 合体卡片

**文件**：`ticket-type-config-header.tsx` + `.less`

结构：

```text
.ticket-type-config-page-header__hero-card
  ├── __title-row（标题 + Tag）
  └── __tabs（底边激活线，无 margin 负值）
```

- 去掉 `margin: 16px -24px 0` 与外层 `border-top`
- Tab 项间距 `24px`，`padding: 10px 0`
- `__hero-card` 承载 title + tabs 整块白卡片阴影

---

## 3. 搜索与过滤

### 3.1 UI

**文件**：`attribute-tab.tsx`（platform 分支）、`attribute-tab.less`

```tsx
<Card bordered={false} title={<><SearchOutlined /> 筛选条件</>}>
  <TableSearchForm
    loading={loading}
    initialValues={{ keyword: "" }}
    onFinish={v => setKeyword(v.keyword ?? "")}
    onReset={() => setKeyword("")}
  >
    <Form.Item name="keyword" label="关键字">
      <Input allowClear placeholder="属性名称或描述" />
    </Form.Item>
  </TableSearchForm>
</Card>
```

### 3.2 过滤逻辑（简化版 — 用户确认 #1）

**文件**：`attribute-tab.tsx` — 调整 `filteredSlots` / `buildDisplayRows` 协作

规则：

1. 对 **全部** `activeSlots`（含 `isFixedSystemSlot`）按 keyword 过滤名称/描述
2. 过滤后若仍存在固定行 + 可排序行，**始终**在固定行与可排序行之间插入分隔行 `{ type: "divider" }`
3. 若过滤后仅有固定行或仅有可排序行，**不**插入分隔行（与现 `buildDisplayRows` 一致）
4. **不**做「固定行始终显示、不参与搜索」的例外

实现要点：

- 先 `filter(activeSlots)` → 得到 `matched`
- 再 `buildDisplayRows(matched)` 生成分隔行
- `keyword` state 已存在，仅补 UI

### 3.3 搜索 + 拖拽（用户确认 #2）

- **不**在搜索时禁用 DnD
- `onDragEnd` 仍在完整 `activeSlots`（draftSlots）上重排，数据源为 `filteredSlots` 仅影响展示
- 拖拽 ID 仍用 `dragId`；`SortableContext items` 取自当前 `filteredSlots` 中可排序行

注意：过滤后拖拽只改变可见可排序行的相对顺序，逻辑与现网一致（在 `activeSlots` 子集上 reorder 后写回 draft）。

---

## 4. 列表 Card（用户确认 #3）

**文件**：`attribute-tab.tsx`、`attribute-tab.less`

移除独立 `attribute-tab__toolbar`；改为：

```tsx
<Card
  bordered={false}
  className="attribute-tab__list-card"
  title="属性列表"
  extra={(
    <AuthGuarded ...>
      <Space>
        <Button icon={<OrderedListOutlined />}>属性排序</Button>
        <Button type="primary" icon={<PlusOutlined />}>添加属性</Button>
      </Space>
    </AuthGuarded>
  )}
  styles={{ body: { padding: 0 } }}
>
  <AttributeSlotTable ... dataSource={filteredSlotRows} />
</Card>
```

`filteredSlotRows`：`buildDisplayRows(filteredSlots)` 的输出，或表格内继续 `buildDisplayRows(dataSource)` — 统一在一处计算避免重复。

样式：

- 去掉 `attribute-tab--platform .attribute-slot-table .ant-table` 外层重复 border（Card 提供容器）
- 表头 `fill-quaternary`、cell padding 保留
- 滚动区：`flex: 1; overflow-y: auto; padding: 16px 24px; gap: 16px;`

---

## 5. 表格列宽与操作列（用户确认 #4）

**文件**：`attribute-slot-table.tsx`、`attribute-slot-table.less`

```tsx
<Table tableLayout="fixed" ... />
```

| 列 | width | 备注 |
|----|-------|------|
| 拖拽 | 40 | center |
| 属性名称 | undefined + `ellipsis` | 剩余空间，name cell flex |
| 属性类型 | 128 | center |
| 描述 | 120 | ellipsis |
| 默认值 | 128 | ellipsis |
| 是否必填 | 96 | center |
| 是否显示 | 104 | center |
| 操作 | 152 | center, `fixed: "right"` 可选 |

操作列（已有 icon，保持文字+图标）：

```tsx
<Button type="link" size="small" icon={<EditOutlined />}>编辑</Button>
<Button type="link" size="small" danger icon={<DeleteOutlined />}>删除</Button>
```

`attribute-slot-table__actions`：`gap: 8px`，保证 152px 内不换行。

分隔行 `colSpan`：按可见列数动态或固定 8（含拖拽列）。

---

## 6. 文件清单

| 文件 | 变更 |
|------|------|
| `ticket-type-config-header.tsx` | hero-card 结构（title + tabs） |
| `ticket-type-config-header.less` | 面包屑 hover、elevation、tabs |
| `attribute-tab.tsx` | 筛选 Card、列表 Card、过滤+分隔行逻辑 |
| `attribute-tab.less` | 滚动区 gap/padding、移除 toolbar 样式 |
| `attribute-slot-table.tsx` | 列宽、tableLayout、colSpan |
| `attribute-slot-table.less` | Card 内表格、操作列间距 |

**不改**：`platform-ticket-type-config-content.tsx`（除间接样式）、DnD 核心、API、Modal。

---

## 7. 验证

```powershell
cd UnionDeskWeb
pnpm run typecheck:admin
```

手工：

1. 无搜索：固定行 + 分隔行 + 可排序行正常
2. 搜索「标题」：仅匹配行 + 分隔行（若同时有固定+非固定匹配）
3. 搜索无结果：Empty 在 Card 内
4. 搜索态拖拽：顺序变更，暂存后保持
5. 面包屑 hover 一致；标题卡有轻阴影
6. 操作列图标+文字完整显示
