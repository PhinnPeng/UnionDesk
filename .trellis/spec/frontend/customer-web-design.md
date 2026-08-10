# CustomerWeb 服务台设计落地规范

> CustomerWeb（UnionDeskCustomerWeb）按 Figma 设计稿落地的代码规范。设计稿：`Figma CustomerWeb`（文件 key `UjSDCyQXtHR9YPSHuXLpX9`），登录页 132:2、服务台首页 132:155。

---

## 1. 设计色板 token（tokens.css）

设计稿为**紫色主调**（与 AdminWeb/登录页蓝调 --ud-* 体系不同），落地时以 `--ud-cx-*` 前缀新增 token，**不得改动现有 --ud-* 定义**（登录页 132:2 已依赖）。

```css
--ud-cx-primary: #4f45e5;            /* 主紫 */
--ud-cx-primary-strong: #7d3bed;     /* 渐变终点 */
--ud-cx-primary-soft: rgba(79, 69, 229, 0.08);  /* 激活底/图标块底 */
--ud-cx-cyan: #05b5d4;               /* 进行中 */
--ud-cx-cyan-soft: rgba(5, 181, 212, 0.08);
--ud-cx-green: #0fba82;              /* 已完成 */
--ud-cx-green-soft: rgba(15, 186, 130, 0.08);
--ud-cx-danger: #f04545;             /* 角标红 */
--ud-cx-text: #1f2938;               /* 标题文字 */
--ud-cx-text-secondary: #6b7080;     /* 导航闲置/次要文字 */
--ud-cx-text-muted: #9ca3b0;         /* 时间/版权 */
--ud-cx-bg-soft: #f7fafc;            /* 搜索框底/hover */
--ud-cx-divider: rgba(0, 0, 0, 0.1);
```

布局 token：`--ud-rail-w: 240px`（侧边栏）、`--ud-content-max: 1136px`（内容区，1440 下左右各 32px 边距）、`--ud-topbar-h: 64px`。

## 2. 侧边栏（AppShell .ud-rail）结构约定

- 240px 白底导航栏：品牌头（渐变 U 标 + UnionDesk 文字）→ 5 项导航 → 底部三栏
- **激活态**：浅紫底 `--ud-cx-primary-soft` + `::before` 左 3px×20px 紫条 + 紫字紫图标；闲置灰 `--ud-cx-text-secondary`
- 角标（「二期」/未读数）：高 21px 圆角 6 bg `--ud-cx-danger`，flex 内联于 label 后 12px（非 absolute）
- 底部三栏：栏1 设置|消息（28px 图标钮 + 1px 竖线 + 未读角标）；栏2 业务域（图标 + 域名 + ▼，→ /domains）；栏3 个人身份（20px 渐变头像 135deg #4F45E5→#05B5D4 + 姓名，→ /me）
- 移动端 <900px：侧边栏整体隐藏（域 chip 显示在顶栏），dock 保留

## 3. Figma 设计稿 → 代码落地流程

1. `get_design_context` 取回参考代码（含全部图标导出 URL）
2. 图标导出为 SVG：**导出图内置固定 fill 色**（如 #4F46E5/#6B7280），落地时把 `<path id="Vector">` 的 d 数据复制进 `Icons.tsx` 组件并改 `fill="currentColor"`，viewBox 保持与导出一致，尺寸由 CSS 上下文控制
3. 栅格化图片区域（如表体/文字转 path 的标题）→ 用真实数据实现，不还原图片

### Convention: Figma 图标转 currentColor 组件

**Why**: 设计稿图标有激活/闲置双色态，导出 SVG 固定 fill 无法用 CSS 切换；转 currentColor 后由 `.ud-rail__item` 的 color 统一着色。

```tsx
// 正确：path 数据来自设计稿导出，fill=currentColor
export function IconNavHome({ className }: IconProps) {
	return (
		<svg className={iconClass(className)} viewBox="0 0 20 20" fill="currentColor" aria-hidden>
			<path d="M8.33333 16.6667V11.6667H11.6667..." />
		</svg>
	);
}
```

## 4. 首页数据绑定（pages/home）

- 统计卡：`countByLifecycle(portal.currentDomainTickets.map(t => t.status))` → pending/active/done
- 未读角标（导航通知 + 栏1消息）：`portal.unreadCount`（>9 显示 9+）
- 未读通知卡：`portal.inboxMessages` 未读优先 slice 3；`kind === "system"` → 盾牌图标，其余 → 工单图标；跳转 `jumpUrl` 需 safeJump（`/workspace` 前缀 → `/home`，空 → `/home`）
- 相对时间（刚刚/n 分钟前/n 小时前/n 天前）：页面私有函数，超 7 天回落 `formatDateTime`

## 5. 常见错误

### Common Mistake: 布局 token 与设计稿高度漂移

**Symptom**: 顶栏设计 64px 但 `--ud-topbar-h` 仍是旧值 56px，导致 `.ud-type-rail` sticky 偏移差 8px（回归隐患）。

**Cause**: 改视觉时只改了 .ud-topbar 样式，忘了同步 token。

**Fix**: 单一事实来源——`.ud-topbar` 用 `min-height: var(--ud-topbar-h)`，改高度只改 token。

**Prevention**: 变更壳层布局尺寸时，grep 检查对应 `--ud-*-h/-w` token 是否同步。
