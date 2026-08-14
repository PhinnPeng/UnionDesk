package com.uniondesk.ticket.mapper;

import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.uniondesk.ticket.entity.TicketTemplatePo;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TicketTemplateMapper extends BaseMapper<TicketTemplatePo> {

    default List<TicketTemplatePo> findByDomainId(long domainId) {
        return selectListByQuery(QueryWrapper.create()
                .from(TicketTemplatePo.class)
                .where(TicketTemplatePo::getBusinessDomainId).eq(domainId)
                .orderBy(TicketTemplatePo::getSortOrder, true)
                .orderBy(TicketTemplatePo::getId, true));
    }

    default TicketTemplatePo findByIdAndDomainId(long id, long domainId) {
        return selectOneByQuery(QueryWrapper.create()
                .from(TicketTemplatePo.class)
                .where(TicketTemplatePo::getId).eq(id)
                .and(TicketTemplatePo::getBusinessDomainId).eq(domainId));
    }

    default int update(long id, long domainId, Long ticketTypeId, String scope, String name, String contentJson, int sortOrder) {
        TicketTemplatePo set = new TicketTemplatePo();
        set.setTicketTypeId(ticketTypeId);
        set.setScope(scope);
        set.setName(name);
        set.setContentJson(contentJson);
        set.setSortOrder(sortOrder);
        return updateByQuery(set, QueryWrapper.create()
                .from(TicketTemplatePo.class)
                .where(TicketTemplatePo::getId).eq(id)
                .and(TicketTemplatePo::getBusinessDomainId).eq(domainId));
    }

    default int deleteByIdAndDomainId(long id, long domainId) {
        return deleteByQuery(QueryWrapper.create()
                .from(TicketTemplatePo.class)
                .where(TicketTemplatePo::getId).eq(id)
                .and(TicketTemplatePo::getBusinessDomainId).eq(domainId));
    }
}
