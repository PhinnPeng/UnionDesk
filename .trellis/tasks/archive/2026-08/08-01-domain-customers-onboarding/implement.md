# 实现计划：业务域入域写能力与邀请码 CRUD

## Checklist

1. [x] shared：`normalizeP0InvitationCode`；增强 `fetchP0InvitationCodes`；新增 `createP0InvitationCode` / `deleteP0InvitationCode`；`CreateP0InvitationCodePayload` 类型
2. [x] `domain-permissions.ts`：补齐 CREATE / DELETE 常量
3. [x] 重写 `pages/domain/onboarding/index.tsx`：可写策略 + 邀请码 Tab（列表/分页/创建 Modal/删除）
4. [x] 自检：权限分支、关闭确认、空域、API 错误提示；相关文件 lint；AdminWeb `tsc --noEmit` 通过

## Validation

```bash
# 类型/构建（按仓库习惯择一）
pnpm -C UnionDeskWeb/packages/shared exec tsc --noEmit
# 或开发服冒烟：业务域登录 → 客户管理/入域配置 → 开关 + 邀请码 CRUD
```

## Review gates

- PRD AC 全部可勾选
- 无 CustomerWeb / 菜单迁移夹带改动

## Rollback

还原 shared api/types、domain-permissions、onboarding 页面三处即可。
