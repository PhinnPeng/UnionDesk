package com.uniondesk.ticket.mapper;

import com.uniondesk.ticket.entity.TicketTypeFlowStatusPo;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TicketTypeFlowStatusMapper {

    List<TicketTypeFlowStatusPo> findByDomainIdAndTypeId(
            @Param("domainId") long domainId,
            @Param("ticketTypeId") long ticketTypeId);

    int batchInsert(@Param("list") List<TicketTypeFlowStatusPo> statuses);

    int deleteByDomainIdAndTypeId(
            @Param("domainId") long domainId,
            @Param("ticketTypeId") long ticketTypeId);
}
