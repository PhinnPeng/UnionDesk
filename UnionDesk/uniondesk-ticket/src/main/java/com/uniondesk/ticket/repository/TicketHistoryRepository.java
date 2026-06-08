package com.uniondesk.ticket.repository;

import com.uniondesk.ticket.entity.TicketHistoryPo;
import com.uniondesk.ticket.mapper.TicketHistoryMapper;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class TicketHistoryRepository {

    private final TicketHistoryMapper mapper;

    public TicketHistoryRepository(TicketHistoryMapper mapper) {
        this.mapper = mapper;
    }

    public void save(TicketHistoryPo po) {
        mapper.insert(po);
    }

    public List<TicketHistoryPo> findByTicketIdAndDomainId(long ticketId, long domainId) {
        return mapper.findByTicketIdAndDomainId(ticketId, domainId);
    }
}
