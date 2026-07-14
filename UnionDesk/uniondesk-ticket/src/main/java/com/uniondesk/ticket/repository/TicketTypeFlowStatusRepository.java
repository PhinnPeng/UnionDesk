package com.uniondesk.ticket.repository;

import com.uniondesk.ticket.entity.TicketTypeFlowStatusPo;
import com.uniondesk.ticket.mapper.TicketTypeFlowStatusMapper;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class TicketTypeFlowStatusRepository {

    private final TicketTypeFlowStatusMapper mapper;

    public TicketTypeFlowStatusRepository(TicketTypeFlowStatusMapper mapper) {
        this.mapper = mapper;
    }

    public List<TicketTypeFlowStatusPo> findByDomainIdAndTypeId(long domainId, long ticketTypeId) {
        return mapper.findByDomainIdAndTypeId(domainId, ticketTypeId);
    }

    public void batchInsert(List<TicketTypeFlowStatusPo> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return;
        }
        mapper.batchInsert(statuses);
    }

    public int deleteByDomainIdAndTypeId(long domainId, long ticketTypeId) {
        return mapper.deleteByDomainIdAndTypeId(domainId, ticketTypeId);
    }
}
