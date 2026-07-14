package com.uniondesk.ticket.mapper;

import com.uniondesk.ticket.entity.TicketTypePo;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TicketTypeMapper {

    List<TicketTypePo> findByDomainId(@Param("domainId") long domainId);

    TicketTypePo findByIdAndDomainId(@Param("id") long id, @Param("domainId") long domainId);

    TicketTypePo findByDomainIdAndCode(@Param("domainId") long domainId, @Param("code") String code);

    TicketTypePo findByDomainIdAndName(@Param("domainId") long domainId, @Param("name") String name);

    List<TicketTypePo> findByPlatform(@Param("keywordLike") String keywordLike);

    long countByPlatform(@Param("keywordLike") String keywordLike);

    List<TicketTypePo> findPageByPlatform(
            @Param("keywordLike") String keywordLike,
            @Param("limit") int limit,
            @Param("offset") long offset);

    TicketTypePo findPlatformById(@Param("id") long id);

    TicketTypePo findPlatformByCode(@Param("code") String code);

    TicketTypePo findPlatformByName(@Param("name") String name);

    Integer findMaxSortOrderPlatform();

    long countLinkedDomainsByGlobalTypeId(@Param("globalTypeId") long globalTypeId);

    void insert(TicketTypePo po);

    void updateMetadata(@Param("id") long id,
                        @Param("domainId") long domainId,
                        @Param("name") String name,
                        @Param("description") String description,
                        @Param("icon") String icon,
                        @Param("status") String status);

    void updatePlatformMetadata(@Param("id") long id,
                                @Param("name") String name,
                                @Param("description") String description,
                                @Param("icon") String icon,
                                @Param("status") String status);

    void updateSortOrder(@Param("id") long id, @Param("sortOrder") int sortOrder);

    int deleteByIdAndDomainId(@Param("id") long id, @Param("domainId") long domainId);

    int deletePlatformById(@Param("id") long id);

    Long findFirstIdByDomainId(@Param("domainId") long domainId);

    int countTicketsByTypeId(@Param("domainId") long domainId, @Param("typeId") long typeId);
}
