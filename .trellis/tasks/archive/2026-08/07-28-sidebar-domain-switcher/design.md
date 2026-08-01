# 侧栏业务域切换 — 技术设计

## 0. Boundaries

| 层 | 范围 |
|:---|:---|
| 数据 | 新建 `user_config`；不改 `staff_account` 列 |
| 后端 | 登录域解析；设默认；切当前会话（含 token/session） |
| 前端 | AdminWeb 业务侧栏 `DomainSwitcherBar` + auth store 语义 |
| 非范围 | 客户门户、平台侧栏切换、OpenSpec 大文档同步（可后续） |

## 1. Data: `user_config`

对齐 `domain_config` KV 形态；按 AGENTS 约定**不加外键**，由业务校验 `user_id` / 域可达性。

```sql
CREATE TABLE user_config (
  id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id      BIGINT UNSIGNED NOT NULL,
  config_key   VARCHAR(128) NOT NULL,
  config_value TEXT,
  value_type   VARCHAR(16) NOT NULL DEFAULT 'string',
  description  VARCHAR(255) DEFAULT NULL,
  updated_at   DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
               ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_config (user_id, config_key),
  KEY idx_user_config_user (user_id)
) COMMENT='用户级配置（KV）';
```

| Key | Value | 说明 |
|:---|:---|:---|
| `default_business_domain_id` | 域 ID 字符串 | 跨登录默认域偏好 |

**解析函数（登录 / 建会话）：**

1. `accessible =` 用户可访问域列表  
2. `preferred =` `user_config` 中该 key  
3. 若 `preferred ∈ accessible` → 用作会话 `businessDomainId`  
4. 否则 → `accessible[0]`（空则沿用现有平台兜底 `resolveDefaultDomainId()`）

## 2. API Contracts

### 2.1 `PUT /v1/auth/me/default-domain`

```json
// request
{ "domainId": 12 }

// response 200
{ "preferredDefaultDomainId": 12 }
```

- 校验：登录用户可访问该域  
- 行为：upsert `user_config`  
- **不**修改当前 session / JWT 域  

### 2.2 `POST /v1/auth/switch-domain`

```json
// request
{ "domainId": 12 }

// response 200 — 对齐登录令牌字段子集
{
  "accessToken": "...",
  "refreshToken": "...",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "businessDomainId": 12,
  "accessibleDomains": [ /* 可选回传 */ ]
}
```

- 校验可访问  
- 更新 login session 的 `businessDomainId`  
- 重发 access/refresh（`UserContext.businessDomainId` 更新）  
- 前端随后用新域调 `permission-snapshot?menuScope=business&domainId=`

### 2.3 Login 行为变更

现有 `POST /v1/auth/login`：将

`defaultBusinessDomainId = accessibleDomains.get(0).id()`

改为 §1 解析函数结果。响应字段名可暂保留 `defaultBusinessDomainId`（兼容），语义变为「本次会话落点域」；另增 `preferredDefaultDomainId`（可为 null）供前端星标与置顶。

## 3. Frontend Architecture

### 3.1 Layout

```
layout-sidebar / layout-mixed-sidebar
├── Logo
├── Menu (height: calc(100% - logo - domainBar - trigger))
├── DomainSwitcherBar   // 新增，仅非 platform 路由树
└── SiderTrigger
```

移动端：`layout-mobile-menu` 底部等价组件。

常量：在 `layout/constants.ts` 增加 `domainSwitcherHeight`（约 48），并修正菜单区 `calc` 高度。

### 3.2 Component: `DomainSwitcherBar`

- 展示：标签「当前业务域」+ 当前域名 + ▾  
- 折叠：图标按钮，Popover 仍可用  
- Popover：标题「切换业务域」；列表排序 = 默认域优先，其余保持原序  
- 行：点主体 → `switchDomain`；点 ★ → `setDefaultDomain`（`stopPropagation`）  
- 平台路由：组件不渲染（或父级条件不挂载）

### 3.3 Auth store

建议字段：

| 字段 | 含义 |
|:---|:---|
| `currentBusinessDomainId` | 当前会话域（驱动 API / 展示） |
| `preferredDefaultDomainId` | 用户偏好（可与 current 不同；null=未设） |
| `accessibleDomains` | 可访问列表 |

兼容迁移：登录仍写入旧字段名时可在 store 内映射到 `currentBusinessDomainId`，避免大爆炸改名；实现时优先一次理清命名。

切换成功后：更新 token → `syncAuthStoreToSharedApi` → 失效 user permission cache → 重拉 snapshot → 更新 access/menus → 路由可达性检查。

## 4. Data Flow

```
设默认: UI ★ → PUT default-domain → preferredDefaultDomainId → 列表重排
切当前: UI 行 → POST switch-domain → tokens + currentId → snapshot → menus → 路由
登录:   login → 读 user_config 解析落点 → JWT/session → store
```

## 5. Compatibility & Rollback

- **兼容：** 无 `user_config` 行时行为与今日一致（第一项）。  
- **回滚：** 下线前端组件 + 停用两接口；登录可临时改回 `get(0)`；表可保留。  
- **安全：** 两写接口均服务端校验域成员；禁止信任客户端「我可访问」。

## 6. Trade-offs

| 选择 | 取舍 |
|:---|:---|
| KV `user_config` vs 账号列 | 可扩展其它偏好；多一次查询（可缓存） |
| 切域重发 token vs 仅请求头带 domainId | 与现有 JWT 内嵌域一致，避免鉴权漂移 |
| 设默认不改当前 | 语义清晰；用户需理解「当前」与「默认」差异（UI 文案标明） |

## 7. Key Files (expected)

| 操作 | 路径 |
|:---|:---|
| 新增 | Flyway `V*__user_config.sql` |
| 新增 | `UserConfig` Repository / Service（auth 或 iam 模块择一，靠近登录账号） |
| 修改 | `AuthService.java`、`AuthController.java`、相关 DTO |
| 新增 | `layout/widgets/domain-switcher/` |
| 修改 | `layout-sidebar`、`layout-mixed-sidebar`、`layout-mobile-menu`、`constants.ts` |
| 修改 | `store/auth.ts`、auth/user API、permission cache |
