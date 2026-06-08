package com.uniondesk.domain.mapper;

import com.uniondesk.domain.entity.DomainCustomerPo;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface DomainCustomerMapper {

    List<DomainCustomerPo> listCustomers(
            @Param("domainId") long domainId,
            @Param("status") String status,
            @Param("keyword") String keyword,
            @Param("limit") int limit,
            @Param("offset") long offset);

    long countCustomers(
            @Param("domainId") long domainId,
            @Param("status") String status,
            @Param("keyword") String keyword);

    DomainCustomerPo selectById(
            @Param("id") long id,
            @Param("domainId") long domainId);

    int insert(DomainCustomerPo po);

    int countByDomainAndCustomer(
            @Param("domainId") long domainId,
            @Param("customerAccountId") long customerAccountId);

    int updateStatus(
            @Param("status") String status,
            @Param("activatedAt") LocalDateTime activatedAt,
            @Param("disabledAt") LocalDateTime disabledAt,
            @Param("id") long id,
            @Param("domainId") long domainId);
}
