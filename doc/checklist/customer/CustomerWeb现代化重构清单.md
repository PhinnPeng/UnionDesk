# CustomerWeb 现代化重构清单

> 适用范围：`UnionDeskWeb/apps/UnionDeskCustomerWeb`
>
> 目标：使用 UI UX Pro Max 技能将客户端所有页面升级为面向终端客户的现代服务门户。

## 前置条件

- [x] P0 命名重构完成，源码层零 `P0` 残留
- [x] 功能闭环已验证（54/54 测试全过）
- [x] UI UX Pro Max 设计系统 MASTER.md 已生成

## 开发任务

- [x] D-0. 设计系统初始化
  - [x] 运行 `--design-system --persist` 生成 CustomerWeb 设计系统 MASTER.md
  - [x] 运行 `--stack react` 生成 React 技术栈指南
  - [x] 运行 `--domain ux` 生成 UX 最佳实践

- [x] D-1. 登录/注册页重构（`login/`）
  - [x] 运行 `--page "login"` 生成登录页覆盖
  - [x] 重构为居中卡片 + 品牌 logo + 渐变背景
  - [x] 保留全部真实 API（loginAuth / registerCustomer / requestPasswordReset / confirmPasswordReset）
  - [x] 保留 SliderCaptcha，样式覆盖为设计系统配色
  - [x] 删除 LoginPage.css（如存在）
  - [x] 检查点：登录 + 注册 + 忘记密码 + 验证码滑块全部功能正常

- [x] D-2. 业务域选择页重构（`domains/`）
  - [x] 运行 `--page "domains"` 生成域选择页覆盖
  - [x] 重构为域卡片网格 + 入域/选择交互
  - [x] 保持 fetchDomains 真实 API + portal fallback 双层策略
  - [x] 检查点：域列表展示 + 加入/选择域 + 邀请码入域正常

- [x] D-3. 工作台（首页）重构（`home/`）
  - [x] 运行 `--page "workspace"` 生成工作台页覆盖
  - [x] 重构为统计摘要卡片 + 快速提单区 + 我的工单列表 + 通知预览
  - [x] 数据源保持 Shared 包 API + portal fallback 双层策略
  - [x] 检查点：数据加载 + 提单 + 撤回 + 通知功能正常

- [x] D-4. 工单详情页重构（`tickets/detail.tsx`）
  - [x] 运行 `--page "ticket-detail"` 生成工单详情页覆盖
  - [x] 重构为对话流式（chat 气泡）+ 回复输入 + 附件上传 + 状态卡
  - [x] 检查点：详情加载 + 回复 + 附件 + 撤回正常

- [x] D-5. 通知中心页重构（`inbox/`）
  - [x] 运行 `--page "inbox"` 生成通知页覆盖
  - [x] 重构为现代化通知列表 + 未读角标 + 分类筛选
  - [x] 检查点：列表加载 + 标记已读 + 跳转正常

- [x] D-6. PortalShell 全局布局重构
  - [x] 统一顶部导航 / 侧边栏
  - [x] 品牌色、字体、间距统一到设计系统
  - [x] 响应式（移动端折叠导航）
  - [x] 检查点：页面切换 + 导航高亮 + 响应式正常

- [x] D-7. 通知偏好设置页纳入客户服务门户
  - [x] 新增 `settings/notification-preferences` 页面并接入本地回退保存
  - [x] 设计系统规则已覆盖通知偏好页
  - [x] 后端接口缺口已在回报中明确为阻塞项，不硬编真实 API
  - [x] 修复 PortalShell 与通知偏好页的按钮命名重复问题，保持测试可访问名稳定

## 最终验证

- [x] `pnpm --dir UnionDeskWeb/apps/UnionDeskCustomerWeb typecheck` 通过
- [x] `pnpm --dir UnionDeskWeb/apps/UnionDeskCustomerWeb test --run` 通过（54 测试）
- [x] 最新回归验证通过（58/58）
- [x] 无 `P0` 命名残留
- [x] 无自建 fetch/axios（统一 @uniondesk/shared）
- [x] LoginPage.css 已删除或合并
- [x] UI UX Pro Max Pre-Delivery 代码级检查通过

## 完成定义

- 所有页面按 UUPM 设计系统重构。
- 布局统一、品牌一致、响应式适配。
- typecheck + test 全绿。
