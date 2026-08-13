package com.uniondesk.ticket.mapper;

import com.uniondesk.ticket.entity.TicketSatisfactionPo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TicketSatisfactionMapper {

    void insert(TicketSatisfactionPo po);

    TicketSatisfactionPo findByTicketIdAndDomainId(@Param("ticketId") long ticketId, @Param("domainId") long domainId);
}
