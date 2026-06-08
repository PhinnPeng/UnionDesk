package com.uniondesk.ticket.repository;

import com.uniondesk.ticket.entity.TicketDetailPo;
import com.uniondesk.ticket.entity.TicketPo;
import com.uniondesk.ticket.mapper.TicketMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class TicketRepository {

    private final TicketMapper mapper;

    public TicketRepository(TicketMapper mapper) {
        this.mapper = mapper;
    }

    public void save(TicketPo po) {
        mapper.insert(po);
    }

    public TicketDetailPo findByIdAndDomainId(long ticketId, long domainId) {
        return mapper.findByIdAndDomainId(ticketId, domainId);
    }

    public TicketDetailPo findRequiredByIdAndDomainId(long ticketId, long domainId) {
        TicketDetailPo po = mapper.findByIdAndDomainId(ticketId, domainId);
        if (po == null) {
            throw new IllegalArgumentException("ticket not found");
        }
        return po;
    }

    public List<TicketDetailPo> listTickets(long domainId, Long customerId, String status, int limit) {
        return mapper.listTickets(domainId, customerId, status, limit);
    }

    public long findIdByTicketNo(String ticketNo) {
        Long id = mapper.findIdByTicketNo(ticketNo);
        if (id == null) {
            throw new IllegalArgumentException("ticket not found");
        }
        return id;
    }

    public int updateStatus(long ticketId, String newStatus, long version, LocalDateTime now) {
        return mapper.updateStatus(ticketId, newStatus, version, now);
    }

    public int updateClaim(long ticketId, long domainId, long assignee, long version, LocalDateTime now) {
        return mapper.updateClaim(ticketId, domainId, assignee, version, now);
    }

    public int updateAssign(long ticketId, long domainId, long assignee, long version, LocalDateTime now) {
        return mapper.updateAssign(ticketId, domainId, assignee, version, now);
    }

    public int updateOnReply(long ticketId, long domainId, String senderType, long version, LocalDateTime now) {
        return mapper.updateOnReply(ticketId, domainId, senderType, version, now);
    }

    public int updateWithdraw(long ticketId, long domainId, long version, String reason) {
        return mapper.updateWithdraw(ticketId, domainId, version, reason);
    }

    public int updateMerge(long ticketId, long domainId, long version) {
        return mapper.updateMerge(ticketId, domainId, version);
    }

    public String findDomainCodeById(long domainId) {
        return mapper.findDomainCodeById(domainId);
    }

    public long findNextTicketSequence(long domainId, String prefix) {
        Long seq = mapper.findNextTicketSequence(domainId, prefix);
        return seq == null ? 1L : seq;
    }

    public String findDefaultPriorityCode(long domainId) {
        return mapper.findDefaultPriorityCode(domainId);
    }
}
