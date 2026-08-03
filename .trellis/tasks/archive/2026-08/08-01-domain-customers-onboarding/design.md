# 设计：业务域入域写能力与邀请码 CRUD

## Boundaries

| In | Out |
|----|-----|
| `pages/domain/onboarding/index.tsx` | CustomerWeb |
| `packages/shared` invitation API | 客户列表页大改 |
| `domain-permissions.ts` | 菜单 Flyway / 侧栏 |
| 行为对齐平台 `detail-onboarding` | 平台 panel 强制共用组件 |

## Contracts

### Domain policy

- Read: `GET /admin/domains/{id}` → `fetchAdminDomain`
- Write: `PUT /admin/domains/{id}` body `{ registration_enabled | invitation_enabled: "allowed"|"disallowed" }` → `updateAdminDomain`
- Permission: read `domain.general.read`；write `domain.general.update`

### Invitation codes

- List: `GET .../invitation-codes?page&page_size` → `fetchP0InvitationCodes`（增强归一化）
- Create: `POST .../invitation-codes` body `{ channel?, expires_at?, max_uses? }`（后端 `@JsonAlias` 亦接受 camelCase）
- Delete: `DELETE .../invitation-codes/{codeId}` → 204
- Permissions: `domain.invitation_code.read|create|delete`

## UI shape

```
BasicContent
└── AuthGuarded [general.read OR invitation_code.read]
    └── Card「入域管理」
        └── Tabs
            ├── 客户注册配置 → 可写 Switch（general.update）
            ├── 客户邀请配置 → 可写 Switch（general.update）
            └── 邀请码（仅 invitation_code.read）
                ├── extra: 新建（create）
                ├── Table + pagination
                └── 行内删除（delete + ConfirmPopover）
```

## Data flow

1. 解析当前 `domainId`（defaultBusinessDomainId / accessibleDomains[0]）。
2. 并行：加载域详情；若有邀请码读权限则加载邀请码第一页。
3. 策略变更 → PUT → 更新本地 `domain`。
4. 创建/删除邀请码 → POST/DELETE → 重新拉列表当前页（删除后若空页回退上一页）。

## Tradeoffs

- **不抽共享组件到 platform**：业务域页需完整写能力与分页；平台 panel 仍只读列表。后续若两侧都要写，再抽 `InvitationCodesPanel`。
- **页面入口 OR 权限**：仅有 general 无 invitation 读时仍可改策略；仅有 invitation 读无 general 时可见邀请码与只读策略。

## Compatibility / Rollback

- 纯前端 + shared 客户端封装；后端已就绪。回滚即还原上述文件即可。
