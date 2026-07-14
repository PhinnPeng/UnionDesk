package com.uniondesk.ticket.mapper;

import com.uniondesk.ticket.entity.TicketTypeFlowTransitionPo;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TicketTypeFlowTransitionMapper {

    List<TicketTypeFlowTransitionPo> findByDomainIdAndTypeId(
            @Param("domainId") long domainId,
            @Param("ticketTypeId") long ticketTypeId);

    int batchInsert(@Param("list") List<TicketTypeFlowTransitionPo> transitions);

    int deleteByDomainIdAndTypeId(
            @Param("domainId") long domainId,
            @Param("ticketTypeId") long ticketTypeId);

    int deleteByDomainIdAndTypeIdAndStateCode(
            @Param("domainId") long domainId,
            @Param("ticketTypeId") long ticketTypeId,
            @Param("stateCode") String stateCode);
}
