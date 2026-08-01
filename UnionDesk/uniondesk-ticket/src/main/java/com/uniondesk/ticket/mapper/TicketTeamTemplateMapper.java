package com.uniondesk.ticket.mapper;

import com.uniondesk.ticket.entity.TicketTeamTemplatePo;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TicketTeamTemplateMapper {

    List<TicketTeamTemplatePo> findAll(@Param("keywordLike") String keywordLike);

    long countAll(@Param("keywordLike") String keywordLike);

    List<TicketTeamTemplatePo> findPage(
            @Param("keywordLike") String keywordLike,
            @Param("limit") int limit,
            @Param("offset") long offset);

    List<TicketTeamTemplatePo> findActiveOptions();

    TicketTeamTemplatePo findById(@Param("id") long id);

    TicketTeamTemplatePo findByCode(@Param("code") String code);

    Integer findMaxSortOrder();

    void insert(TicketTeamTemplatePo po);

    int update(TicketTeamTemplatePo po);

    int updateSortOrder(@Param("id") long id, @Param("sortOrder") int sortOrder, @Param("updatedBy") Long updatedBy);

    int deleteById(@Param("id") long id);
}
