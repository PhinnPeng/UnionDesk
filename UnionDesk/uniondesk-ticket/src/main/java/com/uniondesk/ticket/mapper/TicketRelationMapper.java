package com.uniondesk.ticket.mapper;

import com.uniondesk.ticket.entity.TicketRelationPo;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TicketRelationMapper {

    void insert(TicketRelationPo po);
}
