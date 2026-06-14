package com.uniondesk.common.event;

import java.util.List;

public record RolePermissionsChangedEvent(
        long businessDomainId,
        int roleId,
        String roleName,
        String roleCode,
        List<Long> beforeMenuIds,
        List<Long> afterMenuIds,
        List<Long> beforeButtonIds,
        List<Long> afterButtonIds,
        long operatorUserId) implements UnionDeskDomainEvent {
}
