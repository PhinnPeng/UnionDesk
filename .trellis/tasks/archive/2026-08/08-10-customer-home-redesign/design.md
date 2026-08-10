# CustomerWeb 首页设计落地 — 技术设计

## 1. 文件清单与职责

| 文件 | 改动 | 职责 |
|---|---|---|
| `src/styles/tokens.css` | +新增 | `--ud-cx-*` 设计色板 token（追加在文件末尾，不改现有行） |
| `src/components/Icons.tsx` | +新增 | 14 个实心图标组件（fill="currentColor"，path 取自 `agent-work/home-figma/`） |
| `src/components/AppShell.tsx` | 重构 | 侧边栏 240px 化 + 底部三栏 + 顶栏（搜索/提交工单）+ 移动端域 chip 保留 |
| `src/styles/app.css` | 重构+新增 | .ud-rail 240px 布局、导航项/角标/底部三栏、顶栏搜索/按钮、首页样式块（ud-home-* / ud-stat-* / ud-dash-*） |
| `src/pages/home/index.tsx` | 重写 | 欢迎卡 + 统计卡 + 双栏（我的工单/未读通知）+ 版权；空域状态保留 |

不动：`StatusTag.tsx`、`utils/date.ts`、`utils/ticket-lifecycle.ts`、shared 包、其他页面文件。

## 2. 色板 token（tokens.css 末尾追加）

```css
/* CustomerWeb 服务台设计稿（Figma 132:155）色板 */
--ud-cx-primary: #4f45e5;
--ud-cx-primary-strong: #7d3bed;
--ud-cx-primary-soft: rgba(79, 69, 229, 0.08);
--ud-cx-cyan: #05b5d4;
--ud-cx-cyan-soft: rgba(5, 181, 212, 0.08);
--ud-cx-green: #0fba82;
--ud-cx-green-soft: rgba(15, 186, 130, 0.08);
--ud-cx-danger: #f04545;
--ud-cx-text: #1f2938;
--ud-cx-text-secondary: #6b7080;
--ud-cx-text-muted: #9ca3b0;
--ud-cx-bg-soft: #f7fafc;   /* 搜索框底 */
--ud-cx-divider: rgba(0, 0, 0, 0.1);
```

## 3. 图标组件映射（Icons.tsx 追加）

全部 `fill="currentColor"`，`aria-hidden`，沿用 `IconProps { className?: string }` 与 `iconClass()` 模式；宽高以 CSS 上下文控制（.ud-icon 1.25em 兜底，局部 CSS 覆写）。

| 新组件 | 来源 SVG | viewBox | 尺寸（CSS 覆写） | 用途 |
|---|---|---|---|---|
| `IconNavHome` | imgIconNavHome | 0 0 20 20 | 20px | 导航首页 |
| `IconNavTicket` | imgIconNavTicket | 0 0 20 20 | 20px | 导航工单 / 通知行 ticket |
| `IconNavChat` | imgIconNavChat | 0 0 20 20 | 20px | 导航咨询 |
| `IconNavBell` | imgIconNavBell | 0 0 20 20 | 20px | 导航通知 |
| `IconNavUser` | imgIconNavUser | 0 0 20 20 | 20px | 导航我的 |
| `IconNavGear` | imgFrame | 0 0 14 14 | 14px | 栏1 设置 |
| `IconNavMessage` | imgFrame1 | 0 0 14 14 | 14px | 栏1 消息 |
| `IconNavDomain` | imgFrame2 | 0 0 14 14 | 14px | 栏2 业务域 |
| `IconSearchSolid` | imgIconSolid | 0 0 18 18 | 18px | 顶栏搜索 |
| `IconStatDoc` | imgIconSolid1 | 0 0 24 24 | 24px | 统计-待处理 |
| `IconStatClock` | imgIconSolid2 | 0 0 24 24 | 24px | 统计-进行中 |
| `IconStatCheck` | imgIconSolid3 | 0 0 24 24 | 24px | 统计-已完成 |
| `IconNotifTicket` | imgFrame3 | 0 0 20 20 | 20px | 通知行 ticket 类 |
| `IconNotifShield` | imgFrame4 | 0 0 20 20 | 20px | 通知行 system 类 |

**实施注意**：SVG path 数据从 `agent-work/home-figma/<来源>.svg` 复制（保留原 d 属性，g 内单个 path）；导出图里 `<path id="Vector">` 的 d 即为所需。图标颜色全部由 CSS `color` 控制（激活紫 #4F45E5 / 闲置 #6B7080 / 深色 #1F2937 等）。**不要**保留导出图的固定 fill 色。

## 4. AppShell 重构

### 4.1 新结构

```tsx
<div className="ud-stage">
  <div className="ud-shell">
    <aside className="ud-rail" aria-label="主导航">
      <div className="ud-rail__header">
        <span className="ud-rail__logo" aria-hidden>U</span>
        <span className="ud-rail__wordmark">UnionDesk</span>
      </div>
      <nav className="ud-rail__nav" aria-label="主导航">
        {navItems.map(...)  // NavLink: icon + label + [二期角标] + [未读角标]
      </nav>
      <div className="ud-rail__spacer" />
      <div className="ud-rail__foot">
        <div className="ud-rail__foot-row ud-rail__foot-row--tools">  {/* 栏1 */}
          <button 设置 → /me><IconNavGear /></button>
          <span className="ud-rail__foot-divider" />
          <button 消息 → /inbox><IconNavMessage /><i 未读角标 /></button>
        </div>
        <button className="ud-rail__foot-row" onClick={() => navigate("/domains")}>  {/* 栏2 */}
          <IconNavDomain /><span>{activeDomain?.name ?? "未选择"}</span><em>▼</em>
        </button>
        <button className="ud-rail__foot-row" onClick={() => navigate("/me")}>  {/* 栏3 */}
          <span className="ud-rail__avatar">{avatarLetter}</span><span>{displayName}</span>
        </button>
      </div>
    </aside>

    <div className="ud-shell__body">
      <header className="ud-topbar">
        <div className="ud-topbar__left">
          <h1 className="ud-topbar__title">{title}</h1>
          <form className="ud-topbar__search" onSubmit={onSearch}>
            <IconSearchSolid /><input placeholder="搜索工单、通知..." ... />
          </form>
        </div>
        <div className="ud-topbar__right">
          <button className="ud-topbar__domain" onClick={() => navigate("/domains")}>  {/* 仅移动端显示 */}
            <strong>{activeDomain?.name ?? "未选择"}</strong>
          </button>
          <button className="ud-topbar__submit" onClick={() => navigate("/tickets/new")}>提交工单</button>
        </div>
      </header>
      <main className="ud-content"><Outlet /></main>
    </div>
  </div>
  <nav className="ud-dock">...</nav>  {/* 移动端 dock 不变 */}
</div>
```

### 4.2 关键 CSS（app.css）

- `--ud-rail-w: 76px → 240px`（tokens.css）；`.ud-rail`：白底（#fff）、右边框 rgba(0,0,0,0.06)、padding 顶部 12px 侧 0、`align-items: stretch`；brand header 高 ~77px 含顶部 1px #F3F4F6 分隔
- `.ud-rail__logo`：36×36 圆角 10 渐变（135deg #4F46E5→#06B6D4）白 U 字 800；`.ud-rail__wordmark`：17px 800 #1F2937（对齐 logo 基线）
- `.ud-rail__nav`：gap 4px，padding 0 12px
- `.ud-rail__item`：全宽 215px、高 41px、圆角 8、flex（icon 20px 左 16px + label 14px 左 12px gap）、闲置 #6B7080；hover rgba(79,69,229,0.05)；`is-active`：bg rgba(79,69,229,0.08) + `::before` 左 3px×20px #4F45E5 竖条（居中） + 文字/图标 #4F45E5
- 角标：`.ud-rail__badge` 高 21px 圆角 6 bg #F04545 白 10px 字（padding 0 7px），在 label 后 12px 处（flex inline，非 absolute）；`.ud-rail__soon` 同款（文案「二期」）
- `.ud-rail__foot`：padding 8px 16px 16px；row 高 36px flex 对齐；栏1 图标钮 28×28 圆角 6（icon 14px 居中）、竖线 1px rgba(0,0,0,0.1) 高 20px、消息钮同款 + 未读角标（右上 -3px，14px 圆点 bg #F04545 白 9px）；栏2/栏3 hover bg 圆角 6（24px 高，内容 13px #262E3D，栏2 右「▼」11px #8C94A6）；栏3 头像 20px 圆角 10 渐变 135deg #4F45E5→#05B5D4 白字 9px
- `.ud-topbar`：高 64px、白底、padding 0 32px、分隔线 rgba(0,0,0,0.06)；标题 24px 800 #1F2938；`.ud-topbar__search`：400px（flex 自适应 min(400px, 100%)）、高 40px 圆角 16 bg #F7FAFC 无边框、icon 左 14px #64748B 18px、placeholder 14px #757575；`.ud-topbar__submit`：120×39 圆角 8 bg #4F45E5 白 14px 600（hover 加深）
- `.ud-topbar__domain`：默认 `display: none`；`@media (max-width: 899px)` 显示（沿用现样式）；`.ud-topbar__avatar`：删除（身份迁入侧边栏栏3）
- 移动端：`.ud-topbar__submit` 在 `@media (max-width: 480px)` 隐藏；`.ud-rail` <900px 隐藏（现有）

### 4.3 数据绑定

- 头像/姓名：`portal.account?.displayName?.trim()?.[0]`（大写兜底 U）+ 全名
- 业务域：`portal.activeDomain?.name ?? "未选择"`
- 未读角标（导航通知 + 栏1消息）：`portal.unreadCount`（>9 → 9+，沿用现有逻辑）
- 搜索：现有 onSearch（回车 → `/tickets?q=` 或 `/tickets`），仅 placeholder 改为「搜索工单、通知...」

## 5. 首页重写（pages/home/index.tsx）

### 5.1 结构

```tsx
<div className="ud-dash">   // grid gap 20
  <section className="ud-welcome">               // 渐变欢迎卡
    <div className="ud-welcome__copy">
      <h1>{greetingByHour(...)}，{displayName}</h1>
      <p>{pending > 0 ? `有 ${pending} 个待处理的工单需要您的关注` : "暂时没有待处理的工单"}</p>
    </div>
    <Link to="/tickets?life=pending" className="ud-welcome__btn">查看待处理</Link>
  </section>

  <section className="ud-stat-grid">             // 统计 ×3
    <Link className="ud-stat-card ud-stat-card--pending" to="/tickets?life=pending">
      <span className="ud-stat-card__icon"><IconStatDoc /></span>
      <strong>{life.pending}</strong><span>待处理</span>
    </Link>
    ... active(IconStatClock, #05B5D4) / done(IconStatCheck, #0FBA82)
  </section>

  <section className="ud-dash-grid">             // 双栏
    <div className="ud-card">                     // 我的工单
      <header className="ud-card__head">
        <h2>我的工单</h2><Link to="/tickets">查看全部 →</Link>
      </header>
      {recent.map(t => (
        <Link to={`/tickets/${t.id}`} className="ud-ticket-row">
          <span className="ud-tag ud-tag--blue">{t.typeName}</span>
          <span className="ud-ticket-row__title">{t.title}</span>
          <StatusTag status={t.status} />
          <time>{relativeTime(t.updatedAt)}</time>
        </Link>
      ))}
      {recent.length === 0 && <div className="ud-empty">还没有工单，试着提交第一个吧。</div>}
    </div>

    <div className="ud-card ud-card--notif">      // 未读通知 380px
      <header className="ud-card__head">
        <h2>未读通知</h2><Link to="/inbox">全部通知 →</Link>
      </header>
      {notifs.map(m => (
        <button className="ud-notif-row" onClick={() => navigate(safeJump(m.jumpUrl))}>
          <span className="ud-notif-row__icon">{m.kind === "system" ? <IconNotifShield /> : <IconNotifTicket />}</span>
          <span className="ud-notif-row__body">
            <span className="ud-notif-row__title">{m.title}</span>
            <time>{relativeTime(m.createdAt)}</time>
          </span>
        </button>
      ))}
    </div>
  </section>

  <p className="ud-foot-copy">© 2026 UnionDesk · 客户服务中心</p>
</div>
```

### 5.2 数据绑定

- `greetingByHour`：沿用现有（上午好/下午好/晚上好）
- 统计：`countByLifecycle(portal.currentDomainTickets.map(t => t.status))` → life.pending/active/done
- 我的工单：`currentDomainTickets.slice(0, 5)`；行：typeName 标签（ud-tag--blue）+ 标题 14px 截断 + StatusTag + 相对时间
- 未读通知：`inboxMessages` 未读优先排序后 `slice(0, 3)`；`kind === "system"` → 盾牌图标，其余 → 工单图标；跳转 `jumpUrl`（复用 inbox 页安全逻辑：`/workspace` 前缀 → `/home`，空 → `/home`）
- 相对时间 `relativeTime(createdAt)`：私有函数（<1min 刚刚 / <1h n 分钟前 / <24h n 小时前 / <7d n 天前 / 否则 formatDateTime）
- 空域状态：保留现有「先选择一个服务空间 / 去选择业务域」玻璃卡分支
- 顶部 hero 的 `refreshCustomerTicketsLive()` useEffect 保留（数据刷新）

### 5.3 关键 CSS

- `.ud-welcome`：linear-gradient(90deg, #4F45E5, #7D3BED)、高 138px、圆角 20、白字、内边距 32px、flex 两端；标题 28px 700、副文 16px rgba(255,255,255,0.92)；`.ud-welcome__btn`：120×45 圆角 8 bg rgba(255,255,255,0.2) 白 14px
- `.ud-stat-grid`：grid 3 列 gap 20（窄屏 1 列）；`.ud-stat-card`：白卡 圆角 16 padding 25px、hover 上浮；`__icon` 48px 圆角 12 浅底（--pending→--ud-cx-primary-soft / --active→--ud-cx-cyan-soft / --done→--ud-cx-green-soft），svg 24px #1F2937；`strong` 32px 800 主题色（待处理 #4F45E5 / 进行中 #05B5D4 / 已完成 #0FBA82）；标签 14px #6B7380
- `.ud-dash-grid`：grid `1fr 380px` gap 20（<1100px 单列堆叠，通知卡 380px 全宽或自动）；`.ud-card`：白卡圆角 16；`__head` 高 ~60px padding 0 25px flex 两端，h2 18px #1F2938，链接 14px #4F45E5
- `.ud-ticket-row`：flex（标签 + 标题 flex:1 截断 + 状态 + 时间 12px #9CA3B0）高 ~56px 分隔线 rgba(0,0,0,0.05)；hover bg #F7FAFC
- `.ud-notif-row`：flex（40px 图标块圆角 12 浅紫底 + 14px 标题 + 12px #9CA3B0 时间）高 ~78px 分隔线；hover bg #F7FAFC
- `.ud-foot-copy`：居中 12px #9CA3B0，padding 8px 0 24px
- 空域分支、`.ud-empty` 复用现有样式

## 6. 兼容性

- 侧边栏 76→240px 影响全部页面（内容区变宽）：`--ud-content-max` 1080→1136px（tokens.css 单行改值）
- 顶栏 avatar 移除：`/me` 桌面可达（侧边栏我的 + 栏3）、移动端可达（dock 我的）
- 域 chip 桌面移除：桌面经侧边栏栏2；移动端顶栏保留
- 现有 `.ud-rail__brand`（旧 logo 方块）删除或复用为 `.ud-rail__logo`；`.ud-rail__soon/.ud-rail__badge` 类名保留但样式重写
- `.ud-rail__item` 的旧 CSS（52px 竖排）整体替换；dock 样式不变（NavItems 组件渲染结构改为横向时 dock 分支同步适配——dock 项保持图标+文字竖排，用现有 .ud-dock__item）

## 7. 风险与回滚

- 图标 path 复制错误 → 浏览器冒烟逐图标目检；回滚点 = Icons.tsx 新增块
- 侧边栏改版影响其他页面 → 逐页冒烟（工单/通知/我的/业务域/咨询）
- git 基准：无新增依赖、无后端改动；如视觉偏差，仅需还原 app.css/.tsx 内新增样式块
