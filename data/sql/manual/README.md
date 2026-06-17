# 手工 SQL（非 Flyway）

本目录脚本**不**纳入 `db/migration/current/`，需按环境手工执行（联调/本地）。

| 脚本 | 说明 |
|:---|:---|
| [`business-home-menu.sql`](./business-home-menu.sql) | 业务域控制台首页菜单 `/home`（catalog + menu + 必需按钮 + 角色绑定） |
| [`admin-platform-home-data-fix.sql`](./admin-platform-home-data-fix.sql) | Admin 登录默认 `/platform/home`：清理误绑域角色 + global 角色上的 `domain.member.*` |

执行示例（需 MySQL 客户端或联调库访问）：

```bash
mysql -h 127.0.0.1 -P 30306 -u uniondesk_app -p uniondesk < data/sql/manual/business-home-menu.sql
```
