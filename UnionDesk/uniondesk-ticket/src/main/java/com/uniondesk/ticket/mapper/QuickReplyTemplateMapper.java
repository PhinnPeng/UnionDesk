package com.uniondesk.ticket.mapper;

import com.uniondesk.ticket.entity.QuickReplyTemplatePo;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface QuickReplyTemplateMapper {

    List<QuickReplyTemplatePo> findByDomainId(@Param("domainId") long domainId);

    QuickReplyTemplatePo findByIdAndDomainId(@Param("id") long id, @Param("domainId") long domainId);

    QuickReplyTemplatePo findActiveByIdAndDomainId(@Param("id") long id, @Param("domainId") long domainId);

    void insert(QuickReplyTemplatePo po);

    void update(@Param("id") long id,
                @Param("domainId") long domainId,
                @Param("scopeType") String scopeType,
                @Param("title") String title,
                @Param("content") String content,
                @Param("sortOrder") int sortOrder);

    int deleteByIdAndDomainId(@Param("id") long id, @Param("domainId") long domainId);
}
