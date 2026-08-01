# Implement — 业务域端 P1 三页

## Checklist

- [x] 1. `domain-permissions.ts` 补 customer / blocked_word / config
- [x] 2. 后端客户/屏蔽词/域配置接口并列 `domain.*`（若缺）
- [x] 3. 实现 `/domain/config`（优先复用 DomainConfigPanel + 会话域）
- [x] 4. 实现 `/domain/blockwords`
- [x] 5. 实现 `/domain/customers`
- [x] 6. 切域刷新；typecheck
- [x] 7. 确认平台 detail 三 Tab 无回归
  - 静态/typecheck 已过；建议人工冒烟平台 Tab + 切域

## Validation

```powershell
pnpm -C UnionDeskWeb/apps/UnionDeskAdminWeb exec tsc -b --pretty false
```

手工：三页主流程 + 无读码隐藏 + 切域 + 平台无回归。

## Order

config（小）→ blockwords（中）→ customers（大）。
