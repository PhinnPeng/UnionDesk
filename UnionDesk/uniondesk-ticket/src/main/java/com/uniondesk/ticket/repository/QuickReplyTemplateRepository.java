package com.uniondesk.ticket.repository;

import com.uniondesk.ticket.entity.QuickReplyTemplatePo;
import com.uniondesk.ticket.mapper.QuickReplyTemplateMapper;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class QuickReplyTemplateRepository {

    private final QuickReplyTemplateMapper mapper;

    public QuickReplyTemplateRepository(QuickReplyTemplateMapper mapper) {
        this.mapper = mapper;
    }

    public List<QuickReplyTemplatePo> findByDomainId(long domainId) {
        return mapper.findByDomainId(domainId);
    }

    public QuickReplyTemplatePo findByIdAndDomainId(long id, long domainId) {
        return mapper.findByIdAndDomainId(id, domainId);
    }

    public QuickReplyTemplatePo findRequiredByIdAndDomainId(long id, long domainId) {
        QuickReplyTemplatePo po = mapper.findByIdAndDomainId(id, domainId);
        if (po == null) {
            throw new IllegalArgumentException("quick reply not found");
        }
        return po;
    }

    public QuickReplyTemplatePo findActiveRequiredByIdAndDomainId(long id, long domainId) {
        QuickReplyTemplatePo po = mapper.findActiveByIdAndDomainId(id, domainId);
        if (po == null) {
            throw new IllegalArgumentException("quick reply template not found");
        }
        return po;
    }

    public void save(QuickReplyTemplatePo po) {
        mapper.insert(po);
    }

    public void update(long id, long domainId, String scopeType, String title, String content, int sortOrder) {
        mapper.update(id, domainId, scopeType, title, content, sortOrder);
    }

    public int deleteByIdAndDomainId(long id, long domainId) {
        return mapper.deleteByIdAndDomainId(id, domainId);
    }
}
