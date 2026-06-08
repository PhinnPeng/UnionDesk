package com.uniondesk.ticket.mapper;

import com.uniondesk.ticket.entity.TicketReplyPo;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TicketReplyMapper {

    void insert(TicketReplyPo po);

    List<TicketReplyPo> findByTicketIdAndDomainId(@Param("ticketId") long ticketId,
                                                   @Param("domainId") long domainId);

    Long findLatestIdByTicketId(@Param("ticketId") long ticketId);
}
