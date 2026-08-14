package com.uniondesk.iam.mapper;

import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.uniondesk.iam.entity.StaffAccountPo;
import com.uniondesk.iam.entity.StaffAccountPresentationPo;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface StaffAccountMapper extends BaseMapper<StaffAccountPo> {

    default List<StaffAccountPo> selectAll() {
        return selectListByQuery(QueryWrapper.create()
                .from(StaffAccountPo.class)
                .select(StaffAccountPo::getId, StaffAccountPo::getSubjectId,
                        StaffAccountPo::getUsername, StaffAccountPo::getRealName,
                        StaffAccountPo::getNickname, StaffAccountPo::getAvatarUrl,
                        StaffAccountPo::getPhone, StaffAccountPo::getEmail,
                        StaffAccountPo::getStatus, StaffAccountPo::getEmploymentStatus,
                        StaffAccountPo::getOffboardedAt, StaffAccountPo::getOffboardedBy,
                        StaffAccountPo::getOffboardReason, StaffAccountPo::getSource,
                        StaffAccountPo::getAuthVersion)
                .orderBy(StaffAccountPo::getId, false));
    }

    default StaffAccountPo selectById(long id) {
        return selectOneByQuery(QueryWrapper.create()
                .from(StaffAccountPo.class)
                .select(StaffAccountPo::getId, StaffAccountPo::getSubjectId,
                        StaffAccountPo::getUsername, StaffAccountPo::getRealName,
                        StaffAccountPo::getNickname, StaffAccountPo::getAvatarUrl,
                        StaffAccountPo::getPhone, StaffAccountPo::getEmail,
                        StaffAccountPo::getStatus, StaffAccountPo::getEmploymentStatus,
                        StaffAccountPo::getOffboardedAt, StaffAccountPo::getOffboardedBy,
                        StaffAccountPo::getOffboardReason, StaffAccountPo::getSource,
                        StaffAccountPo::getAuthVersion)
                .where(StaffAccountPo::getId).eq(id));
    }

    int insertRow(StaffAccountPo po);

    int updateSelective(@Param("id") long id,
                        @Param("username") String username,
                        @Param("realName") String realName,
                        @Param("nickname") String nickname,
                        @Param("phone") String phone,
                        @Param("email") String email,
                        @Param("passwordHash") String passwordHash,
                        @Param("status") String status);

    int updateStatus(@Param("id") long id, @Param("status") String status);

    int offboard(@Param("id") long id,
                 @Param("offboardedAt") LocalDateTime offboardedAt,
                 @Param("offboardedBy") Long offboardedBy,
                 @Param("offboardReason") String offboardReason);

    int restoreEmployment(@Param("id") long id);

    default StaffAccountPresentationPo selectPresentationById(long id) {
        return selectOneByQueryAs(QueryWrapper.create()
                .from(StaffAccountPo.class)
                .select(StaffAccountPo::getUsername, StaffAccountPo::getRealName,
                        StaffAccountPo::getNickname, StaffAccountPo::getAvatarUrl,
                        StaffAccountPo::getPhone, StaffAccountPo::getEmail)
                .where(StaffAccountPo::getId).eq(id), StaffAccountPresentationPo.class);
    }

    int revokeActiveSessions(@Param("userId") long userId, @Param("revokedReason") String revokedReason);

    int countActiveStaffByDomainRole(@Param("businessDomainId") long businessDomainId,
                                     @Param("roleCode") String roleCode,
                                     @Param("excludeStaffAccountId") long excludeStaffAccountId);

    List<Long> selectOrganizationIds(@Param("staffAccountId") long staffAccountId);

    int deleteOrganizations(@Param("staffAccountId") long staffAccountId);

    int insertOrganization(@Param("staffAccountId") long staffAccountId, @Param("organizationId") long organizationId);
}
