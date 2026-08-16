# Context Map

## Contexts

- [CustomerWeb 服务台](./docs/context/customer-web.md) — 客户自助门户：提交/跟踪工单、发起咨询、接收通知、评价服务
- [工单域（Ticket Domain）](./docs/context/ticket-domain.md) — 员工端与域管理后台：SLA 配置与计时、超时动作、满意度评价

## Relationships

- **CustomerWeb 服务台 → 工单域**：客户操作映射为工单创建/回复/撤回/删除/满意度评价，状态经生命周期桶聚合后展示
- **工单域 → CustomerWeb 服务台**：SLA 配置产出时限与超时动作，影响工单处理节奏与客户感知的完成状态
