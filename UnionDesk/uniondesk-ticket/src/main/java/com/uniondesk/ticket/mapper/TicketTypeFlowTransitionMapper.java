package com.uniondesk.ticket.mapper;

import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.uniondesk.ticket.entity.TicketTypeFlowTransitionPo;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TicketTypeFlowTransitionMapper extends BaseMapper<TicketTypeFlowTransitionPo> {

    default List<TicketTypeFlowTransitionPo> findByDomainIdAndTypeId(long domainId, long ticketTypeId) {
        return selectListByQuery(QueryWrapper.create()
                .from(TicketTypeFlowTransitionPo.class)
                .where(TicketTypeFlowTransitionPo::getDomainId).eq(domainId)
                .and(TicketTypeFlowTransitionPo::getTicketTypeId).eq(ticketTypeId)
                .orderBy(TicketTypeFlowTransitionPo::getSortOrder, true)
                .orderBy(TicketTypeFlowTransitionPo::getId, true));
    }

    default int batchInsert(List<TicketTypeFlowTransitionPo> transitions) {
        return insertBatchSelective(transitions);
    }

    default int deleteByDomainIdAndTypeId(long domainId, long ticketTypeId) {
        return deleteByQuery(QueryWrapper.create()
                .from(TicketTypeFlowTransitionPo.class)
                .where(TicketTypeFlowTransitionPo::getDomainId).eq(domainId)
                .and(TicketTypeFlowTransitionPo::getTicketTypeId).eq(ticketTypeId));
    }

    default int deleteByDomainIdAndTypeIdAndStateCode(long domainId, long ticketTypeId, String stateCode) {
        return deleteByQuery(QueryWrapper.create()
                .from(TicketTypeFlowTransitionPo.class)
                .where(TicketTypeFlowTransitionPo::getDomainId).eq(domainId)
                .and(TicketTypeFlowTransitionPo::getTicketTypeId).eq(ticketTypeId)
                .and(qw2 -> {
                    qw2.where(TicketTypeFlowTransitionPo::getFromStateCode).eq(stateCode)
                            .or(TicketTypeFlowTransitionPo::getToStateCode).eq(stateCode);
                }));
    }
}
