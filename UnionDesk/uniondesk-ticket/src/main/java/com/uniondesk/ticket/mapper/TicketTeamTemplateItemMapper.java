package com.uniondesk.ticket.mapper;

import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.uniondesk.ticket.entity.TicketTeamTemplateItemPo;
import com.uniondesk.ticket.entity.TicketTypePo;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TicketTeamTemplateItemMapper extends BaseMapper<TicketTeamTemplateItemPo> {

    default List<TicketTeamTemplateItemPo> findByTemplateId(long teamTemplateId) {
        List<TicketTeamTemplateItemPo> items = selectListByQuery(QueryWrapper.create()
                .from(TicketTeamTemplateItemPo.class)
                .where(TicketTeamTemplateItemPo::getTeamTemplateId).eq(teamTemplateId)
                .orderBy(TicketTeamTemplateItemPo::getSortOrder, true)
                .orderBy(TicketTeamTemplateItemPo::getId, true));
        fillTicketTypeNames(items);
        return items;
    }

    default List<TicketTeamTemplateItemPo> findByTemplateIds(List<Long> teamTemplateIds) {
        if (teamTemplateIds == null || teamTemplateIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<TicketTeamTemplateItemPo> items = selectListByQuery(QueryWrapper.create()
                .from(TicketTeamTemplateItemPo.class)
                .where(TicketTeamTemplateItemPo::getTeamTemplateId).in(teamTemplateIds)
                .orderBy(TicketTeamTemplateItemPo::getTeamTemplateId, true)
                .orderBy(TicketTeamTemplateItemPo::getSortOrder, true)
                .orderBy(TicketTeamTemplateItemPo::getId, true));
        fillTicketTypeNames(items);
        return items;
    }

    default int deleteByTemplateId(long teamTemplateId) {
        return deleteByQuery(QueryWrapper.create()
                .from(TicketTeamTemplateItemPo.class)
                .where(TicketTeamTemplateItemPo::getTeamTemplateId).eq(teamTemplateId));
    }

    private void fillTicketTypeNames(List<TicketTeamTemplateItemPo> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        List<Long> typeIds = items.stream()
                .map(TicketTeamTemplateItemPo::getTicketTypeId)
                .distinct()
                .toList();
        if (typeIds.isEmpty()) {
            return;
        }
        Map<Long, TicketTypePo> typeById = selectListByQueryAs(QueryWrapper.create()
                .select(TicketTypePo::getId, TicketTypePo::getCode, TicketTypePo::getName)
                .from(TicketTypePo.class)
                .where(TicketTypePo::getId).in(typeIds), TicketTypePo.class).stream()
                .collect(Collectors.toMap(TicketTypePo::getId, Function.identity()));
        for (TicketTeamTemplateItemPo item : items) {
            TicketTypePo type = typeById.get(item.getTicketTypeId());
            if (type != null) {
                item.setTicketTypeCode(type.getCode());
                item.setTicketTypeName(type.getName());
            }
        }
    }
}
