# AdminWeb 乱码恢复与页面重建清单

> 适用范围：`UnionDeskWeb/apps/UnionDeskAdminWeb`
>
> 目标：先恢复所有可通过 Git 还原的 tracked 文件，再重建 7 个 untracked 页面，最后补回之前丢失的增量改动，并完成编码与类型验证。

## 执行顺序

1. Fix-Alpha：批量恢复 tracked 乱码文件
   - [x] 恢复 `UnionDeskWeb/apps/UnionDeskAdminWeb/src/`
   - [x] 恢复 `UnionDeskWeb/apps/UnionDeskCustomerWeb/src/`
   - [x] 恢复 `UnionDeskWeb/packages/shared/src/`
   - [x] 检查 `git diff --stat` 仅保留预期的未恢复项

2. Fix-Beta：重建 7 个 untracked 页面
   - [x] 删除 7 个乱码文件
   - [x] 重建 `platform/audit-logs`
   - [x] 重建 `platform/domain-config`
   - [x] 重建 `platform/sla-management`
   - [x] 重建 `platform/system-settings`
   - [x] 重建 `platform/ticket-detail`
   - [x] 重建 `system/menu/components/icon-picker`
   - [x] 完成页面类型检查与 UTF-8 检查

3. Fix-Gamma：恢复增量变更
   - [x] 恢复菜单弹窗权限码分组下拉
   - [x] 恢复 `componentKey` 下拉选择
   - [x] 补齐中英文文案
   - [x] 重新应用菜单管理与角色管理的设计系统覆盖

## 验收结果

- [x] `pnpm --dir UnionDeskWeb run check:utf8`
- [x] `pnpm --dir UnionDeskWeb/apps/UnionDeskAdminWeb typecheck`
- [x] `pnpm --dir UnionDeskWeb/apps/UnionDeskAdminWeb test --run`
- [x] `pnpm --dir UnionDeskWeb/apps/UnionDeskCustomerWeb typecheck`
- [x] `pnpm --dir UnionDeskWeb/apps/UnionDeskCustomerWeb test --run`
