# CustomerWeb 登录页设计落地（Figma 132:2 玻璃卡风格）

## Goal

按 Figma 设计稿 132:2「Pixso 迁移 · 登录页」（1440×1024）重构 CustomerWeb 登录页视觉：渐变背景 + 光晕 + **居中玻璃卡**（欢迎回来 / 账号 / 密码 / 滑动验证 / 链接行 / 登录按钮）+ 左上品牌 + **底部居中版权**。沿用现有滑动验证组件（LoginCaptcha + shared SliderCaptcha，不改动）；登录交互与跳转流程不变。

## Background（已确认事实）

- 设计稿 132:2 结构：`div.main` 渐变背景（#EFF6FF→#EEF2FF→#ECFEFF）+ 3 处光晕（蓝/青/紫 radial）→ 居中玻璃卡 `div.glass`（420×598，白 72% 透明、圆角 24、毛玻璃 backdrop-blur、内高光 + 底部阴影）；卡内自上而下：卡头（「欢迎回来」+「使用客户账号登录，随后选择业务域」）→ 账号输入（346×48）→ 密码输入 → 滑动验证区（div.captcha 346×48）→ 链接行 div.links（**左：☐ 记住账号（勾选框）；右：注册账号 | 忘记密码（竖线分割）**）→ 登录按钮（渐变胶囊 999，文案「登录」）；左上品牌 `div.brand`（渐变 U + UnionDesk）；底部版权 `div.foot-copy`（**设计稿左下角 → 按用户约束落地为底部居中**）
- 现状：`apps/UnionDeskCustomerWeb/src/pages/login/index.tsx`（249 行，左右分栏：左品牌+3 张轮播图 slide-*.png / 右表单卡）+ `login.css`（417 行，--auth-* 蓝调变量，**未使用 tokens.css 的 --ud-* 玻璃拟态变量**）；无版权 footer；表单含 LoginCaptcha（滑动验证，enabled 由 fetchLoginConfig 控制）；专属入口 `/d/:domainCode/login` 复用本页
- `styles/tokens.css` 已有与设计稿一致的玻璃拟态 token：`--ud-canvas`（145deg #eff6ff→#eef2ff 50%→#ecfeff，与 132:2 背景同源）、`--ud-surface` rgba(255,255,255,0.62)、`--ud-primary` #1e40af、`--ud-radius-sm/md/lg/pill`、`--ud-shadow/--ud-shadow-soft` → 落地应复用
- 用户约束：① 滑动验证沿用现有组件；② 底部版权信息落地时**居中**
- 调研确认：CustomerWeb 无 forgot-password 页面、无记住账号逻辑；注册路由 `/register` 已存在（registerPath 含专属域变体）

## Requirements

- R1 布局：移除左右分栏与轮播图结构，改为全屏渐变背景 + 光晕 + 垂直水平居中的玻璃卡（窄屏降级：卡片全宽/滚动，见 AC5）
- R2 玻璃卡：复用 tokens.css token（--ud-surface 系、--ud-radius-lg、--ud-shadow）；毛玻璃 backdrop-filter；内高光描边（白 90% 1px）
- R3 表单：账号（placeholder「手机号或登录名」）、密码（placeholder「输入密码」）、LoginCaptcha（沿用，enabled/hint 逻辑不变）、提交按钮文案「继续」→「**登录**」，样式渐变胶囊（#1E40AF→#3B82F6，圆角 999）
- R4 链接行：左「☐ 记住账号」（勾选框视觉，**D1 已确认：勾选后 localStorage 记住 loginName，进入页面预填+默认勾选**）；右「注册账号 | 忘记密码」（竖线 1px #D1D5DB 分割；注册账号 → registerPath；忘记密码 → toast「忘记密码功能开发中」（D2））
- R5 品牌：左上角渐变 U（#14B8A6→#8B5CF6 或现有 auth__mark 渐变，按 tokens 统一）+ UnionDesk，点击回 /login（现有行为）
- R6 版权：底部**居中**（约束 ②），文案占位「© 2026 UnionDesk · 客户服务中心」（可编辑常量）
- R7 专属入口模式完整保留：/d/:domainCode/login 复用本页，「专属入口 · {域名}」提示块保留；演示提示「演示：customer / customer123」保留（现有 auth__hint）
- R8 交互/流程不变：表单提交、验证码校验（captchaToken 依赖）、登录成功跳转（专属→enterDedicatedDomain；普通→activeDomain ? /home : /domains）、风险登录提示、失败重置验证码（captchaKey++）
- R9 样式落位：login.css 改用 tokens.css 的 --ud-* 变量体系（玻璃拟态）；删除 --auth-* 蓝调变量与轮播图相关样式（.auth__story/.auth__carousel 等）

## Decisions（已确认）

- D1 记住账号：**真实落地**——勾选后 localStorage 记住 loginName（本地浏览器方式）；下次进入登录页预填账号并默认勾选。存储 key 命名 `ud_login_remembered`（与 shared storage 风格一致）；仅存 loginName，不存密码
- D2 忘记密码：CustomerWeb 无对应页面 → 点击 toast「忘记密码功能开发中」，不新增页面
- D3 品牌 mark 渐变：统一为 tokens 渐变（#14B8A6→#8B5CF6 现有品牌色）而非设计稿蓝色系

## Acceptance Criteria

- [ ] AC1 1440×1024 下与 132:2 视觉对齐：渐变背景、光晕、居中玻璃卡（卡头/账号/密码/验证码/链接行/登录按钮/品牌/版权）
- [ ] AC2 版权文字底部**居中**显示
- [ ] AC3 滑动验证组件零改动（git diff 确认 LoginCaptcha.tsx / shared SliderCaptcha 无变更）
- [ ] AC4 登录成功/失败/验证码未通过/专属入口跳转与重构前行为一致；记住账号勾选后刷新页面账号预填且勾选保持
- [ ] AC5 窄屏（<900px）玻璃卡不溢出、可滚动登录，背景渐变保留
- [ ] AC6 登录按钮 hover/active/disabled 态正常；链接行 hover 态正常
- [ ] AC7 前端 typecheck（customer-web）+ check:utf8 通过；浏览器冒烟：登录成功进入 /home 或 /domains

## Out of Scope

- AdminWeb 登录页
- 忘记密码页面实现（D2 仅入口）
- 记住账号持久化（D1 默认仅 UI）
- 轮播图内容迁移（资源文件保留，不删除）
- 登录后域选择流程（另有任务 08-07-domain-first-select）

## Technical Notes

- 改动文件：`apps/UnionDeskCustomerWeb/src/pages/login/index.tsx`（结构重构）、`login.css`（重写为玻璃拟态，复用 --ud-*）、必要时 `styles/tokens.css`（补充缺失 token，如渐变按钮色）
- 无新增依赖：光晕用 radial-gradient，毛玻璃用 backdrop-filter（已受支持）
- 详细设计见 design.md（如有），执行计划见 implement.md
