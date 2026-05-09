# CustomerWeb 设计系统生成清单

> 适用范围：`UnionDeskWeb/apps/UnionDeskCustomerWeb` 的设计系统文档层
> 约束：只生成 / 维护 `design-system/uniondesk-customer/MASTER.md` 及页面级覆盖，不改业务代码

## 开发清单

- [x] 1. 按要求执行 UI UX Pro Max 检索命令，生成 CustomerWeb 设计系统初稿
  - [x] 1.1 生成全局设计系统 `MASTER.md`
  - [x] 1.2 生成页面级覆盖：`login`
  - [x] 1.3 生成页面级覆盖：`domains`
  - [x] 1.4 生成页面级覆盖：`workspace`
  - [x] 1.5 生成页面级覆盖：`ticket-detail`
  - [x] 1.6 生成页面级覆盖：`inbox`

- [x] 2. 复核落盘结果，确认文件仅位于 design-system 目录
  - [x] 2.1 确认未改动 CustomerWeb 业务代码
  - [x] 2.2 确认未触碰 Shared rename 相关文件

- [x] 3. 整理设计系统输出，保证后续页面重构可直接读取
  - [x] 3.1 统一全局配色、排版、间距与动效口径
  - [x] 3.2 将各页面覆盖收敛为差异规则

## 完成定义

- `design-system/uniondesk-customer/MASTER.md` 可作为 CustomerWeb 全局设计系统入口
- `design-system/uniondesk-customer/pages/*.md` 提供页面级差异规则
- 本次不产生任何业务代码改动

