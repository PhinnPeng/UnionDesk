# 修复 attribute-slots API 响应格式及数据对齐问题

## Goal

修复两个相关问题：
1. `/ticket-types/{id}/attribute-slots` API 响应格式不符合统一规范（直接返回数组而非 `{total, items}` 结构）
2. 事项类型的属性 slot 未正确关联到 platform 级别的实际属性（而是关联到虚拟系统属性）

## Requirements

### API 响应格式修复
- `attribute-slots` API 必须返回 `{total, items}` 结构，与其他列表 API 保持一致
- 返回类型应为 `AttributeSlotListView`（包含 `long total` 和 `List<AttributeSlotView> items`）

### 数据对齐修复
- Platform 级别的"标题"和"描述"属性应标记为 `is_system = 1`
- Slot 中的系统属性应关联到实际的 platform 属性（ID 1 和 13），而非虚拟系统属性（`system_description`）

## Acceptance Criteria

- [ ] `GET /api/v1/admin/platform/ticket-types/{id}/attribute-slots` 返回 `{total, items}` 结构
- [ ] `ticket-attributes` API 返回的"描述"属性 `is_system` 为 true
- [ ] `attribute-slots` 中的描述属性关联到 ID 13（而非 `system_description`）
- [ ] 所有修改符合后端编码规范（统一使用 `ApiResponse<T>` 包装）

## Implementation Summary

### 已完成的修改

1. **新增 DTO** (`TicketAttributeDtos.java`):
   - 添加 `AttributeSlotListView` record

2. **修改 Controller** (`PlatformTicketConfigController.java`):
   - `listAttributeSlots` 方法返回 `AttributeSlotListView` 而非 `List<AttributeSlotView>`

3. **数据修复迁移** (`V202607120001__fix_system_ticket_attributes.sql`):
   - 将 platform 级别的"标题"和"描述"属性标记为系统属性

## Notes

- 此任务为 Bug 修复，不涉及新功能设计
- 修改已直接执行，需补充文档记录
