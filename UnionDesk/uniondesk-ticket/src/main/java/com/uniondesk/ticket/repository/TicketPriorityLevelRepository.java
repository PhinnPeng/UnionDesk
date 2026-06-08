package com.uniondesk.ticket.repository;

import com.uniondesk.ticket.entity.TicketPriorityLevelPo;
import com.uniondesk.ticket.mapper.TicketPriorityLevelMapper;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class TicketPriorityLevelRepository {

    private final TicketPriorityLevelMapper mapper;

    public TicketPriorityLevelRepository(TicketPriorityLevelMapper mapper) {
        this.mapper = mapper;
    }

    public List<TicketPriorityLevelPo> findByDomainId(long domainId) {
        return mapper.findByDomainId(domainId);
    }

    public TicketPriorityLevelPo findByIdAndDomainId(long id, long domainId) {
        return mapper.findByIdAndDomainId(id, domainId);
    }

    public TicketPriorityLevelPo findRequiredByIdAndDomainId(long id, long domainId) {
        TicketPriorityLevelPo po = mapper.findByIdAndDomainId(id, domainId);
        if (po == null) {
            throw new IllegalArgumentException("priority level not found");
        }
        return po;
    }

    public void save(TicketPriorityLevelPo po) {
        mapper.insert(po);
    }

    public void update(long id, long domainId, String code, String name, int sortOrder, int isDefault) {
        mapper.update(id, domainId, code, name, sortOrder, isDefault);
    }

    public int deleteByIdAndDomainId(long id, long domainId) {
        return mapper.deleteByIdAndDomainId(id, domainId);
    }

    public void clearDefaults(long domainId) {
        mapper.clearDefaults(domainId);
    }

    public void clearDefaultsExcept(long domainId, long keepId) {
        mapper.clearDefaultsExcept(domainId, keepId);
    }
}
