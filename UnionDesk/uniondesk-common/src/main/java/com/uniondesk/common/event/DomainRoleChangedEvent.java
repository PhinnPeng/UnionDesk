package com.uniondesk.common.event;

/**
 * 业务域角色变更事件（新增 / 更新 / 更新权限 / 删除）。
 *
 * <p>由 {@code DomainRoleService} 在写库事务内发布，
 * 审计监听器在 {@code AFTER_COMMIT} 异步消费写入审计日志。
 *
 * @param changeType 变更类型：create / update / update_permissions / delete
 * @param previousName 变更前角色名称（create 为 null）
 * @param previousCode 变更前角色编码（create 为 null）
 */
public record DomainRoleChangedEvent(
        long businessDomainId,
        long roleId,
        String roleName,
        String roleCode,
        long operatorUserId,
        String changeType,
        String previousName,
        String previousCode) implements UnionDeskDomainEvent {
}
