package com.uniondesk.domain.mapper;

import com.uniondesk.domain.entity.StaffAccountPo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface StaffAccountMapper {

    int countById(@Param("id") long id);

    Long selectIdById(@Param("id") long id);

    String selectUsernameById(@Param("id") long id);

    Long selectIdByUsername(@Param("username") String username);

    StaffAccountPo selectStaffInDomain(
            @Param("domainId") long domainId,
            @Param("staffAccountId") long staffAccountId);
}
