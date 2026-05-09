# CustomerWeb 客户端 P0 闭环开发清单

> 适用范围：`UnionDeskWeb/apps/UnionDeskCustomerWeb` 及其依赖的 `UnionDeskWeb/packages/shared`
> 依据文档：`doc/产品需求文档 v1.0.md`、`doc/P0接口契约表.md`、`doc/P0验收用例表.md`

## 目标

- 补齐客户注册/登录、选择业务域、邀请码入域、提单、我的工单、工单详情、撤回、附件上传、通知查看闭环。
- 确保客户可以完成 `注册或登录 -> 入域 -> 提交工单 -> 查看状态 -> 查看回复`。
- 补充必要的组件测试与 `typecheck`。

## 开发项

- [x] 1. 补齐 `shared` 客户端 P0 类型与 API 封装
  - [x] 1.1 增加客户端域列表、入域、工单、通知、附件、注册登录相关类型
  - [x] 1.2 增加客户端 P0 API 封装与导出
  - [x] 1.3 确认共享存储字段可承载客户端选择域与会话信息

- [x] 2. 改造 CustomerWeb 路由与页面入口
  - [x] 2.1 登录/注册入口区分客户身份
  - [x] 2.2 默认进入“业务域选择/最近使用域”入口
  - [x] 2.3 未入域时引导邀请码入域

- [x] 3. 实现客户工单闭环页面
  - [x] 3.1 业务域选择页
  - [x] 3.2 邀请码入域页
  - [x] 3.3 提单页
  - [x] 3.4 我的工单列表页
  - [x] 3.5 工单详情页
  - [x] 3.6 撤回操作与状态展示
  - [x] 3.7 附件上传与通知查看

- [x] 4. 补充组件测试与类型检查
  - [x] 4.1 为关键状态组件补最小测试
  - [x] 4.2 运行 `customer-web` 与 `shared` 的 `typecheck`
  - [x] 4.3 记录验证结果与残余风险

## 完成定义

- 客户端可以顺利完成主闭环。
- `typecheck` 通过。
- 关键组件具备最小覆盖测试。
- 已记录剩余风险与验证结果。

## 验证记录

- 2026-05-04: `pnpm --dir UnionDeskWeb/apps/UnionDeskCustomerWeb test --run` 通过，54/54。
- 2026-05-04: `pnpm --dir UnionDeskWeb/apps/UnionDeskCustomerWeb typecheck` 通过。
