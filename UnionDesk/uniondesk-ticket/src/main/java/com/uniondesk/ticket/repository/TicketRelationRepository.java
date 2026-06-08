package com.uniondesk.ticket.repository;

import com.uniondesk.ticket.entity.TicketRelationPo;
import com.uniondesk.ticket.mapper.TicketRelationMapper;
import org.springframework.stereotype.Repository;

@Repository
public class TicketRelationRepository {

    private final TicketRelationMapper mapper;

    public TicketRelationRepository(TicketRelationMapper mapper) {
        this.mapper = mapper;
    }

    public void save(TicketRelationPo po) {
        mapper.insert(po);
    }
}
