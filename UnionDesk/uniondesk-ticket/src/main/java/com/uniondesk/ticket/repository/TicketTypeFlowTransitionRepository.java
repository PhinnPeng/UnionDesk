package com.uniondesk.ticket.repository;

import com.uniondesk.ticket.entity.TicketTypeFlowTransitionPo;
import com.uniondesk.ticket.mapper.TicketTypeFlowTransitionMapper;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class TicketTypeFlowTransitionRepository {

    private final TicketTypeFlowTransitionMapper mapper;

    public TicketTypeFlowTransitionRepository(TicketTypeFlowTransitionMapper mapper) {
        this.mapper = mapper;
    }

    public List<TicketTypeFlowTransitionPo> findByDomainIdAndTypeId(long domainId, long ticketTypeId) {
        return mapper.findByDomainIdAndTypeId(domainId, ticketTypeId);
    }

    public void batchInsert(List<TicketTypeFlowTransitionPo> transitions) {
        if (transitions == null || transitions.isEmpty()) {
            return;
        }
        mapper.batchInsert(transitions);
    }

    public int deleteByDomainIdAndTypeId(long domainId, long ticketTypeId) {
        return mapper.deleteByDomainIdAndTypeId(domainId, ticketTypeId);
    }

    public int deleteByDomainIdAndTypeIdAndStateCode(long domainId, long ticketTypeId, String stateCode) {
        return mapper.deleteByDomainIdAndTypeIdAndStateCode(domainId, ticketTypeId, stateCode);
    }
}
