package com.uniondesk.ticket.mapper;

import com.uniondesk.ticket.entity.TicketTeamTemplateItemPo;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TicketTeamTemplateItemMapper {

    List<TicketTeamTemplateItemPo> findByTemplateId(@Param("teamTemplateId") long teamTemplateId);

    List<TicketTeamTemplateItemPo> findByTemplateIds(@Param("teamTemplateIds") List<Long> teamTemplateIds);

    void insert(TicketTeamTemplateItemPo po);

    int deleteByTemplateId(@Param("teamTemplateId") long teamTemplateId);
}
