package com.uniondesk.ticket.mapper;

import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.uniondesk.ticket.entity.TicketWatcherPo;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TicketWatcherMapper extends BaseMapper<TicketWatcherPo> {

    default List<Long> findStaffIdsByTicketId(long ticketId) {
        return selectListByQuery(QueryWrapper.create()
                .from(TicketWatcherPo.class)
                .where(TicketWatcherPo::getTicketId).eq(ticketId)
                .orderBy(TicketWatcherPo::getId, true)).stream()
                .map(TicketWatcherPo::getStaffAccountId)
                .toList();
    }

    default int deleteByTicketId(long ticketId) {
        return deleteByQuery(QueryWrapper.create()
                .from(TicketWatcherPo.class)
                .where(TicketWatcherPo::getTicketId).eq(ticketId));
    }
}
