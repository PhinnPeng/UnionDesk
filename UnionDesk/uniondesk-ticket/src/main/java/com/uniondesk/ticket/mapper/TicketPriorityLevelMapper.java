package com.uniondesk.ticket.mapper;

import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.uniondesk.ticket.entity.TicketPriorityLevelPo;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TicketPriorityLevelMapper extends BaseMapper<TicketPriorityLevelPo> {

    default List<TicketPriorityLevelPo> findByDomainId(long domainId) {
        return selectListByQuery(QueryWrapper.create()
                .from(TicketPriorityLevelPo.class)
                .where(TicketPriorityLevelPo::getBusinessDomainId).eq(domainId)
                .orderBy(TicketPriorityLevelPo::getSortOrder, true)
                .orderBy(TicketPriorityLevelPo::getId, true));
    }

    default TicketPriorityLevelPo findByIdAndDomainId(long id, long domainId) {
        return selectOneByQuery(QueryWrapper.create()
                .from(TicketPriorityLevelPo.class)
                .where(TicketPriorityLevelPo::getId).eq(id)
                .and(TicketPriorityLevelPo::getBusinessDomainId).eq(domainId));
    }

    default int update(long id, long domainId, String code, String name, String color, String icon, int sortOrder, int isDefault) {
        TicketPriorityLevelPo set = new TicketPriorityLevelPo();
        set.setCode(code);
        set.setName(name);
        set.setColor(color);
        set.setIcon(icon);
        set.setSortOrder(sortOrder);
        set.setIsDefault(isDefault == 1);
        return updateByQuery(set, QueryWrapper.create()
                .from(TicketPriorityLevelPo.class)
                .where(TicketPriorityLevelPo::getId).eq(id)
                .and(TicketPriorityLevelPo::getBusinessDomainId).eq(domainId));
    }

    default int deleteByIdAndDomainId(long id, long domainId) {
        return deleteByQuery(QueryWrapper.create()
                .from(TicketPriorityLevelPo.class)
                .where(TicketPriorityLevelPo::getId).eq(id)
                .and(TicketPriorityLevelPo::getBusinessDomainId).eq(domainId));
    }

    default void clearDefaults(long domainId) {
        TicketPriorityLevelPo set = new TicketPriorityLevelPo();
        set.setIsDefault(false);
        updateByQuery(set, QueryWrapper.create()
                .from(TicketPriorityLevelPo.class)
                .where(TicketPriorityLevelPo::getBusinessDomainId).eq(domainId));
    }

    default void clearDefaultsExcept(long domainId, long keepId) {
        TicketPriorityLevelPo set = new TicketPriorityLevelPo();
        set.setIsDefault(false);
        updateByQuery(set, QueryWrapper.create()
                .from(TicketPriorityLevelPo.class)
                .where(TicketPriorityLevelPo::getBusinessDomainId).eq(domainId)
                .and(TicketPriorityLevelPo::getId).ne(keepId));
    }
}
