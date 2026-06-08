package com.uniondesk.ticket.mapper;

import com.uniondesk.ticket.entity.TicketTemplatePo;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TicketTemplateMapper {

    List<TicketTemplatePo> findByDomainId(@Param("domainId") long domainId);

    TicketTemplatePo findByIdAndDomainId(@Param("id") long id, @Param("domainId") long domainId);

    void insert(TicketTemplatePo po);

    void update(@Param("id") long id,
                @Param("domainId") long domainId,
                @Param("ticketTypeId") Long ticketTypeId,
                @Param("scope") String scope,
                @Param("name") String name,
                @Param("contentJson") String contentJson,
                @Param("sortOrder") int sortOrder);

    int deleteByIdAndDomainId(@Param("id") long id, @Param("domainId") long domainId);
}
