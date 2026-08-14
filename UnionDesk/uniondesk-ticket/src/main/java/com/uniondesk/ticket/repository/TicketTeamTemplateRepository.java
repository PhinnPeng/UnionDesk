package com.uniondesk.ticket.repository;

import com.mybatisflex.core.paginate.Page;
import com.uniondesk.ticket.entity.TicketTeamTemplateItemPo;
import com.uniondesk.ticket.entity.TicketTeamTemplatePo;
import com.uniondesk.ticket.mapper.TicketTeamTemplateItemMapper;
import com.uniondesk.ticket.mapper.TicketTeamTemplateMapper;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class TicketTeamTemplateRepository {

    public static final int NO_PAGINATION_THRESHOLD = 100;

    private final TicketTeamTemplateMapper templateMapper;
    private final TicketTeamTemplateItemMapper itemMapper;

    public TicketTeamTemplateRepository(
            TicketTeamTemplateMapper templateMapper,
            TicketTeamTemplateItemMapper itemMapper) {
        this.templateMapper = templateMapper;
        this.itemMapper = itemMapper;
    }

    public List<TicketTeamTemplatePo> findAll(String keywordLike) {
        return templateMapper.findAll(keywordLike);
    }

    public Page<TicketTeamTemplatePo> findPage(Page<TicketTeamTemplatePo> page, String keywordLike) {
        return templateMapper.selectPage(page, keywordLike);
    }

    public List<TicketTeamTemplatePo> findActiveOptions() {
        return templateMapper.findActiveOptions();
    }

    public TicketTeamTemplatePo findById(long id) {
        return templateMapper.findById(id);
    }

    public TicketTeamTemplatePo findRequiredById(long id) {
        TicketTeamTemplatePo po = templateMapper.findById(id);
        if (po == null) {
            throw new IllegalArgumentException("团队模板不存在");
        }
        return po;
    }

    public TicketTeamTemplatePo findByCode(String code) {
        return templateMapper.findByCode(code);
    }

    public int nextSortOrder() {
        Integer max = templateMapper.findMaxSortOrder();
        return max == null ? 0 : max + 1;
    }

    public void insert(TicketTeamTemplatePo po) {
        templateMapper.insert(po);
    }

    public void update(TicketTeamTemplatePo po) {
        templateMapper.update(po);
    }

    public void updateSortOrder(long id, int sortOrder, Long updatedBy) {
        templateMapper.updateSortOrder(id, sortOrder, updatedBy);
    }

    public int deleteById(long id) {
        return templateMapper.deleteById(id);
    }

    public List<TicketTeamTemplateItemPo> findItemsByTemplateId(long teamTemplateId) {
        return itemMapper.findByTemplateId(teamTemplateId);
    }

    public List<TicketTeamTemplateItemPo> findItemsByTemplateIds(List<Long> teamTemplateIds) {
        if (teamTemplateIds == null || teamTemplateIds.isEmpty()) {
            return Collections.emptyList();
        }
        return itemMapper.findByTemplateIds(teamTemplateIds);
    }

    public void insertItem(TicketTeamTemplateItemPo po) {
        itemMapper.insert(po);
    }

    public void deleteItemsByTemplateId(long teamTemplateId) {
        itemMapper.deleteByTemplateId(teamTemplateId);
    }
}
