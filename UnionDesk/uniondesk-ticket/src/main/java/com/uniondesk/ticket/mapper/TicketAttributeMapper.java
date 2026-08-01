package com.uniondesk.ticket.mapper;

import com.uniondesk.ticket.entity.TicketAttributePo;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TicketAttributeMapper {

    List<TicketAttributePo> findByPlatform(@Param("keywordLike") String keywordLike);

    List<TicketAttributePo> findByDomain(@Param("domainId") long domainId, @Param("keywordLike") String keywordLike);

    long countByPlatform(@Param("keywordLike") String keywordLike);

    long countByDomain(@Param("domainId") long domainId, @Param("keywordLike") String keywordLike);

    List<TicketAttributePo> findPageByPlatform(
            @Param("keywordLike") String keywordLike,
            @Param("limit") int limit,
            @Param("offset") long offset);

    List<TicketAttributePo> findPageByDomain(
            @Param("domainId") long domainId,
            @Param("keywordLike") String keywordLike,
            @Param("limit") int limit,
            @Param("offset") long offset);

    TicketAttributePo findById(@Param("id") long id);

    TicketAttributePo findPlatformByName(@Param("name") String name);

    TicketAttributePo findPlatformBySystemKey(@Param("systemKey") String systemKey);

    TicketAttributePo findDomainByName(@Param("domainId") long domainId, @Param("name") String name);

    TicketAttributePo findDomainBySystemKey(@Param("domainId") long domainId, @Param("systemKey") String systemKey);

    TicketAttributePo findDomainBySourceAttributeId(
            @Param("domainId") long domainId,
            @Param("sourceAttributeId") long sourceAttributeId);

    Integer findMaxSortOrderPlatform();

    Integer findMaxSortOrderDomain(@Param("domainId") long domainId);

    void insert(TicketAttributePo po);

    int update(TicketAttributePo po);

    int deleteByIdPlatform(@Param("id") long id);

    int deleteByIdDomain(@Param("id") long id, @Param("domainId") long domainId);

    int updateSortOrder(@Param("id") long id, @Param("sortOrder") int sortOrder, @Param("updatedBy") Long updatedBy);
}
