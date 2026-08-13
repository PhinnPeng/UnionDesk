package com.uniondesk.domain.mapper;

import com.uniondesk.domain.entity.RoleTemplateDomainPo;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RoleTemplateDomainMapper {

    List<RoleTemplateDomainPo> selectByTemplateId(@Param("templateId") long templateId);

    RoleTemplateDomainPo selectByTemplateAndDomain(
            @Param("templateId") long templateId,
            @Param("domainId") long domainId);

    int countByTemplateId(@Param("templateId") long templateId);

    void insert(
            @Param("templateId") long templateId,
            @Param("domainId") long domainId,
            @Param("instanceDomainRoleId") long instanceDomainRoleId,
            @Param("syncMode") String syncMode);

    void deleteByTemplateAndDomain(
            @Param("templateId") long templateId,
            @Param("domainId") long domainId);

    void touch(
            @Param("templateId") long templateId,
            @Param("domainId") long domainId);
}
