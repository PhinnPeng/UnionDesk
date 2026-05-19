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

## 图标分页
- [x] 搜索模式改为 Iconify `/search` 服务端分页，使用 `start + limit`，不再截断为前 120 条
- [x] 浏览模式改为 collection 全量拉取后前端分页，不再截断为前 240 条
- [x] 图标选择器底部增加分页控件，搜索和浏览都可翻页
- [x] 搜索分页使用 `pageSize + 1` 估算总量，避免 `total` 被截断后隐藏分页
- [x] 图标数据改为本地离线目录，图标选择器不再请求 `api.iconify.design`

## 树表格展开收缩
- [x] 菜单管理页补充一键展开和缩回
- [x] 与部门页保持一致的树表格展开交互
- [x] 补充展开/缩回的单测与浏览器验证
