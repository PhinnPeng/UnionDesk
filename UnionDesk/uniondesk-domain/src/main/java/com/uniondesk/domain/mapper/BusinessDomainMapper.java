package com.uniondesk.domain.mapper;

import com.uniondesk.domain.entity.BusinessDomainPo;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface BusinessDomainMapper {

    List<BusinessDomainPo> selectAdminDomains(
            @Param("status") Integer status,
            @Param("includeDeleted") boolean includeDeleted,
            @Param("keyword") String keyword,
            @Param("createdFrom") LocalDateTime createdFrom,
            @Param("createdTo") LocalDateTime createdTo,
            @Param("limit") int limit,
            @Param("offset") long offset);

    long countAdminDomains(
            @Param("status") Integer status,
            @Param("includeDeleted") boolean includeDeleted,
            @Param("keyword") String keyword,
            @Param("createdFrom") LocalDateTime createdFrom,
            @Param("createdTo") LocalDateTime createdTo);

    List<BusinessDomainPo> selectBriefDomains(
            @Param("keyword") String keyword,
            @Param("limit") int limit,
            @Param("offset") long offset);

    long countBriefDomains(@Param("keyword") String keyword);

    BusinessDomainPo selectById(@Param("id") long id);

    Long selectIdByCode(@Param("code") String code);

    void insert(BusinessDomainPo po);

    int updateDomain(
            @Param("code") String code,
            @Param("name") String name,
            @Param("description") String description,
            @Param("logo") String logo,
            @Param("visibilityPolicy") String visibilityPolicy,
            @Param("visibilityPolicyCodes") String visibilityPolicyCodes,
            @Param("registrationEnabled") String registrationEnabled,
            @Param("invitationEnabled") String invitationEnabled,
            @Param("status") int status,
            @Param("updatedBy") Long updatedBy,
            @Param("id") long id);

    int softDelete(@Param("id") long id, @Param("updatedBy") Long updatedBy);
}
