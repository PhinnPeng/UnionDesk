package com.uniondesk.ticket.repository;

import com.mybatisflex.core.paginate.Page;
import com.uniondesk.ticket.entity.TicketAttributePo;
import com.uniondesk.ticket.mapper.TicketAttributeMapper;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class TicketAttributeRepository {

    public static final int NO_PAGINATION_THRESHOLD = 100;

    private final TicketAttributeMapper mapper;

    public TicketAttributeRepository(TicketAttributeMapper mapper) {
        this.mapper = mapper;
    }

    public List<TicketAttributePo> findAllPlatform(String keywordLike) {
        return mapper.findByPlatform(keywordLike);
    }

    public List<TicketAttributePo> findAllDomain(long domainId, String keywordLike) {
        return mapper.findByDomain(domainId, keywordLike);
    }

    public Page<TicketAttributePo> findPagePlatform(Page<TicketAttributePo> page, String keywordLike) {
        return mapper.selectPageByPlatform(page, keywordLike);
    }

    public Page<TicketAttributePo> findPageDomain(long domainId, Page<TicketAttributePo> page, String keywordLike) {
        return mapper.selectPageByDomain(domainId, page, keywordLike);
    }

    public TicketAttributePo findById(long id) {
        return mapper.findById(id);
    }

    public TicketAttributePo findRequiredById(long id) {
        TicketAttributePo po = mapper.findById(id);
        if (po == null) {
            throw new IllegalArgumentException("属性不存在");
        }
        return po;
    }

    public TicketAttributePo findPlatformByName(String name) {
        return mapper.findPlatformByName(name);
    }

    public TicketAttributePo findPlatformBySystemKey(String systemKey) {
        return mapper.findPlatformBySystemKey(systemKey);
    }

    public TicketAttributePo findDomainByName(long domainId, String name) {
        return mapper.findDomainByName(domainId, name);
    }

    public TicketAttributePo findDomainBySystemKey(long domainId, String systemKey) {
        return mapper.findDomainBySystemKey(domainId, systemKey);
    }

    public TicketAttributePo findDomainBySourceAttributeId(long domainId, long sourceAttributeId) {
        return mapper.findDomainBySourceAttributeId(domainId, sourceAttributeId);
    }

    public int nextSortOrderPlatform() {
        Integer max = mapper.findMaxSortOrderPlatform();
        return max == null ? 0 : max + 1;
    }

    public int nextSortOrderDomain(long domainId) {
        Integer max = mapper.findMaxSortOrderDomain(domainId);
        return max == null ? 0 : max + 1;
    }

    public void insert(TicketAttributePo po) {
        mapper.insert(po);
    }

    public void update(TicketAttributePo po) {
        mapper.update(po);
    }

    public int deletePlatform(long id) {
        return mapper.deleteByIdPlatform(id);
    }

    public int deleteDomain(long id, long domainId) {
        return mapper.deleteByIdDomain(id, domainId);
    }

    public void updateSortOrder(long id, int sortOrder, Long updatedBy) {
        mapper.updateSortOrder(id, sortOrder, updatedBy);
    }
}
