# UnionDesk 后端

## 项目定位

`UnionDesk` 是 UnionDesk 的后端服务工程，负责提供客户咨询、工单处理、业务域数据隔离与管理端统计等能力。

## 当前功能

当前已落地的接口能力：

- 健康检查
- 业务域列表
- 工单列表
- 创建工单
- 更新工单状态
- 咨询会话列表
- 咨询消息列表
- 发送咨询消息
- 业务看板统计

## 技术栈

- Java 21
- Spring Boot 3.4
- Spring Security
- MyBatis
- Flyway
- MySQL 8.0

## 目录结构

```text
UnionDesk/
├─ docker-compose.yml
├─ pom.xml
├─ mvnw
├─ mvnw.cmd
└─ src/
   ├─ main/
   │  ├─ java/com/uniondesk/backend/
   │  │  ├─ common/        # 通用响应与异常
   │  │  ├─ config/        # 安全、CORS 等配置
   │  │  ├─ controller/    # REST 接口
   │  │  ├─ mapper/        # MyBatis Mapper
   │  │  ├─ model/         # 请求/响应模型
   │  │  └─ service/       # 业务逻辑
   │  └─ resources/
   │     ├─ application.yml
   │     ├─ logback-spring.xml
   │     └─ db/migration/
   │        ├─ current/     # Flyway SQL (active)
   │        └─ archive/     # Historical migrations
   └─ test/                # 单元测试
```

## 启动方式

### 参考启动数据库（按需）

> 如果本机已经有可用的 MySQL，可以直接复用；没有再参考这一步启动。

```powershell
docker compose up -d
```

### 启动应用

```powershell
.\mvnw.cmd spring-boot:run
```

### 执行测试

```powershell
.\mvnw.cmd test
```

## 主要接口

### 基础接口

- `GET /api/v1/health`
- `GET /api/v1/domains`

### 工单接口

- `GET /api/v1/tickets?businessDomainId=1`
- `POST /api/v1/tickets`
- `PATCH /api/v1/tickets/{ticketNo}/status`

### 咨询接口

- `GET /api/v1/consultations?businessDomainId=1`
- `GET /api/v1/consultations?businessDomainId=1&customerId=1`
- `GET /api/v1/consultations/{sessionNo}/messages`
- `POST /api/v1/consultations/messages`

### 看板接口

- `GET /api/v1/dashboard?businessDomainId=1`

## 日志策略

- 默认：控制台 + 本地滚动文件 `logs/uniondesk-backend.log`
- 后续：接入 ELK 时切换到集中式采集

## 当前说明

- 当前安全配置仍为开发态放行，便于联调；后续需要接入 JWT 与 RBAC
- 数据库迁移由 Flyway 管理，当前活跃脚本位于 `src/main/resources/db/migration/current/`，历史脚本归档到 `archive/`
- 本地开发若需要独立数据库环境，当前固定为 MySQL 8.0，以保证 Flyway 兼容性和启动稳定性；已有可用数据库时可直接复用，不必每次启动容器
