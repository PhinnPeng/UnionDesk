package com.uniondesk.domain.core;

/**
 * 建域时可选套用团队模板。由 ticket 模块实现，避免 domain → ticket 循环依赖。
 */
public interface DomainTeamTemplateApplier {

    void applyOnDomainCreate(long domainId, long teamTemplateId, Long operatorId);
}
