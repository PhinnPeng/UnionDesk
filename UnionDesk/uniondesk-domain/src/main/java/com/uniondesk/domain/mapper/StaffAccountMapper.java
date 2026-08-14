package com.uniondesk.domain.mapper;

import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.uniondesk.domain.entity.StaffAccountPo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface StaffAccountMapper extends BaseMapper<StaffAccountPo> {

    default int countById(long id) {
        return (int) selectCountByQuery(QueryWrapper.create()
                .from(StaffAccountPo.class)
                .where(StaffAccountPo::getId).eq(id));
    }

    default Long selectIdById(long id) {
        StaffAccountPo po = selectOneByQuery(QueryWrapper.create()
                .from(StaffAccountPo.class)
                .select(StaffAccountPo::getId)
                .where(StaffAccountPo::getId).eq(id));
        return po == null ? null : po.getId();
    }

    default String selectUsernameById(long id) {
        StaffAccountPo po = selectOneByQuery(QueryWrapper.create()
                .from(StaffAccountPo.class)
                .select(StaffAccountPo::getUsername)
                .where(StaffAccountPo::getId).eq(id));
        return po == null ? null : po.getUsername();
    }

    default Long selectIdByUsername(String username) {
        StaffAccountPo po = selectOneByQuery(QueryWrapper.create()
                .from(StaffAccountPo.class)
                .select(StaffAccountPo::getId)
                .where(StaffAccountPo::getUsername).eq(username));
        return po == null ? null : po.getId();
    }

    @Select("SELECT sa.id, sa.username, sa.phone, sa.email"
            + " FROM domain_member dm"
            + " JOIN staff_account sa ON sa.id = dm.staff_account_id"
            + " WHERE dm.business_domain_id = #{domainId}"
            + " AND dm.staff_account_id = #{staffAccountId}"
            + " AND dm.deleted_at IS NULL"
            + " LIMIT 1")
    StaffAccountPo selectStaffInDomain(
            @Param("domainId") long domainId,
            @Param("staffAccountId") long staffAccountId);
}
