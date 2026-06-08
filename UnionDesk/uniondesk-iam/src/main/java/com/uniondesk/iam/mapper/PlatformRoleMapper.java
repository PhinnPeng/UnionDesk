package com.uniondesk.iam.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PlatformRoleMapper {

    void deleteStaffPlatformRoles(@Param("staffAccountId") long staffAccountId);

    void insertStaffPlatformRole(@Param("staffAccountId") long staffAccountId, @Param("platformRoleId") long platformRoleId);

    List<String> selectCurrentPlatformRoleCodes(@Param("staffAccountId") long staffAccountId);

    int countActivePlatformAdminsExcluding(@Param("staffAccountId") long staffAccountId);

    Long selectPlatformRoleIdByCode(@Param("code") String code);

    List<String> selectCurrentDomainRoleCodes(@Param("staffAccountId") long staffAccountId, @Param("businessDomainId") long businessDomainId);
}
