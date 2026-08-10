# CustomerWeb 首页设计落地 — 执行计划

## 前置

- [x] Figma 132:155 设计上下文已取回（get_design_context）
- [x] 设计图标资源已下载至 `agent-work/home-figma/`（path 数据源）
- [x] shared 数据源已确认（portal 快照字段）
- [x] prd.md / design.md 评审通过

## 实施清单（顺序执行）

1. **tokens.css**：`--ud-rail-w: 76px → 240px`；`--ud-content-max: 1080px → 1136px`；末尾追加 `--ud-cx-*` 色板（design.md §2）
   - 验证 [检查]：`grep -c -- --ud-cx-` ≥ 13；现有 --ud-* 行未被删除（git diff 确认仅改 2 行 + 追加）
2. **Icons.tsx**：追加 14 个实心图标组件（design.md §3 映射表；path 从 agent-work/home-figma/ 对应 SVG 复制；fill="currentColor"；viewBox 按表）
   - 验证 [检查]：`pnpm --filter customer-web typecheck` 通过；无未使用导出告警（本任务页面将使用全部 14 个）
3. **AppShell.tsx**：重构侧边栏（240px：品牌头 + NavItems + 底部三栏）与顶栏（搜索框新样式 + 提交工单按钮 + 域 chip 移动端化 + 移除 avatar）；dock 分支保持现有渲染（图标+文字竖排）
   - 验证 [检查]：NavItems 组件在不破坏 dock 的前提下支持 rail 新布局；路由/跳转逻辑不变
4. **app.css**：重写 `.ud-rail` 系（240px 布局/导航项/角标/底部三栏）与 `.ud-topbar` 系（64px/搜索/提交按钮/域 chip 显隐）；删除 avatar 样式；新增首页样式块（.ud-welcome/.ud-stat-grid/.ud-stat-card/.ud-dash-grid/.ud-card/.ud-ticket-row/.ud-notif-row/.ud-foot-copy）；移动端规则（≤480px 隐藏提交按钮）
   - 验证 [检查]：样式值对照 design.md §4.2/§5.3；`.ud-dock` 与 `.ud-rail` 移动端媒体查询不冲突
5. **pages/home/index.tsx**：重写（欢迎卡/统计卡/双栏/版权/空域分支保留/refresh useEffect 保留；私有 greetingByHour/relativeTime/safeJump）
   - 验证 [检查]：typecheck 通过；统计与通知数据绑定正确
6. **自检**：git diff 全量审查——只动 5 个目标文件；无遗留 agent-work 引用；无死代码

## 验证命令

```bash
# 前端类型检查（customer-web 应用）
pnpm --filter customer-web typecheck
# 或按仓库既有脚本（README 确认后以实际为准）

# UTF-8 检查（仓库既有 check:utf8 脚本，如存在）
pnpm check:utf8

# 浏览器冒烟（本地 dev server）
# 1) 桌面 1440：首页各区块视觉与 132:155 对齐
# 2) 导航激活态（首页）紫底紫条；未激活灰
# 3) 统计数字/待处理文案/未读角标/通知列表/姓名/业务域名与 portal 数据一致
# 4) 交互：搜索回车 / 提交工单 / 查看待处理 / 查看全部 / 通知行 / 栏2 业务域 / 栏3 身份
# 5) <900px：dock 正常、域 chip 可见、卡片不溢出
# 6) /tickets /inbox /me /domains /chat 壳层正常
```

## 评审门（trellis-check）

- prd AC1-AC7 逐条核对（视觉/数据/交互/移动端/回归/构建）
- 精准修改审计：仅 5 个目标文件有改动；tokens.css 现有 token 未被篡改；其他页面文件零改动
- 无新增依赖；无后端改动

## 回滚点

- 步骤 1-2 完成后即形成可编译状态；任意步骤失败回退：`git checkout -- <文件>`
- 视觉偏差仅涉 app.css / AppShell.tsx / home 页面，定向还原
