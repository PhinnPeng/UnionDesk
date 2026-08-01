package com.uniondesk.ticket.repository;

import com.uniondesk.ticket.entity.TicketStatusPo;
import com.uniondesk.ticket.mapper.TicketStatusMapper;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class TicketStatusRepository {

    public static final int NO_PAGINATION_THRESHOLD = 100;

    private final TicketStatusMapper mapper;

    public TicketStatusRepository(TicketStatusMapper mapper) {
        this.mapper = mapper;
    }

    public List<TicketStatusPo> findAllPlatform(String keywordLike) {
        return mapper.findByPlatform(keywordLike);
    }

    public long countPlatform(String keywordLike) {
        return mapper.countByPlatform(keywordLike);
    }

    public List<TicketStatusPo> findPagePlatform(String keywordLike, int limit, long offset) {
        return mapper.findPageByPlatform(keywordLike, limit, offset);
    }

    public TicketStatusPo findById(long id) {
        return mapper.findById(id);
    }

    public TicketStatusPo findRequiredById(long id) {
        TicketStatusPo po = mapper.findById(id);
        if (po == null) {
            throw new IllegalArgumentException("状态不存在");
        }
        return po;
    }

    public TicketStatusPo findPlatformByCode(String code) {
        return mapper.findPlatformByCode(code);
    }

    public TicketStatusPo findPlatformByName(String name) {
        return mapper.findPlatformByName(name);
    }

    public int nextSortOrderPlatform() {
        Integer max = mapper.findMaxSortOrderPlatform();
        return max == null ? 0 : max + 1;
    }

    public List<TicketStatusPo> findAllDomain(long domainId, String keywordLike) {
        return mapper.findByDomain(domainId, keywordLike);
    }

    public long countDomain(long domainId, String keywordLike) {
        return mapper.countByDomain(domainId, keywordLike);
    }

    public List<TicketStatusPo> findPageDomain(long domainId, String keywordLike, int limit, long offset) {
        return mapper.findPageByDomain(domainId, keywordLike, limit, offset);
    }

    public TicketStatusPo findDomainByCode(long domainId, String code) {
        return mapper.findDomainByCode(domainId, code);
    }

    public TicketStatusPo findDomainByName(long domainId, String name) {
        return mapper.findDomainByName(domainId, name);
    }

    public TicketStatusPo findDomainBySourceStatusId(long domainId, long sourceStatusId) {
        return mapper.findDomainBySourceStatusId(domainId, sourceStatusId);
    }

    public int nextSortOrderDomain(long domainId) {
        Integer max = mapper.findMaxSortOrderDomain(domainId);
        return max == null ? 0 : max + 1;
    }

    public void insert(TicketStatusPo po) {
        mapper.insert(po);
    }

    public void update(TicketStatusPo po) {
        mapper.update(po);
    }

    public int deletePlatform(long id) {
        return mapper.deleteByIdPlatform(id);
    }

    public int deleteDomain(long domainId, long id) {
        return mapper.deleteByIdDomain(domainId, id);
    }
}
