package com.uniondesk.domain.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface IdentitySubjectMapper {

    Long selectIdById(@Param("id") long id);

    String selectPhoneByUserId(@Param("userId") long userId, @Param("fallback") String fallback);

    void insertPerson(@Param("id") long id, @Param("phone") String phone);

    Long selectIdByPhone(@Param("phone") String phone);
}
