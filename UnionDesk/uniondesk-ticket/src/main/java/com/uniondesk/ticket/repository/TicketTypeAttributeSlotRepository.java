package com.uniondesk.ticket.repository;

import com.uniondesk.ticket.entity.TicketTypeAttributeSlotPo;
import com.uniondesk.ticket.mapper.TicketTypeAttributeSlotMapper;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class TicketTypeAttributeSlotRepository {

    private final TicketTypeAttributeSlotMapper mapper;

    public TicketTypeAttributeSlotRepository(TicketTypeAttributeSlotMapper mapper) {
        this.mapper = mapper;
    }

    public List<TicketTypeAttributeSlotPo> findByTicketTypeId(long ticketTypeId) {
        return mapper.findByTicketTypeId(ticketTypeId);
    }

    public TicketTypeAttributeSlotPo findById(long id) {
        return mapper.findById(id);
    }

    public TicketTypeAttributeSlotPo findRequiredById(long id) {
        TicketTypeAttributeSlotPo po = mapper.findById(id);
        if (po == null) {
            throw new IllegalArgumentException("属性插槽不存在");
        }
        return po;
    }

    public TicketTypeAttributeSlotPo findByTypeAndAttribute(long ticketTypeId, long attributeId) {
        return mapper.findByTypeAndAttribute(ticketTypeId, attributeId);
    }

    public int nextSortOrder(long ticketTypeId) {
        Integer max = mapper.findMaxSortOrder(ticketTypeId);
        return max == null ? 0 : max + 1;
    }

    public void insert(TicketTypeAttributeSlotPo po) {
        mapper.insert(po);
    }

    public void updateSlotConfig(long id, String slotConfig, Long updatedBy) {
        mapper.updateSlotConfig(id, slotConfig, updatedBy);
    }

    public void updateSortOrder(long id, int sortOrder, Long updatedBy) {
        mapper.updateSortOrder(id, sortOrder, updatedBy);
    }

    public int deleteById(long id) {
        return mapper.deleteById(id);
    }

    public void deleteByTicketTypeId(long ticketTypeId) {
        mapper.deleteByTicketTypeId(ticketTypeId);
    }

    public int countByAttributeId(long attributeId) {
        return mapper.countByAttributeId(attributeId);
    }
}
