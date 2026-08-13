package com.uniondesk.ticket.repository;

import com.uniondesk.ticket.entity.TicketSatisfactionPo;
import com.uniondesk.ticket.mapper.TicketSatisfactionMapper;
import org.springframework.stereotype.Repository;

@Repository
public class TicketSatisfactionRepository {

    private final TicketSatisfactionMapper mapper;

    public TicketSatisfactionRepository(TicketSatisfactionMapper mapper) {
        this.mapper = mapper;
    }

    public void save(TicketSatisfactionPo po) {
        mapper.insert(po);
    }

    public TicketSatisfactionPo findByTicketIdAndDomainId(long ticketId, long domainId) {
        return mapper.findByTicketIdAndDomainId(ticketId, domainId);
    }
}
