package com.uniondesk.ticket.mapper;

import com.uniondesk.ticket.entity.TicketStatusPo;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TicketStatusMapper {

    List<TicketStatusPo> findByPlatform(@Param("keywordLike") String keywordLike);

    long countByPlatform(@Param("keywordLike") String keywordLike);

    List<TicketStatusPo> findPageByPlatform(
            @Param("keywordLike") String keywordLike,
            @Param("limit") int limit,
            @Param("offset") long offset);

    TicketStatusPo findById(@Param("id") long id);

    TicketStatusPo findPlatformByCode(@Param("code") String code);

    TicketStatusPo findPlatformByName(@Param("name") String name);

    Integer findMaxSortOrderPlatform();

    void insert(TicketStatusPo po);

    int update(TicketStatusPo po);

    int deleteByIdPlatform(@Param("id") long id);
}
