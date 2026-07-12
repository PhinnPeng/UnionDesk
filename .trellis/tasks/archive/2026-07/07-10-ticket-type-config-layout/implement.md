# Implement — 事项类型配置页布局优化

## 步骤

### Step 1 — Header 视觉

- [ ] `ticket-type-config-header.tsx`：合并 title + tabs 为 `__hero-card`
- [ ] `ticket-type-config-header.less`：统一面包屑 hover；title elevation；移除 tabs 负边距

**验证**：浏览器查看面包屑 hover、标题卡阴影、Tab 激活线

### Step 2 — AttributeTab 布局骨架

- [ ] `attribute-tab.tsx`（platform）：增加筛选 Card + TableSearchForm
- [ ] 列表 Card + extra 按钮；删除 `__toolbar`
- [ ] `attribute-tab.less`：body 滚动区 `gap: 16px`、`padding: 16px 24px`

**验证**：筛选 Card 与列表 Card 双层结构可见

### Step 3 — 过滤逻辑

- [ ] 调整过滤：固定行纳入 keyword 匹配
- [ ] `displayRows = buildDisplayRows(filteredSlots)` 统一入口
- [ ] 确认分隔行在有过滤结果时仍正确插入

**验证**：搜「标题」/「描述」/自定义属性名；分隔行行为符合 prd

### Step 4 — 表格列宽与操作列

- [ ] `attribute-slot-table.tsx`：`tableLayout="fixed"`、列宽表
- [ ] 操作列 152px，图标+文字；分隔行 colSpan 对齐
- [ ] `attribute-slot-table.less`：Card 内样式、actions gap

**验证**：列宽均衡；操作不换行

### Step 5 — 质量检查

- [ ] `pnpm run typecheck:admin`
- [ ] 搜索 + 拖拽 + 暂存 冒烟

## 回滚

均为前端样式/布局，按文件 `git checkout` 即可，无 DB/接口变更。
