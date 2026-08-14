package com.uniondesk.ticket.mapper;

import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.query.QueryMethods;
import com.mybatisflex.core.query.QueryWrapper;
import com.uniondesk.ticket.entity.TicketTypeAttributeSlotPo;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TicketTypeAttributeSlotMapper extends BaseMapper<TicketTypeAttributeSlotPo> {

    default List<TicketTypeAttributeSlotPo> findByTicketTypeId(long ticketTypeId) {
        return selectListByQuery(QueryWrapper.create()
                .from(TicketTypeAttributeSlotPo.class)
                .where(TicketTypeAttributeSlotPo::getTicketTypeId).eq(ticketTypeId)
                .orderBy(TicketTypeAttributeSlotPo::getSortOrder, true)
                .orderBy(TicketTypeAttributeSlotPo::getId, true));
    }

    default TicketTypeAttributeSlotPo findById(long id) {
        return selectOneByQuery(QueryWrapper.create()
                .from(TicketTypeAttributeSlotPo.class)
                .where(TicketTypeAttributeSlotPo::getId).eq(id));
    }

    default TicketTypeAttributeSlotPo findByTypeAndAttribute(long ticketTypeId, long attributeId) {
        return selectOneByQuery(QueryWrapper.create()
                .from(TicketTypeAttributeSlotPo.class)
                .where(TicketTypeAttributeSlotPo::getTicketTypeId).eq(ticketTypeId)
                .and(TicketTypeAttributeSlotPo::getAttributeId).eq(attributeId));
    }

    default Integer findMaxSortOrder(long ticketTypeId) {
        return selectObjectByQueryAs(QueryWrapper.create()
                .select(QueryMethods.max(TicketTypeAttributeSlotPo::getSortOrder))
                .from(TicketTypeAttributeSlotPo.class)
                .where(TicketTypeAttributeSlotPo::getTicketTypeId).eq(ticketTypeId), Integer.class);
    }

    default int updateSlotConfig(long id, String slotConfig, Long updatedBy) {
        TicketTypeAttributeSlotPo set = new TicketTypeAttributeSlotPo();
        set.setSlotConfig(slotConfig);
        set.setUpdatedBy(updatedBy);
        return updateByQuery(set, QueryWrapper.create()
                .from(TicketTypeAttributeSlotPo.class)
                .where(TicketTypeAttributeSlotPo::getId).eq(id));
    }

    default int updateSortOrder(long id, int sortOrder, Long updatedBy) {
        TicketTypeAttributeSlotPo set = new TicketTypeAttributeSlotPo();
        set.setSortOrder(sortOrder);
        set.setUpdatedBy(updatedBy);
        return updateByQuery(set, QueryWrapper.create()
                .from(TicketTypeAttributeSlotPo.class)
                .where(TicketTypeAttributeSlotPo::getId).eq(id));
    }

    default int deleteById(long id) {
        return deleteByQuery(QueryWrapper.create()
                .from(TicketTypeAttributeSlotPo.class)
                .where(TicketTypeAttributeSlotPo::getId).eq(id));
    }

    default int deleteByTicketTypeId(long ticketTypeId) {
        return deleteByQuery(QueryWrapper.create()
                .from(TicketTypeAttributeSlotPo.class)
                .where(TicketTypeAttributeSlotPo::getTicketTypeId).eq(ticketTypeId));
    }

    default int countByAttributeId(long attributeId) {
        return (int) selectCountByQuery(QueryWrapper.create()
                .from(TicketTypeAttributeSlotPo.class)
                .where(TicketTypeAttributeSlotPo::getAttributeId).eq(attributeId));
    }
}
