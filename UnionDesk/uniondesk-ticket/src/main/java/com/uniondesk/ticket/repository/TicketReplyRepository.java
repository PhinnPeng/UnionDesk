package com.uniondesk.ticket.repository;

import com.uniondesk.ticket.entity.TicketReplyPo;
import com.uniondesk.ticket.mapper.TicketReplyMapper;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class TicketReplyRepository {

    private final TicketReplyMapper mapper;

    public TicketReplyRepository(TicketReplyMapper mapper) {
        this.mapper = mapper;
    }

    public void save(TicketReplyPo po) {
        mapper.insert(po);
    }

    public List<TicketReplyPo> findByTicketIdAndDomainId(long ticketId, long domainId) {
        return mapper.findByTicketIdAndDomainId(ticketId, domainId);
    }

    public long findLatestIdByTicketId(long ticketId) {
        Long id = mapper.findLatestIdByTicketId(ticketId);
        if (id == null) {
            throw new IllegalStateException("reply not found");
        }
        return id;
    }
}
