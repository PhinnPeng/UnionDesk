package com.uniondesk.ticket.mapper;

import com.uniondesk.ticket.entity.TicketHistoryPo;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TicketHistoryMapper {

    void insert(TicketHistoryPo po);

    List<TicketHistoryPo> findByTicketIdAndDomainId(@Param("ticketId") long ticketId,
                                                     @Param("domainId") long domainId);
}
