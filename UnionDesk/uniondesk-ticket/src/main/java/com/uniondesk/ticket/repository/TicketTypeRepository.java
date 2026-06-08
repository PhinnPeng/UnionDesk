package com.uniondesk.ticket.repository;

import com.uniondesk.ticket.entity.TicketTypePo;
import com.uniondesk.ticket.mapper.TicketTypeMapper;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class TicketTypeRepository {

    private final TicketTypeMapper mapper;

    public TicketTypeRepository(TicketTypeMapper mapper) {
        this.mapper = mapper;
    }

    public List<TicketTypePo> findByDomainId(long domainId) {
        return mapper.findByDomainId(domainId);
    }

    public TicketTypePo findByIdAndDomainId(long id, long domainId) {
        return mapper.findByIdAndDomainId(id, domainId);
    }

    public TicketTypePo findRequiredByIdAndDomainId(long id, long domainId) {
        TicketTypePo po = mapper.findByIdAndDomainId(id, domainId);
        if (po == null) {
            throw new IllegalArgumentException("ticket type not found");
        }
        return po;
    }

    public void save(TicketTypePo po) {
        mapper.insert(po);
    }

    public void update(long id, long domainId, String name, String statusFlowConfig) {
        mapper.update(id, domainId, name, statusFlowConfig);
    }

    public int deleteByIdAndDomainId(long id, long domainId) {
        return mapper.deleteByIdAndDomainId(id, domainId);
    }

    public Long findFirstIdByDomainId(long domainId) {
        return mapper.findFirstIdByDomainId(domainId);
    }
}
