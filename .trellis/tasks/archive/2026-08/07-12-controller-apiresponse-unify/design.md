# Design: Controller层统一返回ApiResponse优化

## 1. 现状分析

### 1.1 全量 Controller 排查结果（27 个 Controller）

| Controller | 模块 | 返回类型问题 | 方法数 |
|---|---|---|---|
| `HealthController` | common | ❌ `Map<String,String>` / `Map<String,Object>` | 2 |
| `AuthController` | app | ❌ 裸 DTO / `List<T>` / `void` | 16 |
| `IamController` | iam | ❌ 裸 DTO / `List<T>`(8) / `Object`(1) / `void`(3) | 22 |
| `OrganizationController` | iam | ❌ 裸 DTO / `List<T>`(1) / `void`(1) | 4 |
| `StaffController` | iam | ❌ 裸 DTO / `PageResult<T>` | 9 |
| `DomainController` | domain | ❌ 裸 DTO / `PageResult<T>` / `void`(1) | 6 |
| `DomainMemberController` | domain | ❌ 裸 DTO / `PageResult<T>` / `void`(1) | 6 |
| `DomainRoleController` | domain | ❌ 裸 DTO / `List<T>`(2) / `void`(1) | 7 |
| `DomainConfigController` | domain | ❌ 裸 DTO | 2 |
| `DomainCustomerController` | domain | ❌ 裸 DTO / `PageResult<T>` | 5 |
| `PlatformDomainRoleController` | domain | ❌ 裸 DTO / `List<T>`(1) | 2 |
| `InvitationCodeController` | domain | ❌ 裸 DTO / `PageResult<T>` / `void`(1) | 3 |
| `TicketController` | ticket | ❌ 裸 DTO / `List<T>`(3) | 12 |
| `TicketConfigController` | ticket | ❌ 裸 DTO / `List<T>`(5) / `void`(8) | 22 |
| `PlatformTicketConfigController` | ticket | ❌ 裸 DTO / `void`(5) | 18 |
| `PlatformTicketStatusController` | ticket | ❌ 裸 DTO / `void`(1) | 4 |
| `SlaController` | support | ❌ 裸 DTO / `PageResult<T>` / `void`(1) | 7 |
| `NotificationTemplateController` | support | ❌ 裸 DTO / `PageResult<T>` | 2 |
| `InboxController` | support | ❌ `List<T>`(1) / `Map`(3) | 4 |
| `SystemConfigController` | support | ❌ 裸 DTO | 2 |
| `BlockedWordController` | support | ❌ 裸 DTO / `PageResult<T>` / `void`(1) | 4 |
| `PlatformBlockedWordController` | support | ❌ 裸 DTO / `PageResult<T>` / `void`(1) | 4 |
| `AuditLogController` | support | ❌ 裸 DTO / `PageResult<T>` | 2 |
| `LoginLogController` | support | ❌ 裸 DTO / `PageResult<T>` | 2 |
| `AttachmentController` | support | ❌ 裸 DTO / `void`(1) | 4 |
| `DashboardController` | support(demo) | ✅ 已用 `ApiResponse<T>` | 1 |
| `ConsultationController` | support(demo) | ✅ 已用 `ApiResponse<T>` (但内部 `List`) | 5 |

### 1.2 问题分类

| 问题类型 | 影响方法数 | 说明 |
|---|---|---|
| 裸 DTO 返回（无 ApiResponse 包裹） | ~90+ | 绝大多数方法 |
| `List<T>` 直接返回 | ~22 | 缺少 total 字段 |
| `Map<K,V>` 直接返回 | 5 | 无类型安全 |
| `Object` 返回 | 1 | 无类型安全 |
| `void` 返回 | ~25 | 需跳过包装（204 No Content） |

### 1.3 关键发现：无自动包装机制

`ApiExceptionHandler`（`@RestControllerAdvice`）仅处理异常，**不存在 `ResponseBodyAdvice`** 对成功响应做自动包装。规范中"Handler 自动包装为 `ApiResponse.ok(data)`"的描述与实现不符。

## 2. 方案选择

### 方案 A: ResponseBodyAdvice 自动包装（推荐）

**核心思路**：新增 `ApiResponseWrapper` 实现 `ResponseBodyAdvice`，拦截所有 Controller 返回值：
- 非 `ApiResponse` 类型 → 自动包装为 `ApiResponse.ok(data)`
- `ApiResponse` 类型 → 原样返回（不二次包装）
- `void` 返回 / HTTP 204 → 跳过

**优点**：
- 一处改动，全局生效，Controller 代码无需逐个修改
- 符合规范描述的"自动包装"行为
- 新增 Controller 自然遵守，无需记忆包裹

**缺点**：
- 行为隐式（"魔法"），需文档说明
- 仍需修复 `List<T>` / `Map<K,V>` / `Object` 的返回类型

### 方案 B: 每个 Controller 显式 `ApiResponse<T>`

**优点**：显式、自文档化
**缺点**：90+ 方法签名修改，改动量大，且与规范描述的"自动包装"方向相反

### 决策：采用方案 A

理由：
1. 规范已描述自动包装机制，方案 A 使规范与实现一致
2. 改动量最小，风险可控
3. `List<T>` / `Map<K,V>` / `Object` 问题单独修复

## 3. 技术设计

### 3.1 新增 `ApiResponseWrapper`（ResponseBodyAdvice）

**位置**：`uniondesk-app/src/main/java/com/uniondesk/common/web/ApiResponseWrapper.java`

```java
@RestControllerAdvice
public class ApiResponseWrapper implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class converterType) {
        // 跳过 void 返回
        if (Void.TYPE.equals(returnType.getParameterType())) {
            return false;
        }
        // 跳过已经是 ApiResponse 的返回
        if (ApiResponse.class.isAssignableFrom(returnType.getParameterType())) {
            return false;
        }
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, ...) {
        if (body == null) {
            return ApiResponse.ok(null);
        }
        if (body instanceof ApiResponse<?> apiResponse) {
            return apiResponse;
        }
        return ApiResponse.ok(body);
    }
}
```

**排除项**：
- `HealthController`：K8s 探针端点，保持原始 `Map` 返回。通过 `@RestController` 包路径过滤或 `supports()` 中判断包路径排除。
- `ApiResponse` 子类型：跳过，避免二次包装。
- `void` 返回：跳过。

### 3.2 修复 `List<T>` 返回 → `ListView` / `PageResult<T>`

需修改的 Controller 方法及方案：

| Controller | 方法 | 当前返回 | 修改为 |
|---|---|---|---|
| `TicketController` | `listCustomerTickets` | `List<TicketRow>` | 新增 `TicketListView` record |
| `TicketController` | `listAdminTickets` | `List<TicketRow>` | 同上 |
| `TicketController` | `listTicketHistory` | `List<TicketHistoryRow>` | 新增 `TicketHistoryListView` record |
| `TicketConfigController` | `listTicketTypes` | `List<TicketTypeView>` | 新增 `TicketTypeListView` record |
| `TicketConfigController` | `listAttributeSlots` | `List<AttributeSlotView>` | 已有 `AttributeSlotListView`，改用之 |
| `TicketConfigController` | `listTicketTemplates` | `List<TicketTemplateView>` | 新增 `TicketTemplateListView` record |
| `TicketConfigController` | `listQuickReplies` | `List<QuickReplyView>` | 新增 `QuickReplyListView` record |
| `TicketConfigController` | `listPriorityLevels` | `List<PriorityLevelView>` | 新增 `PriorityLevelListView` record |
| `IamController` | `listResources` | `List<ResourceView>` | 新增 `ResourceListView` record |
| `IamController` | `listRoleResources` | `List<ResourceView>` | 同上 |
| `IamController` | `replaceRoleResources` | `List<ResourceView>` | 同上 |
| `IamController` | `listAdminPermissionCodes` | `List<AdminPermissionCodeView>` | 新增 `AdminPermissionCodeListView` record |
| `IamController` | `listRoles` | `List<RoleView>` | 新增 `RoleListView` record |
| `IamController` | `listUsers` | `List<UserAccountView>` | 新增 `UserAccountListView` record |
| `IamController` | `listOffboardPool` | `List<UserAccountView>` | 同上 |
| `IamController` | `currentMenuResources` | `List<ResourceView>` | 新增 `ResourceListView` record |
| `OrganizationController` | `listOrganizations` | `List<OrganizationUnitView>` | 新增 `OrganizationListView` record |
| `InboxController` | `listInbox` | `List<InboxMessageView>` | 新增 `InboxMessageListView` record |
| `PlatformDomainRoleController` | `listPlatformDomainRoles` | `List<DomainRoleView>` | 新增 `DomainRoleListView` record |
| `DomainRoleController` | `listRoles` | `List<DomainRoleView>` | 同上 |
| `DomainRoleController` | `listPermissionItems` | `List<PermissionItemView>` | 新增 `PermissionItemListView` record |
| `AuthController` | `listOnlineSessions` | `List<OnlineSessionView>` | 新增 `OnlineSessionListView` record |

### 3.3 修复 `Map<K,V>` 返回 → DTO record

| Controller | 方法 | 当前返回 | 修改为 |
|---|---|---|---|
| `InboxController` | `unreadCount` | `Map<String, Long>` | 新增 `UnreadCountView(long unreadCount)` record |
| `InboxController` | `markRead` | `Map<String, Object>` | 新增 `MarkReadResultView(boolean ok, int updated)` record |
| `InboxController` | `markReadBatch` | `Map<String, Object>` | 同上 |
| `HealthController` | `health` | `Map<String, String>` | 排除在 ApiResponseWrapper 外（探针端点） |
| `HealthController` | `readiness` | `Map<String, Object>` | 排除在 ApiResponseWrapper 外（探针端点） |

### 3.4 修复 `Object` 返回 → 具体 DTO

| Controller | 方法 | 当前返回 | 修改为 |
|---|---|---|---|
| `IamController` | `listMenusTree` | `Object`（List 或 Map） | 拆为两个方法或统一返回 `MenuTreeListView` |

**方案**：`listMenusTree` 当前逻辑：
- 有 `scope` 参数 → 返回 `List<MenuTreeNodeView>`
- 无 `scope` 参数 → 返回 `Map<String, List<MenuTreeNodeView>>`（含 platform/business 两个 key）

统一为：始终返回 `MenuTreeResultView` record：
```java
public record MenuTreeResultView(
    List<MenuTreeNodeView> platform,
    List<MenuTreeNodeView> business
) {}
```
有 `scope` 时只填充对应字段，无 `scope` 时填充两个字段。

### 3.5 修正规范文档

`.trellis/spec/backend/error-handling.md` 中：
- 修正"Handler 自动包装"描述，补充 `ApiResponseWrapper` 的说明
- 保持"Controller 可直接返回业务对象"的指导不变

## 4. 影响范围

### 4.1 新增文件

| 文件 | 说明 |
|---|---|
| `uniondesk-app/.../common/web/ApiResponseWrapper.java` | ResponseBodyAdvice 自动包装 |
| 各模块 `*Dtos.java` 中新增 ListView record | 列表包装 DTO |

### 4.2 修改文件

| 文件 | 改动 |
|---|---|
| 22 个 Controller 文件 | 修复 `List<T>` / `Map<K,V>` / `Object` 返回类型 |
| `InboxController.java` | Map → DTO record |
| `IamController.java` | Object → DTO record |
| `.trellis/spec/backend/error-handling.md` | 修正自动包装描述 |

### 4.3 不修改

- `ApiExceptionHandler.java`（异常处理不变）
- `ApiResponse.java`（结构不变）
- `PageResult.java`（已有 Controller 在用，保留）
- 所有 Service / Repository / Mapper 层（业务逻辑不变）
- 所有测试文件（测试断言以业务对象为准，自动包装对 MockMvc standaloneSetup 透明）

## 5. 风险与缓解

| 风险 | 缓解 |
|---|---|
| `ResponseBodyAdvice` 对 `String` 返回值序列化冲突 | 项目无返回 `String` 的 Controller 方法 |
| 前端已有适配逻辑可能受影响 | 前端已按 `ApiResponse` 解析（DashboardController 验证），此次为正向修复 |
| `HealthController` 被 K8s 探针调用 | 显式排除在包装范围外 |
| 测试中 MockMvc 断言需更新 | `standaloneSetup` 不走 `ResponseBodyAdvice`，集成测试需更新断言 |

## 6. 回滚方案

- `ApiResponseWrapper` 为新增独立文件，删除即回滚到无自动包装状态。
- Controller 中 `List<T>` / `Map<K,V>` → DTO 的修改为纯签名变更，git revert 即可。
