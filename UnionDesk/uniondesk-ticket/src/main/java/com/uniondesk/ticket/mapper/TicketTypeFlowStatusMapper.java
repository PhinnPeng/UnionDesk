package com.uniondesk.ticket.mapper;

import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.uniondesk.ticket.entity.TicketTypeFlowStatusPo;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TicketTypeFlowStatusMapper extends BaseMapper<TicketTypeFlowStatusPo> {

    default List<TicketTypeFlowStatusPo> findByDomainIdAndTypeId(long domainId, long ticketTypeId) {
        return selectListByQuery(QueryWrapper.create()
                .from(TicketTypeFlowStatusPo.class)
                .where(TicketTypeFlowStatusPo::getDomainId).eq(domainId)
                .and(TicketTypeFlowStatusPo::getTicketTypeId).eq(ticketTypeId)
                .orderBy(TicketTypeFlowStatusPo::getSortOrder, true)
                .orderBy(TicketTypeFlowStatusPo::getId, true));
    }

    default int batchInsert(List<TicketTypeFlowStatusPo> statuses) {
        return insertBatchSelective(statuses);
    }

    default int deleteByDomainIdAndTypeId(long domainId, long ticketTypeId) {
        return deleteByQuery(QueryWrapper.create()
                .from(TicketTypeFlowStatusPo.class)
                .where(TicketTypeFlowStatusPo::getDomainId).eq(domainId)
                .and(TicketTypeFlowStatusPo::getTicketTypeId).eq(ticketTypeId));
    }
}
