# Implementation Plan

## Changes Made

### 1. Backend Code Changes

#### File: `uniondesk-ticket/src/main/java/com/uniondesk/ticket/web/TicketAttributeDtos.java`
- **Added**: `AttributeSlotListView` record
  ```java
  public record AttributeSlotListView(
          long total,
          List<AttributeSlotView> items) {
  }
  ```

#### File: `uniondesk-ticket/src/main/java/com/uniondesk/ticket/web/PlatformTicketConfigController.java`
- **Modified**: `listAttributeSlots` method signature
  - Before: `public List<AttributeSlotView> listAttributeSlots(...)`
  - After: `public AttributeSlotListView listAttributeSlots(...)`
  - Implementation wraps list in `AttributeSlotListView` with size as total

### 2. Database Migration

#### File: `uniondesk-app/src/main/resources/db/migration/current/V202607120001__fix_system_ticket_attributes.sql`
- **Purpose**: Fix `is_system` flag for platform-level title and description attributes
- **SQL**:
  ```sql
  UPDATE ticket_attribute 
  SET is_system = 1,
      updated_at = CURRENT_TIMESTAMP(3)
  WHERE scope = 'platform' 
    AND business_domain_id IS NULL
    AND name IN ('标题', '描述')
    AND is_system = 0;
  ```

## Verification Steps

1. Restart application with new migration
2. Verify API response format:
   ```bash
   curl http://localhost:8080/api/v1/admin/platform/ticket-types/8/attribute-slots
   # Should return: {"total": 2, "items": [...]}
   ```
3. Verify data alignment:
   ```bash
   curl http://localhost:8080/api/v1/admin/platform/ticket-attributes
   # "描述" should have is_system: true
   ```

## Build Commands

```bash
mvn clean install -DskipTests -pl uniondesk-ticket,uniondesk-app -am
```
