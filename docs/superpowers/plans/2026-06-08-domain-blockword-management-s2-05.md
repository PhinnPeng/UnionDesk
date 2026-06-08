---
change: domain-blockword-management-s2-05
design-doc: docs/superpowers/specs/2026-06-07-domain-blockword-management-s2-05-design.md
base-ref: cf250ed7fd3cd5d73695b10c411e962aa6436084
archived-with: 2026-06-08-domain-blockword-management-s2-05
---

# US-S2-05 双层屏蔽词库 — 实施计划

## 任务

1. Flyway + PermissionCodes + AdminPermissionCatalog + 菜单（平台页 + 域 Tab catalog）
2. BlockedWordService/Mapper/Repository + 双 Controller + 单测
3. shared 分页/batch API + 类型
4. 平台页 `blockwords/index.tsx`（独立）
5. 域 Tab `detail-blockwords.tsx` + sider 门控 + 权限常量 + labels + com-registry
6. 测试与 tasks 勾选

## 检查点

- [ ] Maven test blockedword + domain 相关通过
- [ ] AdminWeb typecheck
- [ ] 菜单 `/platform/blockwords` 与域 Tab 按钮 Flyway 就绪
