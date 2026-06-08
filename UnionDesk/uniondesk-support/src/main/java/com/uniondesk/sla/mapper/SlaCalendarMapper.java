package com.uniondesk.sla.mapper;

import com.uniondesk.sla.entity.SlaCalendarPo;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SlaCalendarMapper {

    List<SlaCalendarPo> selectByDomainId(
            @Param("domainId") long domainId,
            @Param("limit") int limit,
            @Param("offset") long offset);

    long countByDomainId(@Param("domainId") long domainId);

    SlaCalendarPo selectByIdAndDomainId(
            @Param("id") long id,
            @Param("domainId") long domainId);

    void insert(SlaCalendarPo po);

    void updateByIdAndDomainId(SlaCalendarPo po);
}
