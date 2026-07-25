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

    public void insert(TicketStatusPo po) {
        mapper.insert(po);
    }

    public void update(TicketStatusPo po) {
        mapper.update(po);
    }

    public int deletePlatform(long id) {
        return mapper.deleteByIdPlatform(id);
    }
}
