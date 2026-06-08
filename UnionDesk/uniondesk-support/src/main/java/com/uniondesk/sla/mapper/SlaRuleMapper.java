package com.uniondesk.sla.mapper;

import com.uniondesk.sla.entity.SlaRulePo;
import com.uniondesk.sla.entity.TicketSlaPolicyPo;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SlaRuleMapper {

    List<SlaRulePo> selectByDomainId(
            @Param("domainId") long domainId,
            @Param("limit") int limit,
            @Param("offset") long offset);

    long countByDomainId(@Param("domainId") long domainId);

    SlaRulePo selectByIdAndDomainId(
            @Param("id") long id,
            @Param("domainId") long domainId);

    void insert(SlaRulePo po);

    void updateByIdAndDomainId(SlaRulePo po);

    int deleteByIdAndDomainId(
            @Param("id") long id,
            @Param("domainId") long domainId);

    TicketSlaPolicyPo selectPolicy(
            @Param("domainId") long domainId,
            @Param("ticketTypeId") long ticketTypeId,
            @Param("priorityCode") String priorityCode);

    String selectTicketPriority(@Param("ticketId") long ticketId);
}
