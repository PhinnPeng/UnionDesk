package com.uniondesk.domain.mapper;

import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.query.If;
import com.mybatisflex.core.query.QueryWrapper;
import com.uniondesk.domain.entity.DomainMemberPo;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface DomainMemberMapper extends BaseMapper<DomainMemberPo> {

    default List<DomainMemberPo> listMembers(
            long domainId, String status, String keyword,
            LocalDateTime createdFrom, LocalDateTime createdTo, int limit, long offset) {
        QueryWrapper qw = baseMemberQuery(domainId, status, keyword, createdFrom, createdTo)
                .orderBy(DomainMemberPo::getId, false)
                .limit(offset, limit);
        return selectListByQuery(qw);
    }

    default long countMembers(
            long domainId, String status, String keyword,
            LocalDateTime createdFrom, LocalDateTime createdTo) {
        return selectCountByQuery(baseMemberQuery(domainId, status, keyword, createdFrom, createdTo));
    }

    default DomainMemberPo selectMemberById(long id, long domainId) {
        return selectOneByQuery(baseMemberQuery(domainId, null, null, null, null)
                .and(DomainMemberPo::getId).eq(id));
    }

    default void insert(long staffAccountId, long businessDomainId) {
        insertWithSource(staffAccountId, businessDomainId, "manual");
    }

    default void insertWithSource(long staffAccountId, long businessDomainId, String source) {
        DomainMemberPo po = new DomainMemberPo();
        po.setStaffAccountId(staffAccountId);
        po.setBusinessDomainId(businessDomainId);
        po.setStatus("active");
        po.setSource(source);
        insert(po);
    }

    @Update("UPDATE domain_member"
            + " SET status = #{status},"
            + " disabled_at = CASE WHEN #{status} = 'disabled' THEN CURRENT_TIMESTAMP(3) ELSE NULL END,"
            + " activated_at = CASE WHEN #{status} = 'active' THEN CURRENT_TIMESTAMP(3) ELSE activated_at END,"
            + " updated_at = CURRENT_TIMESTAMP(3)"
            + " WHERE id = #{memberId} AND business_domain_id = #{domainId} AND deleted_at IS NULL")
    int updateStatus(
            @Param("status") String status,
            @Param("memberId") long memberId,
            @Param("domainId") long domainId);

    @Update("UPDATE domain_member"
            + " SET status = 'deleted', deleted_at = CURRENT_TIMESTAMP(3), updated_at = CURRENT_TIMESTAMP(3)"
            + " WHERE id = #{memberId} AND business_domain_id = #{domainId} AND deleted_at IS NULL")
    int softDelete(@Param("memberId") long memberId, @Param("domainId") long domainId);

    @Select("SELECT COUNT(DISTINCT dm.id)"
            + " FROM domain_member dm"
            + " JOIN domain_member_role dmr ON dmr.domain_member_id = dm.id"
            + " JOIN domain_role dr ON dr.id = dmr.domain_role_id"
            + " WHERE dr.business_domain_id = #{domainId}"
            + " AND dr.code = 'domain_admin'"
            + " AND dm.status = 'active'"
            + " AND dm.deleted_at IS NULL"
            + " AND dm.id <> #{excludeMemberId}")
    int countActiveDomainAdmins(
            @Param("domainId") long domainId,
            @Param("excludeMemberId") long excludeMemberId);

    @Select("SELECT COUNT(DISTINCT dm.id)"
            + " FROM domain_member dm"
            + " JOIN domain_member_role dmr ON dmr.domain_member_id = dm.id"
            + " JOIN domain_role dr ON dr.id = dmr.domain_role_id"
            + " WHERE dr.business_domain_id = #{domainId}"
            + " AND dr.code = 'super_admin'"
            + " AND dm.status = 'active'"
            + " AND dm.deleted_at IS NULL"
            + " AND dm.id <> #{excludeMemberId}")
    int countActiveDomainSuperAdmins(
            @Param("domainId") long domainId,
            @Param("excludeMemberId") long excludeMemberId);

    default int countByDomainAndStaff(long domainId, long staffAccountId) {
        return (int) selectCountByQuery(QueryWrapper.create()
                .from(DomainMemberPo.class)
                .where(DomainMemberPo::getBusinessDomainId).eq(domainId)
                .and(DomainMemberPo::getStaffAccountId).eq(staffAccountId)
                .and(DomainMemberPo::getDeletedAt).isNull());
    }

    default Long findMemberIdByDomainAndStaff(long domainId, long staffAccountId) {
        DomainMemberPo po = selectOneByQuery(QueryWrapper.create()
                .from(DomainMemberPo.class)
                .select(DomainMemberPo::getId)
                .where(DomainMemberPo::getBusinessDomainId).eq(domainId)
                .and(DomainMemberPo::getStaffAccountId).eq(staffAccountId)
                .and(DomainMemberPo::getDeletedAt).isNull()
                .orderBy(DomainMemberPo::getId, false));
        return po == null ? null : po.getId();
    }

    default Long findActiveMemberId(long domainId, long staffAccountId) {
        DomainMemberPo po = selectOneByQuery(QueryWrapper.create()
                .from(DomainMemberPo.class)
                .select(DomainMemberPo::getId)
                .where(DomainMemberPo::getBusinessDomainId).eq(domainId)
                .and(DomainMemberPo::getStaffAccountId).eq(staffAccountId)
                .and(DomainMemberPo::getDeletedAt).isNull());
        return po == null ? null : po.getId();
    }

    private QueryWrapper baseMemberQuery(
            long domainId, String status, String keyword,
            LocalDateTime createdFrom, LocalDateTime createdTo) {
        QueryWrapper qw = QueryWrapper.create()
                .select(
                        "domain_member.id",
                        "domain_member.staff_account_id",
                        "domain_member.business_domain_id",
                        "domain_member.status",
                        "domain_member.source",
                        "domain_member.activated_at",
                        "domain_member.disabled_at",
                        "domain_member.deleted_at",
                        "domain_member.created_at",
                        "sa.username",
                        "sa.phone",
                        "sa.email")
                .from(DomainMemberPo.class)
                .join("staff_account").as("sa").on("sa.id = domain_member.staff_account_id")
                .where(DomainMemberPo::getBusinessDomainId).eq(domainId)
                .and(DomainMemberPo::getStatus).eq(status, If::hasText)
                .and(DomainMemberPo::getDeletedAt).isNull();
        if (keyword != null) {
            qw.and("(sa.username LIKE ? OR sa.phone LIKE ? OR sa.email LIKE ?"
                    + " OR sa.real_name LIKE ? OR sa.nickname LIKE ?)",
                    keyword, keyword, keyword, keyword, keyword);
        }
        if (createdFrom != null) {
            qw.and(DomainMemberPo::getCreatedAt).ge(createdFrom);
        }
        if (createdTo != null) {
            qw.and(DomainMemberPo::getCreatedAt).le(createdTo);
        }
        return qw;
    }
}
