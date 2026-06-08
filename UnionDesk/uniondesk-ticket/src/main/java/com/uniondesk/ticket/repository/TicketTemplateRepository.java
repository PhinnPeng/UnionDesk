package com.uniondesk.ticket.repository;

import com.uniondesk.ticket.entity.TicketTemplatePo;
import com.uniondesk.ticket.mapper.TicketTemplateMapper;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class TicketTemplateRepository {

    private final TicketTemplateMapper mapper;

    public TicketTemplateRepository(TicketTemplateMapper mapper) {
        this.mapper = mapper;
    }

    public List<TicketTemplatePo> findByDomainId(long domainId) {
        return mapper.findByDomainId(domainId);
    }

    public TicketTemplatePo findByIdAndDomainId(long id, long domainId) {
        return mapper.findByIdAndDomainId(id, domainId);
    }

    public TicketTemplatePo findRequiredByIdAndDomainId(long id, long domainId) {
        TicketTemplatePo po = mapper.findByIdAndDomainId(id, domainId);
        if (po == null) {
            throw new IllegalArgumentException("ticket template not found");
        }
        return po;
    }

    public void save(TicketTemplatePo po) {
        mapper.insert(po);
    }

    public void update(long id, long domainId, Long ticketTypeId, String scope, String name, String contentJson, int sortOrder) {
        mapper.update(id, domainId, ticketTypeId, scope, name, contentJson, sortOrder);
    }

    public int deleteByIdAndDomainId(long id, long domainId) {
        return mapper.deleteByIdAndDomainId(id, domainId);
    }
}
