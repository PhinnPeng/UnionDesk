package com.uniondesk.ticket.mapper;

import com.uniondesk.ticket.entity.TicketPriorityLevelPo;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TicketPriorityLevelMapper {

    List<TicketPriorityLevelPo> findByDomainId(@Param("domainId") long domainId);

    TicketPriorityLevelPo findByIdAndDomainId(@Param("id") long id, @Param("domainId") long domainId);

    void insert(TicketPriorityLevelPo po);

    void update(@Param("id") long id,
                @Param("domainId") long domainId,
                @Param("code") String code,
                @Param("name") String name,
                @Param("sortOrder") int sortOrder,
                @Param("isDefault") int isDefault);

    int deleteByIdAndDomainId(@Param("id") long id, @Param("domainId") long domainId);

    void clearDefaults(@Param("domainId") long domainId);

    void clearDefaultsExcept(@Param("domainId") long domainId, @Param("keepId") long keepId);
}
