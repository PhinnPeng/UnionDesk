# 侧栏业务域切换 — 实现计划

## Checklist

1. [ ] **DB** — 新增 Flyway：`user_config` 表（无 FK）；本地 migration 可执行  
   - 验证：表存在，`uk_user_config(user_id, config_key)` 生效  
2. [ ] **UserConfig 读写** — Repository + Service：`get` / `upsert` by `(userId, key)`  
   - 验证：单测或手工插入后可读  
3. [ ] **登录解析** — `AuthService` 用「偏好 ∈ 可访问 ? 偏好 : 第一项」；响应增加 `preferredDefaultDomainId`  
   - 验证：有/无/无效偏好三种落点  
4. [ ] **API: set default** — `PUT /v1/auth/me/default-domain`  
   - 验证：可访问成功；不可访问 4xx；不改当前 session 域  
5. [ ] **API: switch domain** — `POST /v1/auth/switch-domain`（更新 session + 重发 token）  
   - 验证：JWT/session 域 ID 更新；不可访问拒绝  
6. [ ] **前端 API + auth store** — 封装两接口；拆清 current / preferred；切换后 sync + 清 permission cache + 重拉 snapshot  
   - 验证：store 与 shared session 一致  
7. [ ] **DomainSwitcherBar** — Popover 列表、置顶、星标、折叠态  
   - 验证：交互符合 `prd.md` R1–R3  
8. [ ] **挂载布局** — `layout-sidebar` / `mixed` / `mobile`；高度常量；**平台路由不挂载**  
   - 验证：AC1、AC6  
9. [ ] **切域后路由** — 新域菜单不可达则跳转 `/home`  
   - 验证：AC3  
10. [ ] **联调冒烟** — 切域、设默认、重登、无偏好回退、非法 domainId  

## Validation Commands

```bash
# 后端（按仓库惯例调整）
# mvn -pl uniondesk-app -am test
# 或启动后用 HTTP 客户端打 login / default-domain / switch-domain

# 前端
# 在 UnionDeskWeb 下 typecheck / 启动 AdminWeb，业务端侧栏手工验收 AC1–AC6
```

## Risky Files / Rollback Points

| 风险点 | 说明 | 回滚 |
|:---|:---|:---|
| `AuthService` 登录落点 | 影响所有登录会话初始域 | 恢复 `accessibleDomains.get(0)` |
| JWT 重发 | switch-domain 实现错误会导致鉴权失败 | 临时禁用接口 + 前端隐藏入口 |
| 菜单高度 calc | 漏改常量导致侧栏溢出/遮挡 | 还原 `domainSwitcherHeight` 相关改动 |
| auth store 字段重命名 | 易漏引用 | 先映射兼容层再删旧字段 |

## Before `task.py start`

- [ ] 用户已审阅 `prd.md` / `design.md` / `implement.md`  
- [ ] `implement.jsonl` / `check.jsonl` 含真实条目（非 `_example`）  
- [ ] 无阻塞性 Open Questions  

## Out of this checklist

- OpenSpec 主规格同步（可另开）  
- 客户门户域切换  
- 平台侧栏域切换  
