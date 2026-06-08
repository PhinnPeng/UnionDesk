package com.uniondesk.ticket.mapper;

import com.uniondesk.ticket.entity.TicketTypePo;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TicketTypeMapper {

    List<TicketTypePo> findByDomainId(@Param("domainId") long domainId);

    TicketTypePo findByIdAndDomainId(@Param("id") long id, @Param("domainId") long domainId);

    void insert(TicketTypePo po);

    void update(@Param("id") long id,
                @Param("domainId") long domainId,
                @Param("name") String name,
                @Param("statusFlowConfig") String statusFlowConfig);

    int deleteByIdAndDomainId(@Param("id") long id, @Param("domainId") long domainId);

    Long findFirstIdByDomainId(@Param("domainId") long domainId);
}
