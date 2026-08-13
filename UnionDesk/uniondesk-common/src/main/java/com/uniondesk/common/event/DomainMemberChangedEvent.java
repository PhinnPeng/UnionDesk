package com.uniondesk.common.event;

import java.util.List;

/**
 * 业务域成员变更事件（新增 / 改角色 / 移除）。
 *
 * <p>由 {@code DomainMemberService} 在写库事务内发布，
 * 审计监听器在 {@code AFTER_COMMIT} 异步消费写入审计日志。
 *
 * @param changeType 变更类型：create / update_roles / remove
 * @param beforeRoleCodes 变更前角色码（create 为空）
 * @param afterRoleCodes 变更后角色码（remove 为空）
 */
public record DomainMemberChangedEvent(
        long businessDomainId,
        long memberId,
        long staffAccountId,
        long operatorUserId,
        String changeType,
        List<String> beforeRoleCodes,
        List<String> afterRoleCodes,
        String displayName,
        String loginName) implements UnionDeskDomainEvent {
}
