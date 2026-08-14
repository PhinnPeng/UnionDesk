package com.uniondesk.ticket.repository;

import com.mybatisflex.core.paginate.Page;
import com.uniondesk.ticket.entity.TicketTypePo;
import com.uniondesk.ticket.mapper.TicketTypeMapper;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class TicketTypeRepository {

    public static final int NO_PAGINATION_THRESHOLD = 100;

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

    public TicketTypePo findByDomainIdAndSourceGlobalTypeId(long domainId, long sourceGlobalTypeId) {
        return mapper.findByDomainIdAndSourceGlobalTypeId(domainId, sourceGlobalTypeId);
    }

    public int nextSortOrderDomain(long domainId) {
        Integer max = mapper.findMaxSortOrderDomain(domainId);
        return max == null ? 0 : max + 1;
    }

    public TicketTypePo findRequiredByIdAndDomainId(long id, long domainId) {
        TicketTypePo po = mapper.findByIdAndDomainId(id, domainId);
        if (po == null) {
            throw new IllegalArgumentException("事项类型不存在");
        }
        return po;
    }

    public List<TicketTypePo> findAllPlatform(String keywordLike) {
        return mapper.findByPlatform(keywordLike);
    }

    public Page<TicketTypePo> findPagePlatform(Page<TicketTypePo> page, String keywordLike) {
        return mapper.selectPageByPlatform(page, keywordLike);
    }

    public TicketTypePo findPlatformById(long id) {
        return mapper.findPlatformById(id);
    }

    public TicketTypePo findRequiredPlatformById(long id) {
        TicketTypePo po = mapper.findPlatformById(id);
        if (po == null) {
            throw new IllegalArgumentException("事项类型不存在");
        }
        return po;
    }

    public TicketTypePo findPlatformByCode(String code) {
        return mapper.findPlatformByCode(code);
    }

    public TicketTypePo findPlatformByName(String name) {
        return mapper.findPlatformByName(name);
    }

    public int nextSortOrderPlatform() {
        Integer max = mapper.findMaxSortOrderPlatform();
        return max == null ? 0 : max + 1;
    }

    public long countLinkedDomainsByGlobalTypeId(long globalTypeId) {
        return mapper.countLinkedDomainsByGlobalTypeId(globalTypeId);
    }

    public void save(TicketTypePo po) {
        mapper.insert(po);
    }

    public void updateMetadata(
            long id,
            long domainId,
            String name,
            String description,
            String descriptionTemplateMd,
            String icon,
            String status) {
        mapper.updateMetadata(id, domainId, name, description, descriptionTemplateMd, icon, status);
    }

    public void updatePlatformMetadata(
            long id,
            String name,
            String description,
            String descriptionTemplateMd,
            String icon,
            String status) {
        mapper.updatePlatformMetadata(id, name, description, descriptionTemplateMd, icon, status);
    }

    public void updateSortOrder(long id, int sortOrder) {
        mapper.updateSortOrder(id, sortOrder);
    }

    public int deleteByIdAndDomainId(long id, long domainId) {
        return mapper.deleteByIdAndDomainId(id, domainId);
    }

    public int deletePlatformById(long id) {
        return mapper.deletePlatformById(id);
    }

    public int countTicketsByTypeId(long domainId, long typeId) {
        return mapper.countTicketsByTypeId(domainId, typeId);
    }

    public Long findFirstIdByDomainId(long domainId) {
        return mapper.findFirstIdByDomainId(domainId);
    }
}
