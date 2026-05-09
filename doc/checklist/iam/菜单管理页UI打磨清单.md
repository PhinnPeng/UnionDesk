# 菜单管理页 UI 打磨清单

## 目标
- 表格序号与展开按钮垂直居中对齐
- 按钮节点与叶子节点不显示展开图标
- 组件路径列去掉前导 `./`，注册表下拉 value 也统一规范化
- 弹窗表单 Label 左对齐，并为各字段补充问号 tooltip

## 任务
- [x] 补充表格相关测试，覆盖展开图标隐藏与组件路径显示
- [x] 补充弹窗表单相关测试，覆盖 labelAlign 和 tooltip 绑定
- [x] 实现表格 UI 打磨
- [x] 实现弹窗表单 UI 打磨
- [x] 浏览器回归验证

## 验收
- `pnpm --dir UnionDeskWeb/apps/UnionDeskAdminWeb typecheck` 通过
- `pnpm --dir UnionDeskWeb/apps/UnionDeskAdminWeb test --run` 全绿
- 浏览器中四个场景均符合预期
- 菜单弹窗已确认显示左对齐 Label 和问号提示
