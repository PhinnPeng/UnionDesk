# Design — 业务域端 P1 三页

## Boundaries

| 层 | 范围 |
|:---|:---|
| 前端 | `pages/domain/{customers,blockwords,config}`；`domain-permissions.ts` 补 customer / blocked_word / config |
| API | 复用 shared 客户/屏蔽词/域配置 API |
| 后端 | 缺口则并列 `@RequirePermission` 的 `domain.*` |
| 非范围 | 日志/角色/概览 KPI |

## domainId

同 P0：`defaultBusinessDomainId > 0` 优先，否则 `accessibleDomains[0]`。

## 对照

| 业务页 | 平台参考 | 差异 |
|:---|:---|:---|
| `customers/index.tsx` | `detail-customers.tsx` | `domain.customer.*` + 会话域 |
| `blockwords/index.tsx` | `detail-blockwords.tsx` | `domain.blocked_word.*` + 会话域 |
| `config/index.tsx` | `detail-config.tsx` + `DomainConfigPanel` | 会话域；确认 Panel 内权限是否可注入/已用 `domain.config.*` |

## 权限常量

| 码 |
|:---|
| `domain.customer.read/create/update_status` |
| `domain.blocked_word.read/create/delete` |
| `domain.config.read/update` |

## 风险

- `DomainConfigPanel` 若写死平台权限码 → 业务页需传权限或 fork 最小改动支持业务码。
- 客户面板体积大 → 复制改编，勿改平台源文件行为。
