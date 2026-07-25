package com.uniondesk.ticket.mapper;

import com.uniondesk.ticket.entity.TicketWatcherPo;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TicketWatcherMapper {

    List<Long> findStaffIdsByTicketId(@Param("ticketId") long ticketId);

    void deleteByTicketId(@Param("ticketId") long ticketId);

    void insert(TicketWatcherPo po);
}
