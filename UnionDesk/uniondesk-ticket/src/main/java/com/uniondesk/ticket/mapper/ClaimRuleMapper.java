package com.uniondesk.ticket.mapper;

import com.uniondesk.ticket.entity.ClaimRulePo;
import com.uniondesk.ticket.entity.ClaimRulePolicyPo;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ClaimRuleMapper {

    List<ClaimRulePo> selectByDomainId(
            @Param("domainId") long domainId,
            @Param("limit") int limit,
            @Param("offset") long offset);

    long countByDomainId(@Param("domainId") long domainId);

    ClaimRulePo selectByIdAndDomainId(
            @Param("id") long id,
            @Param("domainId") long domainId);

    void insert(ClaimRulePo po);

    void updateByIdAndDomainId(ClaimRulePo po);

    int deleteByIdAndDomainId(
            @Param("id") long id,
            @Param("domainId") long domainId);

    /**
     * 规则匹配：域 + enabled + 类型/NULL + 优先级/NULL，具体度优先、同度取 id 大（与 SlaRuleMapper.selectPolicy 同构）。
     */
    ClaimRulePolicyPo selectPolicy(
            @Param("domainId") long domainId,
            @Param("ticketTypeId") long ticketTypeId,
            @Param("priorityCode") String priorityCode);

    /**
     * 候选池内受理未完结工单最少者；并列取最近分配久者（MAX(updated_at) 最小），再并列取 id 小。
     */
    Long selectLeastLoadedAssignee(@Param("domainId") long domainId);

    /**
     * 指定人是否在候选池内（域 active 成员 × staff active × 就业 active × 域角色 agent/domain_admin）。
     */
    long countCandidateByStaffId(
            @Param("domainId") long domainId,
            @Param("staffAccountId") long staffAccountId);
}
