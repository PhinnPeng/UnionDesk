package com.uniondesk.domain.mapper;

import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.uniondesk.domain.entity.BusinessDomainPo;
import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface BusinessDomainMapper extends BaseMapper<BusinessDomainPo> {

    default Page<BusinessDomainPo> selectPageByAdmin(
            Page<BusinessDomainPo> page,
            Integer status,
            boolean includeDeleted,
            String keywordLike,
            LocalDateTime createdFrom,
            LocalDateTime createdTo) {
        QueryWrapper qw = QueryWrapper.create()
                .select(
                        "business_domain.id",
                        "business_domain.code",
                        "business_domain.name",
                        "business_domain.description",
                        "business_domain.logo",
                        "business_domain.visibility_policy_codes",
                        "business_domain.registration_enabled",
                        "business_domain.invitation_enabled",
                        "business_domain.status",
                        "business_domain.created_at",
                        "business_domain.updated_at",
                        "business_domain.deleted_at",
                        "business_domain.created_by",
                        "business_domain.updated_by",
                        "uc.username AS creator_name",
                        "uu.username AS updater_name")
                .from(BusinessDomainPo.class)
                .leftJoin("staff_account").as("uc").on("uc.id = business_domain.created_by")
                .leftJoin("staff_account").as("uu").on("uu.id = business_domain.updated_by")
                .where(BusinessDomainPo::getDeletedAt).isNull(!includeDeleted);
        if (status != null) {
            qw.and(BusinessDomainPo::getStatus).eq(status);
        }
        if (keywordLike != null) {
            qw.and("(business_domain.code LIKE ? OR business_domain.name LIKE ?)", keywordLike, keywordLike);
        }
        if (createdFrom != null) {
            qw.and(BusinessDomainPo::getCreatedAt).ge(createdFrom);
        }
        if (createdTo != null) {
            qw.and(BusinessDomainPo::getCreatedAt).le(createdTo);
        }
        qw.orderBy(BusinessDomainPo::getId, false);
        return paginate(page, qw);
    }

    default Page<BusinessDomainPo> selectPageByBrief(Page<BusinessDomainPo> page, String keywordLike) {
        QueryWrapper qw = QueryWrapper.create()
                .from(BusinessDomainPo.class)
                .where(BusinessDomainPo::getStatus).eq(1);
        if (keywordLike != null) {
            qw.and("(business_domain.code LIKE ? OR business_domain.name LIKE ?)", keywordLike, keywordLike);
        }
        qw.orderBy(BusinessDomainPo::getId, false);
        return paginate(page, qw);
    }

    default BusinessDomainPo selectById(long id) {
        return selectOneByQuery(QueryWrapper.create()
                .select(
                        "business_domain.id",
                        "business_domain.code",
                        "business_domain.name",
                        "business_domain.description",
                        "business_domain.logo",
                        "business_domain.visibility_policy_codes",
                        "business_domain.registration_enabled",
                        "business_domain.invitation_enabled",
                        "business_domain.status",
                        "business_domain.created_at",
                        "business_domain.updated_at",
                        "business_domain.deleted_at",
                        "business_domain.created_by",
                        "business_domain.updated_by",
                        "uc.username AS creator_name",
                        "uu.username AS updater_name")
                .from(BusinessDomainPo.class)
                .leftJoin("staff_account").as("uc").on("uc.id = business_domain.created_by")
                .leftJoin("staff_account").as("uu").on("uu.id = business_domain.updated_by")
                .where(BusinessDomainPo::getId).eq(id)
                .and(BusinessDomainPo::getDeletedAt).isNull());
    }

    default Long selectIdByCode(String code) {
        BusinessDomainPo po = selectOneByQuery(QueryWrapper.create()
                .from(BusinessDomainPo.class)
                .select(BusinessDomainPo::getId)
                .where(BusinessDomainPo::getCode).eq(code));
        return po == null ? null : po.getId();
    }

    default int updateDomain(
            String code, String name, String description, String logo,
            String visibilityPolicy, String visibilityPolicyCodes,
            String registrationEnabled, String invitationEnabled,
            int status, Long updatedBy, long id) {
        BusinessDomainPo set = new BusinessDomainPo();
        set.setCode(code);
        set.setName(name);
        set.setDescription(description);
        set.setLogo(logo);
        set.setVisibilityPolicy(visibilityPolicy);
        set.setVisibilityPolicyCodes(visibilityPolicyCodes);
        set.setRegistrationEnabled(registrationEnabled);
        set.setInvitationEnabled(invitationEnabled);
        set.setStatus(status);
        set.setUpdatedBy(updatedBy);
        return updateByQuery(set, QueryWrapper.create()
                .where(BusinessDomainPo::getId).eq(id)
                .and(BusinessDomainPo::getDeletedAt).isNull());
    }

    @Update("UPDATE business_domain SET deleted_at = CURRENT_TIMESTAMP(3),"
            + " updated_at = CURRENT_TIMESTAMP(3), updated_by = #{updatedBy}"
            + " WHERE id = #{id} AND deleted_at IS NULL")
    int softDelete(@Param("id") long id, @Param("updatedBy") Long updatedBy);

    @Update("UPDATE business_domain SET applied_team_template_id = #{templateId},"
            + " applied_team_template_version = #{templateVersion},"
            + " applied_team_template_at = CURRENT_TIMESTAMP(3),"
            + " updated_at = CURRENT_TIMESTAMP(3), updated_by = #{updatedBy}"
            + " WHERE id = #{id} AND deleted_at IS NULL")
    int updateAppliedTeamTemplate(
            @Param("id") long id,
            @Param("templateId") long templateId,
            @Param("templateVersion") int templateVersion,
            @Param("updatedBy") Long updatedBy);
}
