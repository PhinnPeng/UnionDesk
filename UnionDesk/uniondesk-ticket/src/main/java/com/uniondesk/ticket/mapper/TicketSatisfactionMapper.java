package com.uniondesk.ticket.mapper;

import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.uniondesk.ticket.entity.TicketSatisfactionPo;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TicketSatisfactionMapper extends BaseMapper<TicketSatisfactionPo> {

    default TicketSatisfactionPo findByTicketIdAndDomainId(long ticketId, long domainId) {
        return selectOneByQuery(QueryWrapper.create()
                .from(TicketSatisfactionPo.class)
                .where(TicketSatisfactionPo::getTicketId).eq(ticketId)
                .and(TicketSatisfactionPo::getBusinessDomainId).eq(domainId));
    }
}
