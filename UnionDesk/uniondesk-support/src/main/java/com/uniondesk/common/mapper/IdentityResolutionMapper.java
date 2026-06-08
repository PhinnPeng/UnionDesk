package com.uniondesk.common.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface IdentityResolutionMapper {

    Long findCustomerSubjectId(@Param("accountId") long accountId);

    Long findStaffSubjectId(@Param("accountId") long accountId);

    Long findIdentitySubjectId(@Param("id") long id);

    String findUserAccountPhone(@Param("userId") long userId, @Param("fallback") String fallback);

    void insertIdentitySubject(@Param("id") long id, @Param("phone") String phone);

    Long findIdentitySubjectIdByPhone(@Param("phone") String phone);
}
