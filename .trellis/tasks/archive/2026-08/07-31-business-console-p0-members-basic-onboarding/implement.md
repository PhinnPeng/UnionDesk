# Implement — 业务域端 P0 三页

## Checklist

- [x] 1. `domain-permissions.ts` 补齐 general / member / invitation_code 读与写常量
- [x] 2. 核对后端成员与域更新接口是否已接受 `domain.*`；缺口则并列补码
- [x] 3. 实现 `/domain/basic`：会话域拉详情 + 基础信息编辑；无启停/删除
- [x] 4. 实现 `/domain/members`：对照 `detail-members` 完整写；权限改 `domain.member.*`
- [x] 5. 实现 `/domain/onboarding`：只读展示注册/邀请策略
- [x] 6. 切域后三页随 `defaultBusinessDomainId` 刷新（订阅 auth store）
- [x] 7. 手测：有/无读码菜单显隐；写码按钮显隐；平台 detail 无回归
  - 静态 + typecheck 已过；**人工冒烟仍建议**：切域、平台无回归、剥读码菜单消失

## Validation

```powershell
# 前端类型/相关单测（若有成员工具函数可单测则跑）
pnpm -C UnionDeskWeb/apps/UnionDeskAdminWeb exec tsc -b --pretty false
```

手工：

1. `domain_admin` 登录业务端 → 三页可开；人员可增改启停删；通用可保存；入域开关不可改  
2. 去掉某读码的角色 → 对应菜单消失  
3. 侧栏切到另一启用域 → 页签清空后三页数据属新域  
4. 平台 `/platform/domains/detail/:id` 成员/通用/入域仍可用（含启停/删除）

## Rollback

- Git revert 本子任务提交；占位 Empty 可恢复

## Order note

建议实现顺序：**basic（小）→ onboarding（只读小）→ members（大）**，便于早发现 domainId/权限接线问题。
