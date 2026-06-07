## 1. Flyway 与权限

- [x] 1.1 rename 至 `platform.domain.control.customer.{read,create,update-status}`
- [x] 1.2 catalog `PLATFORM-DOMAIN-CONTROL-CUSTOMER` + 按钮
- [x] 1.3 PermissionCodes + AdminPermissionCatalog + 角色绑定迁移

## 2. 后端

- [x] 2.1 GET 单条 + 全接口改绑 CONTROL_CUSTOMER 码
- [x] 2.2 ControllerTests

## 3. Shared

- [x] 3.1 normalize + fetchDomainCustomer

## 4. 前端

- [x] 4.1 platform-domain-permissions + detail-shared + labels
- [x] 4.2 detail-customers + detail-sider 改绑新码
- [x] 4.3 只读 Modal；禁用二次确认

## 5. 验收

- [x] 5.1 联调 + 文档收口
