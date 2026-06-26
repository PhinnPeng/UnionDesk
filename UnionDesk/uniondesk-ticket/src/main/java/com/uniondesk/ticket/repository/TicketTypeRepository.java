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

    public TicketTypePo findByDomainIdAndCode(long domainId, String code) {
        return mapper.findByDomainIdAndCode(domainId, code);
    }

    public TicketTypePo findByDomainIdAndName(long domainId, String name) {
        return mapper.findByDomainIdAndName(domainId, name);
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

    public void updateMetadata(long id, long domainId, String name, String description, String icon, String statusFlowConfig, String status) {
        mapper.updateMetadata(id, domainId, name, description, icon, statusFlowConfig, status);
    }

    public void updateFormSchemaDraft(long id, long domainId, String formSchemaDraft) {
        mapper.updateFormSchemaDraft(id, domainId, formSchemaDraft);
    }

    public void publishFormSchema(long id, long domainId, String formSchema, String formSchemaDraft) {
        mapper.publishFormSchema(id, domainId, formSchema, formSchemaDraft);
    }

    public int deleteByIdAndDomainId(long id, long domainId) {
        return mapper.deleteByIdAndDomainId(id, domainId);
    }

    public int countTicketsByTypeId(long domainId, long typeId) {
        return mapper.countTicketsByTypeId(domainId, typeId);
    }

    public Long findFirstIdByDomainId(long domainId) {
        return mapper.findFirstIdByDomainId(domainId);
    }
}
