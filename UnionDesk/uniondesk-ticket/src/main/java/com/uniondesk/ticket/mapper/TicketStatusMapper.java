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

    List<TicketStatusPo> findByDomain(
            @Param("domainId") long domainId,
            @Param("keywordLike") String keywordLike);

    long countByDomain(
            @Param("domainId") long domainId,
            @Param("keywordLike") String keywordLike);

    List<TicketStatusPo> findPageByDomain(
            @Param("domainId") long domainId,
            @Param("keywordLike") String keywordLike,
            @Param("limit") int limit,
            @Param("offset") long offset);

    TicketStatusPo findDomainByCode(
            @Param("domainId") long domainId,
            @Param("code") String code);

    TicketStatusPo findDomainByName(
            @Param("domainId") long domainId,
            @Param("name") String name);

    TicketStatusPo findDomainBySourceStatusId(
            @Param("domainId") long domainId,
            @Param("sourceStatusId") long sourceStatusId);

    Integer findMaxSortOrderDomain(@Param("domainId") long domainId);

    void insert(TicketStatusPo po);

    int update(TicketStatusPo po);

    int deleteByIdPlatform(@Param("id") long id);

    int deleteByIdDomain(@Param("domainId") long domainId, @Param("id") long id);
}
