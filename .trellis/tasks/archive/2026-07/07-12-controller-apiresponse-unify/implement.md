# Implement: Controller层统一返回ApiResponse优化

## 执行步骤

### Step 1: 新增 `ApiResponseWrapper`

- [ ] 创建 `uniondesk-app/src/main/java/com/uniondesk/common/web/ApiResponseWrapper.java`
- [ ] 实现 `ResponseBodyAdvice<Object>`，自动包装非 `ApiResponse` / 非 `void` 返回值
- [ ] 排除 `HealthController`（包路径过滤或注解标记）
- [ ] 验证：编译通过

### Step 2: 修复 `List<T>` 返回 → `ListView` record

按模块分批：

- [ ] **ticket 模块**：在 `TicketConfigDtos.java` / `TicketAttributeDtos.java` 中新增 ListView record；修改 `TicketController` (3)、`TicketConfigController` (5) 返回类型
- [ ] **iam 模块**：在 `IamDtos.java` 中新增 ListView record；修改 `IamController` (8)、`OrganizationController` (1) 返回类型
- [ ] **domain 模块**：在 `DomainRoleDtos.java` 中新增 ListView record；修改 `DomainRoleController` (2)、`PlatformDomainRoleController` (1) 返回类型
- [ ] **support 模块**：在 `InboxController` 对应 Dtos 中新增 ListView record；修改 `InboxController` (1) 返回类型
- [ ] **app 模块**：在 `AuthDtos.java` 中新增 ListView record；修改 `AuthController` (1) 返回类型
- [ ] 验证：`mvnw clean compile` 通过

### Step 3: 修复 `Map<K,V>` 返回 → DTO record

- [ ] `InboxController.unreadCount` → `UnreadCountView(long unreadCount)`
- [ ] `InboxController.markRead` → `MarkReadResultView(boolean ok, int updated)`
- [ ] `InboxController.markReadBatch` → `MarkReadResultView(boolean ok, int updated)`
- [ ] 验证：编译通过

### Step 4: 修复 `Object` 返回 → 具体 DTO

- [ ] `IamController.listMenusTree` → 拆为 `MenuTreeResultView(platform, business)` record
- [ ] 验证：编译通过

### Step 5: 全量测试

- [ ] `mvnw test` 全量通过
- [ ] 修复因返回类型变更导致的测试断言失败（如有）

### Step 6: 修正规范文档

- [ ] 更新 `.trellis/spec/backend/error-handling.md`，补充 `ApiResponseWrapper` 说明

## 验证命令

```powershell
cd UnionDesk
.\mvnw.cmd clean compile          # 编译检查
.\mvnw.cmd test                   # 全量测试
```

## 文件清单

### 新增
1. `uniondesk-app/src/main/java/com/uniondesk/common/web/ApiResponseWrapper.java`

### 修改（Controller）
2. `uniondesk-ticket/.../web/TicketController.java`
3. `uniondesk-ticket/.../web/TicketConfigController.java`
4. `uniondesk-iam/.../web/IamController.java`
5. `uniondesk-iam/.../web/OrganizationController.java`
6. `uniondesk-support/.../notification/web/InboxController.java`
7. `uniondesk-domain/.../web/DomainRoleController.java`
8. `uniondesk-domain/.../web/PlatformDomainRoleController.java`
9. `uniondesk-app/.../auth/web/AuthController.java`

### 修改（DTO 新增 record）
10. `uniondesk-ticket/.../web/TicketConfigDtos.java`
11. `uniondesk-ticket/.../web/TicketAttributeDtos.java`
12. `uniondesk-iam/.../web/IamDtos.java`
13. `uniondesk-iam/.../web/OrganizationDtos.java`
14. `uniondesk-domain/.../web/DomainRoleDtos.java`
15. `uniondesk-support/.../notification/web/` 下 Dtos（或 InboxController 内定义）
16. `uniondesk-app/.../auth/web/AuthDtos.java`

### 修改（规范）
17. `.trellis/spec/backend/error-handling.md`

## 回滚点

- 每个 Step 完成后 `git stash` 可回滚单步
- `ApiResponseWrapper.java` 删除即恢复无自动包装状态
