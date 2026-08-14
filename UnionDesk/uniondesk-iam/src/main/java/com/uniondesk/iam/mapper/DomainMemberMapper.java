package com.uniondesk.iam.mapper;

import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.uniondesk.iam.entity.DomainMemberPo;
import com.uniondesk.iam.entity.DomainMemberPresentationPo;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface DomainMemberMapper extends BaseMapper<DomainMemberPo> {

    default DomainMemberPresentationPo selectPresentation(long staffAccountId, long domainId) {
        return selectOneByQueryAs(QueryWrapper.create()
                .from(DomainMemberPo.class)
                .select(DomainMemberPo::getDomainNickname, DomainMemberPo::getDomainAvatarUrl,
                        DomainMemberPo::getDomainContactPhone, DomainMemberPo::getDomainContactEmail)
                .where(DomainMemberPo::getStaffAccountId).eq(staffAccountId)
                .and(DomainMemberPo::getBusinessDomainId).eq(domainId)
                .and("deleted_at IS NULL"), DomainMemberPresentationPo.class);
    }

    List<Long> selectDistinctDomainIds(@Param("staffAccountId") long staffAccountId);

    List<String> selectRoleCodes(@Param("staffAccountId") long staffAccountId);

    default Long selectId(long businessDomainId, long staffAccountId) {
        return selectObjectByQueryAs(QueryWrapper.create()
                .from(DomainMemberPo.class)
                .select(DomainMemberPo::getId)
                .where(DomainMemberPo::getBusinessDomainId).eq(businessDomainId)
                .and(DomainMemberPo::getStaffAccountId).eq(staffAccountId)
                .and("deleted_at IS NULL"), Long.class);
    }

    void insertRow(@Param("staffAccountId") long staffAccountId, @Param("businessDomainId") long businessDomainId);

    void deleteRolesByMemberId(@Param("domainMemberId") long domainMemberId);

    void insertRole(@Param("domainMemberId") long domainMemberId, @Param("domainRoleId") long domainRoleId);
}
