package com.uniondesk.domain.mapper;

import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.uniondesk.domain.entity.RoleTemplateDomainPo;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface RoleTemplateDomainMapper extends BaseMapper<RoleTemplateDomainPo> {

    default List<RoleTemplateDomainPo> selectByTemplateId(long templateId) {
        return selectListByQuery(QueryWrapper.create()
                .from(RoleTemplateDomainPo.class)
                .where(RoleTemplateDomainPo::getTemplateId).eq(templateId)
                .orderBy(RoleTemplateDomainPo::getId, true));
    }

    default RoleTemplateDomainPo selectByTemplateAndDomain(long templateId, long domainId) {
        return selectOneByQuery(QueryWrapper.create()
                .from(RoleTemplateDomainPo.class)
                .where(RoleTemplateDomainPo::getTemplateId).eq(templateId)
                .and(RoleTemplateDomainPo::getBusinessDomainId).eq(domainId));
    }

    default int countByTemplateId(long templateId) {
        return (int) selectCountByQuery(QueryWrapper.create()
                .from(RoleTemplateDomainPo.class)
                .where(RoleTemplateDomainPo::getTemplateId).eq(templateId));
    }

    default void insert(long templateId, long domainId, long instanceDomainRoleId, String syncMode) {
        RoleTemplateDomainPo po = new RoleTemplateDomainPo();
        po.setTemplateId(templateId);
        po.setBusinessDomainId(domainId);
        po.setInstanceDomainRoleId(instanceDomainRoleId);
        po.setSyncMode(syncMode);
        insert(po);
    }

    default void deleteByTemplateAndDomain(long templateId, long domainId) {
        deleteByQuery(QueryWrapper.create()
                .from(RoleTemplateDomainPo.class)
                .where(RoleTemplateDomainPo::getTemplateId).eq(templateId)
                .and(RoleTemplateDomainPo::getBusinessDomainId).eq(domainId));
    }

    @Update("UPDATE role_template_domain SET updated_at = CURRENT_TIMESTAMP(3)"
            + " WHERE template_id = #{templateId} AND business_domain_id = #{domainId}")
    void touch(@Param("templateId") long templateId, @Param("domainId") long domainId);
}
